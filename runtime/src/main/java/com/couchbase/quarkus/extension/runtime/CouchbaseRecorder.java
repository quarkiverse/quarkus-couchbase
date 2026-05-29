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
import java.util.function.Function;
import java.util.function.Supplier;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.ClusterOptions;
import com.couchbase.client.java.env.ClusterEnvironment;
import com.couchbase.client.metrics.micrometer.MicrometerMeter;

import io.micrometer.core.instrument.Metrics;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class CouchbaseRecorder {

    private final RuntimeValue<CouchbaseRuntimeConfig> config;

    public CouchbaseRecorder(RuntimeValue<CouchbaseRuntimeConfig> config) {
        this.config = config;
    }

    public Supplier<Cluster> getCluster(boolean metricsEnabled) {
        return () -> {
            CouchbaseRuntimeConfig c = config.getValue();
            ClusterOptions clusterOptions = ClusterOptions.clusterOptions(c.username(), c.password());
            clusterOptions.environment(env -> configureEnvironment(c, env, metricsEnabled));
            return Cluster.connect(c.connectionString().orElseThrow(
                    () -> new IllegalStateException("quarkus.couchbase.connection-string is required")), clusterOptions);
        };
    }

    public Function<SyntheticCreationalContext<Bucket>, Bucket> getBucket() {
        return context -> {
            String bucketName = config.getValue().bucketName()
                    .filter(name -> !name.isBlank())
                    .orElseThrow(
                            () -> new IllegalStateException("quarkus.couchbase.bucket-name is required to inject a Bucket"));
            Cluster cluster = context.getInjectedReference(Cluster.class);
            return cluster.bucket(bucketName);
        };
    }

    private void configureEnvironment(CouchbaseRuntimeConfig c, ClusterEnvironment.Builder env, boolean metricsEnabled) {
        if (metricsEnabled) {
            env.meter(MicrometerMeter.wrap(Metrics.globalRegistry))
                    .loggingMeterConfig(meterConfig -> meterConfig
                            .enabled(true)
                            .emitInterval(Duration.ofSeconds(c.emitInterval())));
        }

        if (c.preferredServerGroup().isPresent()) {
            env.preferredServerGroup(c.preferredServerGroup().get());
        }

        if (c.enableNativeTls().isPresent()) {
            env.securityConfig().enableNativeTls(c.enableNativeTls().get());
        }

        if (c.enableTls().isPresent()) {
            env.securityConfig().enableTls(c.enableTls().get());
        }

        if (c.enableHostnameVerification().isPresent()) {
            env.securityConfig().enableHostnameVerification(c.enableHostnameVerification().get());
        }

        if (c.certificatePath().isPresent()) {
            env.securityConfig().trustCertificate(Path.of(c.certificatePath().get()));
        }

        if (c.ciphers().isPresent()) {
            var parsedCiphers = Arrays.asList(c.ciphers().get().split(","));
            env.securityConfig().ciphers(parsedCiphers);
        }
    }
}
