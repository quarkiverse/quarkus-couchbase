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

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;

import com.couchbase.client.core.api.kv.CoreKvBinaryOps;
import com.couchbase.client.core.api.kv.CoreKvOps;
import com.couchbase.client.java.Cluster;
import com.couchbase.quarkus.extension.runtime.CouchbaseConfig;
import com.couchbase.quarkus.extension.runtime.CouchbaseRecorder;

import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.nativeimage.NativeImageConfigBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageProxyDefinitionBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.deployment.metrics.MetricsCapabilityBuildItem;
import io.quarkus.runtime.metrics.MetricsFactory;
import io.quarkus.smallrye.health.deployment.spi.HealthBuildItem;

public class CouchbaseProcessor {

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    public void produceCouchbaseClient(CouchbaseRecorder recorder,
            CouchbaseConfig config,
            Optional<MetricsCapabilityBuildItem> metricsCapability,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeans) {

        var metricsEnabled = config.metricsEnabled()
                && metricsCapability.isPresent()
                && metricsCapability.get().metricsSupported(MetricsFactory.MICROMETER);

        syntheticBeans.produce(SyntheticBeanBuildItem
                .configure(Cluster.class)
                .scope(ApplicationScoped.class)
                .unremovable()
                .supplier(recorder.getCluster(config, metricsEnabled))
                .setRuntimeInit()
                .done());
    }

    @BuildStep
    HealthBuildItem addHealthCheck(CouchbaseConfig couchbaseConfig) {
        return new HealthBuildItem("com.couchbase.quarkus.extension.runtime.health.CouchbaseReadyCheck",
                couchbaseConfig.healthEnabled());
    }

    @BuildStep
    NativeImageProxyDefinitionBuildItem kvProxies() {
        return new NativeImageProxyDefinitionBuildItem(
                CoreKvOps.class.getName(),
                CoreKvBinaryOps.class.getName());
    }

    @BuildStep
    NativeImageConfigBuildItem nativeConfiguration() {
        NativeImageConfigBuildItem.Builder builder = NativeImageConfigBuildItem.builder();
        // NettyProcessor configures UnpooledByteBufAllocator and other related Netty classes for runtime initialization,
        // but an instance of it is making its way into the native heap through CoreHttpRequest$Builder.EMPTY_BYTE_BUFF.
        // Therefore, we defer its initialization until runtime.
        builder.addRuntimeInitializedClass("com.couchbase.client.core.endpoint.http.CoreHttpRequest$Builder");
        return builder.build();
    }

