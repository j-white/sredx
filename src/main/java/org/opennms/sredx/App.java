/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2020 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2020 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.sredx;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.opennms.sredx.jira.JiraClient;
import org.opennms.sredx.model.Project;
import org.opennms.sredx.model.SredConfig;
import org.opennms.sredx.model.User;

import fr.vidal.oss.jaxb.atom.core.Entry;

public class App {

    @Option(name="-f",usage="Model file")
    public String modelFile;

    public static void main(String[] args) {
        App sredx = new App();
        CmdLineParser parser = new CmdLineParser(sredx);
        try {
            parser.parseArgument(args);
            sredx.run();
        } catch (CmdLineException e) {
            // handling of wrong arguments
            System.err.println(e.getMessage());
            parser.printUsage(System.err);
        }
    }

    private void run() {
        System.out.println("Running with mode: " + modelFile);
        SredConfig config = SredConfig.load(new File(modelFile));

        Map<String, User> usersByEmail = new LinkedHashMap<>();
        for (User user : config.getUsers()) {
            for (String email : user.getEmail()) {
                usersByEmail.put(email.toLowerCase().trim(), user);
            }
        }

        Set<String> unmatchedEmails = new LinkedHashSet<>();
        JiraClient jiraClient = new JiraClient(config.getJira());
        for (Project project : config.getProjects()) {
            List<Activity> activities = new LinkedList<>();
            for (User user : config.getUsers()) {
                System.out.printf("Retrieving JIRA activity for %s ...\n", user.getJiraUser());
                Collection<Entry> entries = jiraClient.getUserActivity(user.getJiraUser(), project.getStartDate(), project.getEndDate());
                System.out.printf("Found %d entries for: %s.\n", entries.size(), user.getName());
                for (Entry entry : entries) {
                    activities.add(new Activity(project, user, Activity.Type.JIRA, JiraClient.getJiraIssueId(entry), entry.getPublishedDate().toInstant(), entry.getTitle()));
                }
            }

            for (String gitRepo : project.getGitRepositories()) {
                try {
                    Repository repo = new FileRepositoryBuilder()
                            .setGitDir(new File(gitRepo))
                            .build();
                    Git git = new Git(repo);
                    for (RevCommit revCommit : git.log().all().call()) {
                        // Only consider commits within the window of the project
                        PersonIdent author = revCommit.getAuthorIdent();
                        if (!project.isInProjectWindow(author.getWhen())) {
                            continue;
                        }
                        // Try and associate the commits with a known user in the project
                        String authorEmail = author.getEmailAddress().toLowerCase().trim();
                        User user = usersByEmail.get(authorEmail);
                        if (user == null) {
                            // Track unmatched e-mails for auditing (i.e. review them to see if one of these should be added to a user?)
                            unmatchedEmails.add(authorEmail);
                            // We haven't matched a known user in this project, so skip the commit
                            continue;
                        }
                        activities.add(new Activity(project, user, Activity.Type.GIT, revCommit.getId().getName(), author.getWhen().toInstant(), revCommit.getFullMessage()));
                    }
                } catch (IOException | GitAPIException e) {
                    e.printStackTrace();
                }
            }

            // We now have all the Git & JIRA activity for all the users in the project, let's save it to a .csv file
            activities.sort(Comparator.comparing(Activity::getTimestamp));
            try {
                final CSVPrinter printer = CSVFormat.DEFAULT.withHeader(
                        "Project",
                        "Date",
                        "User",
                        "Type",
                        "ID",
                        "Summary").print(new File("out.csv"), StandardCharsets.UTF_8);
                for (Activity activity : activities) {
                    printer.printRecord(activity.getProject().getCode(), activity.getTimestamp(), activity.getUser().getName(), activity.getType(), activity.getId(), activity.getSummary());
                }
                printer.close(true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.out.println("Unmatched e-mails:" + unmatchedEmails);
    }
}
