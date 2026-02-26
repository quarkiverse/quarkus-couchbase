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
package com.couchbase.quarkus.extension.runtime;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.function.Supplier;

import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.ClusterOptions;
import com.couchbase.client.java.env.ClusterEnvironment;
import com.couchbase.client.metrics.micrometer.MicrometerMeter;

import io.micrometer.core.instrument.Metrics;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class CouchbaseRecorder {

    public Supplier<Cluster> getCluster(CouchbaseConfig config, boolean metricsEnabled) {
        final ClusterOptions clusterOptions = ClusterOptions.clusterOptions(config.username(), config.password());

        clusterOptions.environment(env -> configureEnvironment(config, env, metricsEnabled));

        return () -> Cluster.connect(config.connectionString().orElseThrow(
                () -> new IllegalStateException("quarkus.couchbase.connection-string is required")), clusterOptions);
    }

    private void configureEnvironment(CouchbaseConfig config, ClusterEnvironment.Builder env, boolean metricsEnabled) {
        if (metricsEnabled) {
            env.meter(MicrometerMeter.wrap(Metrics.globalRegistry))
                    .loggingMeterConfig(meterConfig -> meterConfig
                            .enabled(true)
                            .emitInterval(Duration.ofSeconds(config.emitInterval())));
        }

        if (config.preferredServerGroup().isPresent()) {
            env.preferredServerGroup(config.preferredServerGroup().get());
        }

        if (config.enableNativeTls().isPresent()) {
            env.securityConfig().enableNativeTls(config.enableNativeTls().get());
        }

        if (config.enableTls().isPresent()) {
            env.securityConfig().enableTls(config.enableTls().get());
        }

        if (config.enableHostnameVerification().isPresent()) {
            env.securityConfig().enableHostnameVerification(config.enableHostnameVerification().get());
        }

        if (config.certificatePath().isPresent()) {
            env.securityConfig().trustCertificate(Path.of(config.certificatePath().get()));
        }

        if (config.ciphers().isPresent()) {
            var parsedCiphers = Arrays.asList(config.ciphers().get().split(","));
            env.securityConfig().ciphers(parsedCiphers);
        }
    }
}
