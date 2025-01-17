/*
 * Copyright (c) 2024 Couchbase, Inc.
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

import java.util.Map;

import jakarta.inject.Inject;

import org.testcontainers.couchbase.CouchbaseContainer;
import org.testcontainers.couchbase.CouchbaseService;

import com.couchbase.quarkus.extension.runtime.CouchbaseConfig;

import io.quarkus.deployment.IsNormal;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.builditem.CuratedApplicationShutdownBuildItem;
import io.quarkus.deployment.builditem.DevServicesResultBuildItem;
import io.quarkus.deployment.builditem.DevServicesResultBuildItem.RunningDevService;
import io.quarkus.deployment.dev.devservices.GlobalDevServicesConfig;

/**
 * This class provides a Quarkus DevService for Couchbase. The dev service provides all non enterprise features of Couchbase.
 */
@BuildSteps(onlyIfNot = IsNormal.class, onlyIf = GlobalDevServicesConfig.Enabled.class)
public class CouchbaseDevService {

    static volatile RunningDevService devService;

    @Inject
    CouchbaseConfig config;

    @BuildStep
    DevServicesResultBuildItem startCouchBase(
            CuratedApplicationShutdownBuildItem closeBuildItem) {
        if (devService != null) {
            return devService.toBuildItem();
        }
        QuarkusCouchbaseContainer couchbase = startContainer();
        devService = new RunningDevService(CouchbaseQuarkusExtensionProcessor.FEATURE,
                couchbase.getContainerId(), couchbase::close, Map.of());
        closeBuildItem.addCloseTask(couchbase::close, true);
        return devService.toBuildItem();

    }

    private QuarkusCouchbaseContainer startContainer() {
        QuarkusCouchbaseContainer couchbase = new QuarkusCouchbaseContainer(config.version(), config.username(),
                config.password());
        couchbase.start();
        return couchbase;
    }

    /**
     * A {@link CouchbaseContainer} that exposes all ports to fixed values.
     * <p>
     * This is needed because the default {@link CouchbaseContainer} exposes ports to random values.
     */
    private static class QuarkusCouchbaseContainer extends CouchbaseContainer {

        // copied from org.testcontainers.couchbase.CouchbaseContainer
        private static final int MGMT_PORT = 8091;
        private static final int MGMT_SSL_PORT = 18091;
        private static final int VIEW_PORT = 8092;
        private static final int VIEW_SSL_PORT = 18092;
        private static final int QUERY_PORT = 8093;
        private static final int QUERY_SSL_PORT = 18093;
        private static final int SEARCH_PORT = 8094;
        private static final int SEARCH_SSL_PORT = 18094;
        private static final int ANALYTICS_PORT = 8095;
        private static final int ANALYTICS_SSL_PORT = 18095;
        private static final int EVENTING_PORT = 8096;
        private static final int EVENTING_SSL_PORT = 18096;
        private static final int KV_PORT = 11210;
        private static final int KV_SSL_PORT = 11207;

        public QuarkusCouchbaseContainer(String version, String userName, String password) {
            super("couchbase/server:" + version);
            withCredentials(userName, password);
            // we enable all non-enterprise services because we don't know which ones are needed
            withEnabledServices(CouchbaseService.EVENTING, CouchbaseService.INDEX, CouchbaseService.KV,
                    CouchbaseService.QUERY, CouchbaseService.SEARCH);
            addFixedExposedPort(MGMT_PORT, MGMT_PORT);
            addFixedExposedPort(MGMT_SSL_PORT, MGMT_SSL_PORT);
            addFixedExposedPort(ANALYTICS_PORT, ANALYTICS_PORT);
            addFixedExposedPort(VIEW_PORT, VIEW_PORT);
            addFixedExposedPort(VIEW_SSL_PORT, VIEW_SSL_PORT);
            addFixedExposedPort(ANALYTICS_PORT, ANALYTICS_PORT);
            addFixedExposedPort(ANALYTICS_SSL_PORT, ANALYTICS_SSL_PORT);
            addFixedExposedPort(QUERY_PORT, QUERY_PORT);
            addFixedExposedPort(QUERY_SSL_PORT, QUERY_SSL_PORT);
            addFixedExposedPort(SEARCH_PORT, SEARCH_PORT);
            addFixedExposedPort(SEARCH_SSL_PORT, SEARCH_SSL_PORT);
            addFixedExposedPort(EVENTING_PORT, EVENTING_PORT);
            addFixedExposedPort(EVENTING_SSL_PORT, EVENTING_SSL_PORT);
            addFixedExposedPort(KV_PORT, KV_PORT);
            addFixedExposedPort(KV_SSL_PORT, KV_SSL_PORT);
        }
    }
}
