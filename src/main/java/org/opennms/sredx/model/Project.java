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

package org.opennms.sredx.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Project {

    private String name;

    private String code;

    @JsonProperty("start-date")
    private Instant startDate;

    @JsonProperty("end-date")
    private Instant endDate;

    @JsonProperty("jira-projects")
    private List<String> jiraProjects = new ArrayList<>();

    @JsonProperty("git-repositories")
    private List<String> gitRepositories = new ArrayList<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Instant getStartDate() {
        return startDate;
    }

    public void setStartDate(Instant startDate) {
        this.startDate = startDate;
    }

    public Instant getEndDate() {
        return endDate;
    }

    public void setEndDate(Instant endDate) {
        this.endDate = endDate;
    }

    public List<String> getJiraProjects() {
        return jiraProjects;
    }

    public void setJiraProjects(List<String> jiraProjects) {
        this.jiraProjects = jiraProjects;
    }

    public List<String> getGitRepositories() {
        return gitRepositories;
    }

    public void setGitRepositories(List<String> gitRepositories) {
        this.gitRepositories = gitRepositories;
    }

    public boolean isInProjectWindow(Date date) {
        Instant instant = date.toInstant();
        return startDate.isBefore(instant) && endDate.isAfter(instant);
    }
}