    @BuildStep
    ReflectiveClassBuildItem reflection() {
        return ReflectiveClassBuildItem.builder(
                new String[] {
                        //Transactions
                        "com.couchbase.client.core.util.ReactorOps",
                        //Error
                        "com.couchbase.client.core.transaction.components.ActiveTransactionRecordEntry",
                        "com.couchbase.client.core.transaction.components.DocRecord",
                        //Bucket Manager
                        "com.couchbase.client.core.manager.bucket.CoreCompressionMode",
                        "com.couchbase.client.core.manager.bucket.CoreEvictionPolicyType",
                        "com.couchbase.client.core.manager.bucket.CoreStorageBackend",
                        "com.couchbase.client.core.manager.bucket.CoreConflictResolutionType",
                        "com.couchbase.client.core.manager.bucket.BucketSettings",
                        "com.couchbase.client.java.manager.bucket.BucketSettings",
                        "com.couchbase.client.core.manager.bucket.CoreBucketSettings",
                        "com.couchbase.client.core.classic.manager.CoreBucketSettingsJson",
                        "com.couchbase.client.core.config.BucketType",
                        "com.couchbase.client.core.msg.kv.DurabilityLevel",

                        "com.couchbase.client.core.config.CollectionsManifestCollection",
                        "com.couchbase.client.core.config.CollectionsManifestScope",
                        "com.couchbase.client.core.config.CollectionsManifest",
                        "com.couchbase.client.core.error.ErrorCodeAndMessage",
                        "com.couchbase.client.core.msg.BaseResponse",
                        "com.couchbase.client.core.endpoint.http.CoreHttpResponse",
                        "com.couchbase.client.core.endpoint.http.CoreCommonOptions",
                        "com.couchbase.client.core.logging.RedactableArgument",
                        "com.couchbase.client.core.msg.CancellationReason",
                        "com.couchbase.client.core.api.manager.search.CoreSearchIndex",
                        "com.couchbase.client.java.manager.search.SearchIndex",
                        //Search
                        //Result
                        "com.couchbase.client.core.api.search.result.CoreAbstractSearchFacetResult",
                        "com.couchbase.client.core.api.search.result.CoreDateRangeSearchFacetResult",
                        "com.couchbase.client.core.api.search.result.CoreNumericRangeSearchFacetResult",
                        "com.couchbase.client.core.api.search.result.CoreReactiveSearchResult",
                        "com.couchbase.client.core.api.search.result.CoreSearchDateRange",
                        "com.couchbase.client.core.api.search.result.CoreSearchFacetResult",
                        "com.couchbase.client.core.api.search.result.CoreSearchMetrics",
                        "com.couchbase.client.core.api.search.result.CoreSearchNumericRange",
                        "com.couchbase.client.core.api.search.result.CoreSearchResult",
                        "com.couchbase.client.core.api.search.result.CoreSearchRow",
                        "com.couchbase.client.core.api.search.result.CoreSearchRowLocation",
                        "com.couchbase.client.core.api.search.result.CoreSearchRowLocations",
                        "com.couchbase.client.core.api.search.result.CoreSearchStatus",
                        "com.couchbase.client.core.api.search.result.CoreSearchTermRange",
                        "com.couchbase.client.core.api.search.result.CoreTermSearchFacetResult",
                        //Facet
                        "com.couchbase.client.core.api.search.facet.CoreDateRange",
                        "com.couchbase.client.core.api.search.facet.CoreDateRangeFacet",
                        "com.couchbase.client.core.api.search.facet.CoreNumericRange",
                        "com.couchbase.client.core.api.search.facet.CoreNumericRangeFacet",
                        "com.couchbase.client.core.api.search.facet.CoreSearchFacet",
                        "com.couchbase.client.core.api.search.facet.CoreTermFacet",
                        //Vector
                        "com.couchbase.client.core.api.search.vector.CoreVector",
                        "com.couchbase.client.core.api.search.vector.CoreVectorQuery",
                        "com.couchbase.client.core.api.search.vector.CoreVectorQueryCombination",
                        "com.couchbase.client.core.api.search.vector.CoreVectorSearch",
                        "com.couchbase.client.core.api.search.vector.CoreVectorSearchOptions"
                }).fields().methods().build();
    }

    @BuildStep
    void configureRuntimeInitializedClasses(BuildProducer<RuntimeInitializedClassBuildItem> runtimeInitialized) {
        List<String> classes = Arrays.asList(
                "com.couchbase.client.core.deps.org.xbill.DNS.config.WindowsResolverConfigProvider$InnerWindowsResolverConfigProvider",
                "com.couchbase.client.core.deps.io.netty.util.NetUtilSubstitutions$NetUtilNetworkInterfacesLazyHolder",
                "com.couchbase.client.core.deps.org.xbill.DNS.SimpleResolver",
                "com.couchbase.client.core.deps.org.xbill.DNS.NioUdpClient",
                "com.couchbase.client.core.deps.io.netty.util.NetUtilSubstitutions$NetUtilLocalhost4LazyHolder",
                "com.couchbase.client.core.deps.org.xbill.DNS.Header", // has static Random instance
                "com.couchbase.client.core.deps.org.xbill.DNS.ResolverConfig",
                "com.couchbase.client.core.deps.org.xbill.DNS.config.WindowsResolverConfigProvider",
                "com.couchbase.client.core.deps.io.grpc.internal.RetriableStream",
                "com.couchbase.client.core.msg.kv.RangeScanCreateRequest",
                "com.couchbase.client.core.deps.org.xbill.DNS.io.DefaultIoClientFactory",
                "com.couchbase.client.core.deps.io.netty.handler.ssl.OpenSsl",
                "com.couchbase.client.core.deps.io.netty.internal.tcnative.SSL",
                "com.couchbase.client.core.deps.io.netty.internal.tcnative.CertificateVerifier",
                "com.couchbase.client.core.io.netty.SslHandlerFactory$InitOnDemandHolder",
                "com.couchbase.client.core.cnc.apptelemetry.reporter.AppTelemetryReporterImpl");// has static Random instance

        for (String className : classes) {
            runtimeInitialized.produce(new RuntimeInitializedClassBuildItem(className));
        }
    }

}
