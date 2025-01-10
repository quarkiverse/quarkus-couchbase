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
import com.couchbase.client.java.env.ClusterEnvironment;
import com.couchbase.client.metrics.micrometer.MicrometerMeter;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class CouchbaseRecorder {

    public Supplier<Cluster> getCluster(CouchbaseConfig config, boolean metricsEnabled) {
        var clusterOptions = ClusterOptions.clusterOptions(config.username(), config.password());
        var clusterEnvironmentBuilder = ClusterEnvironment.builder();
        if (metricsEnabled) {
            configureMetrics(clusterEnvironmentBuilder, config.emitInterval());
        }
        clusterOptions.environment(clusterEnvironmentBuilder.build());
        return () -> Cluster.connect(config.connectionString(), clusterOptions);
    }

    public void configureMetrics(ClusterEnvironment.Builder clusterEnvironmentBuilder, int emitInterval) {
        //Micrometer won't create a histogram by default. Configuring it here.
        Metrics.globalRegistry.config().meterFilter(new MeterFilter() {
            @Override
            public DistributionStatisticConfig configure(Meter.Id id, DistributionStatisticConfig config) {
                if (id.getType() == Meter.Type.DISTRIBUTION_SUMMARY) {
                    return DistributionStatisticConfig.builder()
                            .percentilesHistogram(true)
                            .build()
                            .merge(config);
                }
                return config;
            }
        });

        clusterEnvironmentBuilder.meter(MicrometerMeter.wrap(Metrics.globalRegistry))
                .loggingMeterConfig(meterConfig -> meterConfig
                        .enabled(true)
                        .emitInterval(Duration.ofSeconds(emitInterval)));

    }
}
