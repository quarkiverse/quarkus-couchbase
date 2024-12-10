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
package com.couchbase.quarkus.extension.runtime.health;

import static com.couchbase.client.core.service.ServiceType.KV;
import static com.couchbase.client.java.diagnostics.WaitUntilReadyOptions.waitUntilReadyOptions;
import static java.util.Objects.requireNonNull;

import java.time.Duration;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Readiness;

import com.couchbase.client.core.Core;
import com.couchbase.client.core.CoreContext;
import com.couchbase.client.core.Reactor;
import com.couchbase.client.core.config.ConfigVersion;
import com.couchbase.client.core.msg.ResponseStatus;
import com.couchbase.client.core.msg.kv.CarrierGlobalConfigRequest;
import com.couchbase.client.core.retry.BestEffortRetryStrategy;
import com.couchbase.client.core.topology.ClusterTopology;
import com.couchbase.client.core.topology.NodeIdentifier;
import com.couchbase.client.java.Cluster;
import com.couchbase.quarkus.extension.runtime.CouchbaseConfig;

import io.smallrye.health.api.AsyncHealthCheck;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.converters.uni.UniReactorConverters;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Readiness
@ApplicationScoped
public class CouchbaseReadyCheck implements AsyncHealthCheck {

    @Inject
    Cluster cluster;

    @Inject
    CouchbaseConfig config;

    /**
     * Wait for a Cluster config, then pings the GCCCP connections on each node.
     * @return A HealthCheckResponse.
     */
    @Override
    public Uni<HealthCheckResponse> call() {
        HealthCheckResponseBuilder builder = HealthCheckResponse.named("Couchbase Cluster Readiness Check");

        return Uni.createFrom()
                .converter(UniReactorConverters.fromMono(), quarkusWaitUntilReady(cluster))
                .map(ignored -> builder.up().build())
                .onFailure().recoverWithItem(throwable -> builder.down()
                        .withData("error", throwable.getMessage())
                        .build());
    }

    public Mono<Void> quarkusWaitUntilReady(Cluster cluster) {
        return cluster.reactive()
                .waitUntilReady(
                        Duration.ofSeconds(config.readinessTimeout()),
                        waitUntilReadyOptions().serviceTypes(KV) // Can't really wait for KV -- this just waits for global config
                )
                .then(Mono.defer(() -> {
                    // Make sure every KV node in the global config is still online
                    ClusterTopology clusterTopology = requireNonNull(
                            cluster.core().configurationProvider().config().globalTopology(), "Cluster topology isn't loaded?");
                    return Flux.fromIterable(clusterTopology.nodes())
                            .filter(it -> it.has(KV))
                            .flatMap(node -> pingGcccp(cluster.core(), node.id()))
                            .then();
                }));
    }

    private Mono<Void> pingGcccp(Core core, NodeIdentifier nodeId) {
        final CoreContext ctx = core.context();
        return Mono.defer(() -> {
            CarrierGlobalConfigRequest request = new CarrierGlobalConfigRequest(
                    ctx.environment().timeoutConfig().connectTimeout(),
                    ctx,
                    BestEffortRetryStrategy.INSTANCE,
                    nodeId,
                    ConfigVersion.ZERO);
            core.send(request);
            return Reactor.wrap(request, request.response(), true);
        }).flatMap(response -> {
            ResponseStatus status = response.status();
            return status.success() || status == ResponseStatus.UNSUPPORTED || status == ResponseStatus.NO_BUCKET
                    ? Mono.empty() // success, or can't ping because GCCCP not supported by this server version.
                    : Mono.error(new RuntimeException("Got unexpected response when pinging GCCCP: " + response));
        });
    }
}
