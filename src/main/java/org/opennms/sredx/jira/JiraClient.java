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

package org.opennms.sredx.jira;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;

import org.opennms.sredx.model.Jira;

import fr.vidal.oss.jaxb.atom.core.AtomJaxb;
import fr.vidal.oss.jaxb.atom.core.Entry;
import fr.vidal.oss.jaxb.atom.core.Feed;
import fr.vidal.oss.jaxb.atom.core.Link;
import okhttp3.Credentials;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class JiraClient {

    private static final int MAX_RESULTS = 25;

    private final OkHttpClient client;
    private final HttpUrl baseUrl;
    private final String credentials;
    private final Unmarshaller feedUnmarshaller;

    public JiraClient(String url, String username, String password) {
        this.baseUrl = HttpUrl.parse(url);
        this.client = new OkHttpClient();
        this.credentials = Credentials.basic(username, password);

        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.connectTimeout(10, TimeUnit.SECONDS);
        builder.writeTimeout(10, TimeUnit.SECONDS);
        builder.readTimeout(30, TimeUnit.SECONDS);

        try {
            feedUnmarshaller = AtomJaxb.newContext().createUnmarshaller();
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }

    public JiraClient(Jira jira) {
        this(jira.getUrl(), jira.getUsername(), jira.getPassword());
    }

    public List<Entry> getUserActivity(String user, Instant start, Instant end) {
        // Perform a binary search over the target range to retrieve all of the results while taking into account the search limit
        Feed feed = getFeed("user IS " + user, String.format("update-date BETWEEN %d %d", start.toEpochMilli(), end.toEpochMilli()));
        if (feed.getEntries().size() < MAX_RESULTS) {
            // We've successfully retrieved all of the results in the range
            return new ArrayList<>(feed.getEntries());
        } else {
            // There may be more results in the range than what we have

            // Calculate the midpoint
            Instant midpoint = Instant.ofEpochMilli(Math.floorDiv(start.toEpochMilli() + end.toEpochMilli(), 2));

            // Recurse
            List<Entry> entries = new LinkedList<>();
            entries.addAll(getUserActivity(user, start, midpoint));
            entries.addAll(getUserActivity(user, midpoint, end));
            return entries;
        }
    }

    // https://issues.opennms.org/plugins/servlet/streams?maxResults=10&streams=user+IS+j-white&streams=update-date%20BETWEEN%201594267200000%201594353599999&_=1594411542710
    private Feed getFeed(String... queries)  {
        HttpUrl.Builder url = baseUrl.newBuilder()
                .addPathSegments("plugins/servlet/streams")
                .addQueryParameter("maxResults", Integer.toString(MAX_RESULTS));
        for (String query : queries) {
            url.addQueryParameter("streams", query);
        }
        url.addQueryParameter("_", Long.toString(System.currentTimeMillis()));
        Request request = new Request.Builder()
                .url(url.build())
                .addHeader("Accept", "application/xml")
                .header("Authorization", credentials)
                .get()
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Request failed with: " + response.code());
            }
            try (ResponseBody body = response.body()) {
               return (Feed) feedUnmarshaller.unmarshal(new StreamSource(body.byteStream()));
            } catch (JAXBException e) {
               throw new IOException(e);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String getJiraIssueId(Entry entry) {
        for (Link link : entry.getLinks()) {
            int idx = link.getHref().indexOf("browse/");
            if (idx > 0) {
                String issueInUrl = link.getHref().substring(idx + 7);
                idx = issueInUrl.indexOf("?");
                if (idx > 0) {
                    return issueInUrl.substring(0, idx);
                } else {
                    return issueInUrl;
                }
            }
        }
        return "(unknown)";
    }
}
