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
import java.util.Optional;

import jakarta.inject.Inject;

import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;
import org.testcontainers.couchbase.BucketDefinition;
import org.testcontainers.couchbase.CouchbaseContainer;
import org.testcontainers.couchbase.CouchbaseService;

import com.couchbase.quarkus.extension.runtime.CouchbaseBuildTimeConfig;

import io.quarkus.deployment.IsNormal;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.builditem.CuratedApplicationShutdownBuildItem;
import io.quarkus.deployment.builditem.DevServicesResultBuildItem;
import io.quarkus.deployment.builditem.DevServicesResultBuildItem.RunningDevService;
import io.quarkus.deployment.dev.devservices.DevServicesConfig;

/**
 * This class provides a Quarkus DevService for Couchbase. The dev service provides all non enterprise features of Couchbase.
 */
@BuildSteps(onlyIfNot = IsNormal.class, onlyIf = DevServicesConfig.Enabled.class)
public class CouchbaseDevService {

    private static final Logger log = Logger.getLogger(CouchbaseDevService.class);
    private static final String DEFAULT_USERNAME = "Administrator";
    private static final String DEFAULT_PASSWORD = "password";

    static volatile RunningDevService devService;

    @Inject
    CouchbaseBuildTimeConfig buildTimeConfig;

    @BuildStep
    DevServicesResultBuildItem startCouchBase(
            CuratedApplicationShutdownBuildItem closeBuildItem) {

        if (ConfigProvider.getConfig()
                .getOptionalValue("quarkus.couchbase.connection-string", String.class)
                .filter(connectionString -> !connectionString.isBlank())
                .isPresent()) {
            log.info("quarkus.couchbase.connection-string is set, skipping TestContainer deployment.");
            return null;
        }

        if (devService != null) {
            return devService.toBuildItem();
        }

        // Credentials are required runtime config, but when DevServices provisions a container we
        // supply them ourselves (defaulting if unset) and inject the same values at runtime so the
        // app needs no credential configuration to connect to the container.
        var config = ConfigProvider.getConfig();
        String username = config.getOptionalValue("quarkus.couchbase.username", String.class).orElse(DEFAULT_USERNAME);
        String password = config.getOptionalValue("quarkus.couchbase.password", String.class).orElse(DEFAULT_PASSWORD);

        QuarkusCouchbaseContainer couchbase = startContainer(username, password);

        // The container always determines the connection string and UI port whether
        // dynamic or fixed, so we inject both at runtime. Since we only reach this point when no
        // connection string was set, we can safely set it.
        Map<String, String> dynamicConfig = Map.of(
                "quarkus.couchbase.connection-string", couchbase.getConnectionString(),
                "quarkus.couchbase.username", username,
                "quarkus.couchbase.password", password,
                "quarkus.couchbase.devservices.ui-port", String.valueOf(couchbase.getMappedPort(8091)));

        // Pass the dynamic values via config overrides so they're available at runtime
        devService = new RunningDevService(CouchbaseQuarkusExtensionProcessor.FEATURE,
                couchbase.getContainerId(), couchbase::close, dynamicConfig);
        closeBuildItem.addCloseTask(couchbase::close, true);
        return devService.toBuildItem();

    }

    private QuarkusCouchbaseContainer startContainer(String username, String password) {
        // bucket-name is runtime config but sourced from application.properties, so it's visible
        // during the build phase. Provision that bucket so an injected Bucket bean is usable.
        Optional<String> bucketName = ConfigProvider.getConfig()
                .getOptionalValue("quarkus.couchbase.bucket-name", String.class)
                .filter(name -> !name.isBlank());
        QuarkusCouchbaseContainer couchbase = new QuarkusCouchbaseContainer(buildTimeConfig.version(), username, password,
                buildTimeConfig.useDynamicPorts(), bucketName);
        couchbase.start();
        return couchbase;
    }

    /**
     * A {@link CouchbaseContainer} that exposes all ports to random or fixed values
     * depending on configuration
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

        private final boolean useDynamicPorts;

        public QuarkusCouchbaseContainer(String version, String userName, String password, boolean useDynamicPorts,
                Optional<String> bucketName) {
            super("couchbase/server:" + version);
            this.useDynamicPorts = useDynamicPorts;
            withCredentials(userName, password);
            // we enable all non-enterprise services because we don't know which ones are needed
            withEnabledServices(CouchbaseService.EVENTING, CouchbaseService.INDEX, CouchbaseService.KV,
                    CouchbaseService.QUERY, CouchbaseService.SEARCH);

            bucketName.ifPresent(name -> withBucket(new BucketDefinition(name)));

            if (!useDynamicPorts) {
                // Fixed ports mode, map container ports to the same host ports.
                addFixedExposedPort(MGMT_PORT, MGMT_PORT);
                addFixedExposedPort(MGMT_SSL_PORT, MGMT_SSL_PORT);
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

        /**
         * In fixed-ports mode, return the original port instead of the parent's random mapping so the
         * container's advertised ports, bindings, and connection string all stay consistent.
         */
        @Override
        public Integer getMappedPort(int originalPort) {
            return useDynamicPorts ? super.getMappedPort(originalPort) : originalPort;
        }
    }
}
