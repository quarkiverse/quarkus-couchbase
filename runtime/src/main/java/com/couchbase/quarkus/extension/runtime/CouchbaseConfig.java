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
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public interface CouchbaseConfig {
    /**
     * The connection string, e.g. "couchbase://10.202.32.32" or "localhost".
     * Optional when DevServices are enabled with dynamic ports, in which case
     * it will be automatically set to the container's connection string.
     */
    Optional<String> connectionString();

    /**
     * The username to authenticate with.
     */
    String username();

    /**
     * The password to authenticate with.
     */
    String password();

    /**
     * The version of the Couchbase server to connect to during the dev/test phase as dev service.
     * Default is latest, see the <a href="https://hub.docker.com/_/couchbase">Docker Hub</a> for available versions.
     */
    @WithDefault("latest")
    @WithName(("devservices.version"))
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
     * The timeout for the Ready health check in seconds
     * In other words: "How long you are willing to wait to know whether the cluster is ready or not".
     */
    @WithDefault("3")
    @WithName("health.readiness.timeout")
    int readinessTimeout();

    /**
     * Whether metrics are enabled
     */
    @WithDefault("false")
    @WithName("metrics.enabled")
    boolean metricsEnabled();

    /**
     * The interval in seconds when metrics are emitted.
     */
    @WithDefault("600")
    @WithName("metrics.emit-interval")
    int emitInterval();

    /**
     * The preferred server group to use for operations that support such.
     */
    @WithName("preferredServerGroup")
    Optional<String> preferredServerGroup();

    /**
     * Whether to enable TLS for the Couchbase connections.
     */
    @WithName("security.enableTls")
    Optional<Boolean> enableTls();

    /**
     * Whether to enable native TLS (OpenSSL) for better performance.
     */
    @WithName("security.enableNativeTls")
    Optional<Boolean> enableNativeTls();

    /**
     * The path to a certificate file for TLS connections.
     */
    @WithName("security.trustCertificate")
    Optional<String> certificatePath();

    /**
     * Whether to enable hostname verification for TLS connections.
     * Set false to skip hostname verification (carries security risks, not recommended for production).
     */
    @WithName("security.enableHostnameVerification")
    Optional<Boolean> enableHostnameVerification();

    /**
     * A list of enabled ciphers for TLS connections, separated by commas.
     */
    @WithName("security.ciphers")
    Optional<String> ciphers();

}
