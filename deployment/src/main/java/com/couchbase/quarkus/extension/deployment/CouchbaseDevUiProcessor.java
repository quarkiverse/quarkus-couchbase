/*
 * Copyright (c) 2025 Couchbase, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.couchbase.quarkus.extension.deployment;

import com.couchbase.client.core.util.ConnectionString;
import com.couchbase.quarkus.extension.runtime.CouchbaseConfig;

import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.devui.spi.page.Page;

public class CouchbaseDevUiProcessor {
    @BuildStep(onlyIf = IsDevelopment.class)
    public CardPageBuildItem pages(CouchbaseConfig config) {
        CardPageBuildItem cardPageBuildItem = new CardPageBuildItem();

        var hostname = extractHostnameFromConnectionString(config.connectionString());
        var clusterUiUrl = "http://" + hostname + ":8091/ui/index.html";

        String JAVA_SDK_DOCS = "https://docs.couchbase.com/java-sdk/current/hello-world/overview.html";

        cardPageBuildItem.addPage(Page.externalPageBuilder("Cluster Dashboard")
                .url(clusterUiUrl, clusterUiUrl)
                .doNotEmbed()
                .icon("font-awesome-solid:database"));

        cardPageBuildItem.addPage(Page.externalPageBuilder("Java SDK Docs")
                .url(JAVA_SDK_DOCS, JAVA_SDK_DOCS)
                .doNotEmbed()
                .icon("font-awesome-solid:couch"));

        cardPageBuildItem.addPage(Page.externalPageBuilder("Extension Guide")
                .url("https://docs.quarkiverse.io/quarkus-couchbase/dev/index.html")
                .isHtmlContent()
                .icon("font-awesome-solid:book"));

        return cardPageBuildItem;
    }

    /**
     * Extracts the first hostname from the connection string to redirect to the Cluster UI Dashboard.
     *
     * @param connectionString The connection string specified in application.properties.
     * @return The first hostname.
     */
    private String extractHostnameFromConnectionString(String connectionString) {
        ConnectionString connStr = ConnectionString.create(connectionString);
        return connStr.hosts().get(0).host();
    }
}
