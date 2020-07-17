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

import java.time.Instant;
import java.util.Objects;

import org.opennms.sredx.model.Project;
import org.opennms.sredx.model.User;

public class Activity {

    private final Project project;
    private final User user;
    private final Type type;
    private final String id;
    private final Instant timestamp;
    private final String summary;

    public enum Type {
        JIRA,
        GIT;
    }

    public Activity(Project project, User user, Type type, String id, Instant timestamp, String summary) {
        this.project = Objects.requireNonNull(project);
        this.user = Objects.requireNonNull(user);
        this.type = Objects.requireNonNull(type);
        this.id = Objects.requireNonNull(id);
        this.timestamp = Objects.requireNonNull(timestamp);
        this.summary = Objects.requireNonNull(summary);
    }

    public Project getProject() {
        return project;
    }

    public User getUser() {
        return user;
    }

    public Type getType() {
        return type;
    }

    public String getId() {
        return id;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getSummary() {
        return summary;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Activity)) return false;
        Activity activity = (Activity) o;
        return Objects.equals(project, activity.project) &&
                Objects.equals(user, activity.user) &&
                type == activity.type &&
                Objects.equals(timestamp, activity.timestamp) &&
                Objects.equals(summary, activity.summary);
    }

    @Override
    public int hashCode() {
        return Objects.hash(project, user, type, timestamp, summary);
    }

    @Override
    public String toString() {
        return "Activity{" +
                "project=" + project +
                ", user=" + user +
                ", type=" + type +
                ", timestamp=" + timestamp +
                ", summary='" + summary + '\'' +
                '}';
    }
}
