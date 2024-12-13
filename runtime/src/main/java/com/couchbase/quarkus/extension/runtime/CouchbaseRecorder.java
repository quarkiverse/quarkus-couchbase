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

import java.time.Duration;
import java.util.function.Supplier;

import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.ClusterOptions;
import com.couchbase.client.metrics.micrometer.MicrometerMeter;

import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class CouchbaseRecorder {

    public Supplier<Cluster> getCluster(CouchbaseConfig config) {
        return () -> Cluster.connect(config.connectionString(), config.username(), config.password());
    }

    public Supplier<Cluster> getClusterWithMetrics(CouchbaseConfig config) {
        return () -> Cluster.connect(config.connectionString(),
                ClusterOptions.clusterOptions(config.username(), config.password())
                        .environment(
                                env -> env.meter(MicrometerMeter.wrap(io.micrometer.core.instrument.Metrics.globalRegistry))
                                        .loggingMeterConfig(meterConfig -> meterConfig
                                                .enabled(true)
                                                .emitInterval(Duration.ofSeconds(config.emitInterval())))));
    }
}
