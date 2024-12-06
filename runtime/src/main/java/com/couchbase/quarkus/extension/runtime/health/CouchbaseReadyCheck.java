package com.couchbase.quarkus.extension.runtime.health;

import java.time.Duration;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Readiness;

import com.couchbase.client.core.diagnostics.ClusterState;
import com.couchbase.client.core.service.ServiceType;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.diagnostics.WaitUntilReadyOptions;
import com.couchbase.quarkus.extension.runtime.CouchbaseConfig;

import io.smallrye.health.api.AsyncHealthCheck;
import io.smallrye.mutiny.Uni;

@Readiness
@ApplicationScoped
public class CouchbaseReadyCheck implements AsyncHealthCheck {

    @Inject
    Cluster cluster;

    @Inject
    CouchbaseConfig config;

    /**
     * Performs a Cluster-level WaitUntilReady operation to check if the cluster is ready.
     *
     * @return A HealthCheckResponse.
     */
    @Override
    public Uni<HealthCheckResponse> call() {
        HealthCheckResponseBuilder builder = HealthCheckResponse.named("Couchbase Cluster Readiness Check");

        return Uni.createFrom()
                .completionStage(cluster.reactive().waitUntilReady(Duration.ofSeconds(config.readinessTimeout()),
                        WaitUntilReadyOptions.waitUntilReadyOptions().desiredState(ClusterState.ONLINE)
                                .serviceTypes(new ServiceType[] { ServiceType.MANAGER, ServiceType.KV }))
                        .toFuture())
                .onItem().transform(ignore -> builder.up().withData("cluster", "ready").build())
                .onFailure().recoverWithItem(
                        throwable -> builder.down().withData("errorMsg", throwable.getMessage())
                                .build());
    }
}
