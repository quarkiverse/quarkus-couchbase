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
package com.couchbase.quarkus.extension.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

@ConfigMapping(prefix = "quarkus.couchbase")
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public interface CouchbaseBuildTimeConfig {
    /**
     * The version of the Couchbase server to connect to during the dev/test phase as dev service.
     * Default is latest, see the <a href="https://hub.docker.com/_/couchbase">Docker Hub</a> for available versions.
     */
    @WithDefault("latest")
    @WithName("devservices.version")
    String version();

    /**
     * The UI port for the Couchbase Dev UI. This is automatically set by DevServices.
     */
    @WithName("devservices.ui-port")
    Optional<Integer> devServicesUiPort();

    /**
     * Whether to use dynamic ports for DevServices.
     * When true, TestContainers will use random available ports.
     */
    @WithDefault("false")
    @WithName("devservices.use-dynamic-ports")
    boolean useDynamicPorts();

    /**
     * Whether to enable health checks or not.
     */
    @WithDefault("true")
    @WithName("health.enabled")
    boolean healthEnabled();

    /**
     * Whether metrics are enabled.
     */
    @WithDefault("false")
    @WithName("metrics.enabled")
    boolean metricsEnabled();
}
