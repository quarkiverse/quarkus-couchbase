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
        final ClusterOptions clusterOptions = ClusterOptions.clusterOptions(config.username(), config.password());

        if (metricsEnabled) {
            clusterOptions.environment(env -> configureMetrics(env, config.emitInterval()));
        }

        return () -> Cluster.connect(config.connectionString(), clusterOptions);
    }

    private void configureMetrics(ClusterEnvironment.Builder env, int emitInterval) {
        env.meter(MicrometerMeter.wrap(Metrics.globalRegistry))
                .loggingMeterConfig(meterConfig -> meterConfig
                        .enabled(true)
                        .emitInterval(Duration.ofSeconds(emitInterval)));
    }
}