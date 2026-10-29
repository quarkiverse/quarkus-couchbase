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

import org.jboss.jandex.DotName;
import org.jboss.jandex.Type;

import com.couchbase.client.core.api.kv.CoreKvBinaryOps;
import com.couchbase.client.core.api.kv.CoreKvOps;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.quarkus.extension.runtime.ClusterEnvironmentCustomizer;
import com.couchbase.quarkus.extension.runtime.CouchbaseBuildTimeConfig;
import com.couchbase.quarkus.extension.runtime.CouchbaseRecorder;

import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.NativeImageFeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageProxyDefinitionBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedPackageBuildItem;
import io.quarkus.deployment.metrics.MetricsCapabilityBuildItem;
import io.quarkus.runtime.metrics.MetricsFactory;
import io.quarkus.smallrye.health.deployment.spi.HealthBuildItem;

public class CouchbaseProcessor {

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    public void produceCouchbaseClient(CouchbaseRecorder recorder,
            CouchbaseBuildTimeConfig buildTimeConfig,
            Optional<MetricsCapabilityBuildItem> metricsCapability,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeans) {

        var metricsEnabled = buildTimeConfig.metricsEnabled()
                && metricsCapability.isPresent()
                && metricsCapability.get().metricsSupported(MetricsFactory.MICROMETER);

        syntheticBeans.produce(SyntheticBeanBuildItem
                .configure(Cluster.class)
                .scope(ApplicationScoped.class)
                .unremovable()
                .supplier(recorder.getCluster(metricsEnabled))
                .setRuntimeInit()
                .done());
    }

    // The Cluster supplier looks up customizers at runtime, so Arc sees no injection point for them.
    @BuildStep
    UnremovableBeanBuildItem keepClusterEnvironmentCustomizers() {
        return UnremovableBeanBuildItem.beanTypes(ClusterEnvironmentCustomizer.class);
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    public void produceBucket(CouchbaseRecorder recorder,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeans) {

        syntheticBeans.produce(SyntheticBeanBuildItem
                .configure(Bucket.class)
                .scope(ApplicationScoped.class)
                .unremovable()
                .addInjectionPoint(Type.create(DotName.createSimple(Cluster.class.getName()), Type.Kind.CLASS))
                .createWith(recorder.getBucket())
                .setRuntimeInit()
                .done());
    }

    @BuildStep
    HealthBuildItem addHealthCheck(CouchbaseBuildTimeConfig buildTimeConfig) {
        return new HealthBuildItem("com.couchbase.quarkus.extension.runtime.health.CouchbaseReadyCheck",
                buildTimeConfig.healthEnabled());
    }

    @BuildStep
    void kvProxies(BuildProducer<NativeImageProxyDefinitionBuildItem> proxies) {
        proxies.produce(new NativeImageProxyDefinitionBuildItem(CoreKvOps.class.getName()));
        proxies.produce(new NativeImageProxyDefinitionBuildItem(CoreKvBinaryOps.class.getName()));
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
                        "com.couchbase.client.core.api.search.vector.CoreVectorSearchOptions",
                        // Configuration
                        "com.couchbase.client.core.env.ThresholdRequestTracerConfig$Builder",
                        "com.couchbase.client.core.env.SecurityConfig$Builder",
                        "com.couchbase.client.core.env.TimeoutConfig$Builder",
                        "com.couchbase.client.core.env.ThresholdLoggingTracerConfig$Builder",
                        "com.couchbase.client.core.env.CompressionConfig$Builder",
                        "com.couchbase.client.core.env.OrphanReporterConfig$Builder",
                        "com.couchbase.client.core.env.LoggingMeterConfig$Builder",
                        "com.couchbase.client.core.env.TimerConfig$Builder",
                        "com.couchbase.client.core.env.IoConfig$Builder",
                        "com.couchbase.client.core.env.LoggerConfig$Builder",
                        "com.couchbase.client.core.endpoint.CircuitBreakerConfig$Builder",
                        "com.couchbase.client.core.service.AnalyticsServiceConfig$Builder",
                        "com.couchbase.client.core.service.ViewServiceConfig$Builder",
                        "com.couchbase.client.core.service.KeyValueServiceConfig$Builder",
                        "com.couchbase.client.core.service.SearchServiceConfig$Builder",
                        "com.couchbase.client.core.service.QueryServiceConfig$Builder",
                        "com.couchbase.client.core.service.AbstractPooledEndpointServiceConfig$Builder",
                        "com.couchbase.client.core.env.CoreEnvironment$Builder",
                        "com.couchbase.client.java.env.ClusterEnvironment$Builder"
                }).fields().methods().build();
    }

    @BuildStep
    void configureRuntimeInitializedClasses(BuildProducer<RuntimeInitializedClassBuildItem> runtimeInitialized) {
        // Build-time init of these classes would put a runtime-only object, a live thread or an FFM
        // handle into the image heap.
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
                "com.couchbase.client.core.io.netty.SslHandlerFactory$InitOnDemandHolder",
                "com.couchbase.client.core.cnc.apptelemetry.reporter.AppTelemetryReporterImpl", // has static Random instance
                "com.couchbase.client.core.endpoint.http.CoreHttpRequest$Builder",
                // Its static JsonMapper captures a live ForkJoinWorkerThread.
                "com.couchbase.client.core.topology.JacksonHelper",

                // Stores PooledByteBufAllocator.DEFAULT in a static field. The netty metadata marks
                // that allocator as runtime-init.
                "com.couchbase.client.core.endpoint.BaseEndpoint",

                // Netty selects the FFM buffer cleaner in a native image on JDK 25 and later.
                // CleanerJava24Linker.<clinit> builds malloc and free FFM handles, and PlatformDependent
                // stores the chosen cleaner in a static field. FFM handles cannot live in the image heap.
                "com.couchbase.client.core.deps.io.netty.util.internal.CleanerJava24Linker",
                "com.couchbase.client.core.deps.io.netty.util.internal.PlatformDependent",

                // OpenSSL classes. Their <clinit> calls into tcnative, which cannot run at build time.
                // At runtime OpenSsl.isAvailable() returns false and the SDK uses JDK TLS.
                // Listed per class because handler.ssl also holds JDK TLS classes that must stay
                // build-time init.
                "com.couchbase.client.core.deps.io.netty.handler.ssl.DefaultOpenSslCredential",
                "com.couchbase.client.core.deps.io.netty.handler.ssl.DefaultOpenSslKeyMaterial",
                "com.couchbase.client.core.deps.io.netty.handler.ssl.ExtendedOpenSslSession",
                "com.couchbase.client.core.deps.io.netty.handler.ssl.JdkDelegatingPrivateKeyMethod",
                "com.couchbase.client.core.deps.io.netty.handler.ssl.NonOwnedOpenSslCredential",
                "com.couchbase.client.core.deps.io.netty.handler.ssl.OpenSsl",
                "com.couchbase.client.core.deps.io.netty.handler.ssl.OpenSslApplicationProtocolNegotiator",
                "com.couchbase.client.core.deps.io.netty.handler.ssl.OpenSslAsyncPrivateKeyMethod",
                "com.couchbase.client.core.deps.io.netty.handler.ssl.OpenSslCachingKeyMaterialProvider",
                "com.couchbase.client.core.deps.io.netty.handler.ssl.OpenSslCachingX509KeyManagerFactory",
                "com.couchbase.client.core.deps.io.netty.handler.ssl.OpenSslCertificateCompressionAlgorithm",
                "com.couchbase.client.core.deps.io.netty.handler.ssl.OpenSslCertificateCompressionConfig",
                "com.couchbase.client.core.deps.io.netty.handler.ssl.OpenSslCertificateException",
                "com.couchbase.client.core.deps.io.netty.handler.ssl.OpenSslClientContext",
                "com.couchbase.client.core.deps.io.netty.handler.ssl.OpenSslClientSessionCache",
                "com.couchbase.client.core.deps.io.netty.handler.ssl.OpenSslContext",
                "com.couchbase.client.core.deps.io.netty.handler.ssl.OpenSslContextOption",
                "com.couchbase.client.core.deps.io.netty.handler.ssl.OpenSslCredential",
                "com.couchbase.client.core.deps.io.netty.handler.ssl.OpenSslCredentialBuilder",
                "com.couchbase.client.core.deps.io.netty.handler.ssl.OpenSslCredentialPointer",
                "com.couchbase.client.core.deps.io.netty.handler.ssl.OpenSslDefaultApplicationProtocolNegotiator",
                "com.couchbase.client.core.deps.io.netty.handler.ssl.OpenSslEngine",
                "com.couchbase.client.core.deps.io.netty.handler.ssl.OpenSslInternalSession",
                "com.couchbase.client.core.deps.io.netty.handler.ssl.OpenSslKeyMaterial",
                "com.couchbase.client.core.deps.io.netty.handler.ssl.OpenSslKeyMaterialManager",
                "com.couchbase.client.core.deps.io.netty.handler.ssl.OpenSslKeyMaterialProvider",
                "com.couchbase.client.core.deps.io.netty.handler.ssl.OpenSslNpnApplicationProtocolNegotiator",
                "com.couchbase.client.core.deps.io.netty.handler.ssl.OpenSslParametersUtil",
                "com.couchbase.client.core.deps.io.netty.handler.ssl.OpenSslPrivateKey",
                "com.couchbase.client.core.deps.io.netty.handler.ssl.OpenSslPrivateKeyMethod",
                "com.couchbase.client.core.deps.io.netty.handler.ssl.OpenSslServerContext",
                "com.couchbase.client.core.deps.io.netty.handler.ssl.OpenSslServerSessionContext",
                "com.couchbase.client.core.deps.io.netty.handler.ssl.OpenSslSession",
                "com.couchbase.client.core.deps.io.netty.handler.ssl.OpenSslSessionCache",
                "com.couchbase.client.core.deps.io.netty.handler.ssl.OpenSslSessionContext",
                "com.couchbase.client.core.deps.io.netty.handler.ssl.OpenSslSessionId",
                "com.couchbase.client.core.deps.io.netty.handler.ssl.OpenSslSessionStats",
                "com.couchbase.client.core.deps.io.netty.handler.ssl.OpenSslSessionTicketKey",
                "com.couchbase.client.core.deps.io.netty.handler.ssl.OpenSslX509KeyManagerFactory",
                "com.couchbase.client.core.deps.io.netty.handler.ssl.OpenSslX509TrustManagerWrapper",
                "com.couchbase.client.core.deps.io.netty.handler.ssl.ReferenceCountedOpenSslClientContext",
                "com.couchbase.client.core.deps.io.netty.handler.ssl.ReferenceCountedOpenSslContext",
                "com.couchbase.client.core.deps.io.netty.handler.ssl.ReferenceCountedOpenSslEngine",
                "com.couchbase.client.core.deps.io.netty.handler.ssl.ReferenceCountedOpenSslServerContext");

        for (String className : classes) {
            runtimeInitialized.produce(new RuntimeInitializedClassBuildItem(className));
        }
    }

    @BuildStep
    RuntimeInitializedPackageBuildItem tcnativeRuntimeInit() {
        // Shaded netty-tcnative, the JNI bindings to the BoringSSL library inside the SDK jar.
        // Each <clinit> calls a native method, so build-time init fails. At runtime the load fails
        // and the SDK uses JDK TLS.
        return new RuntimeInitializedPackageBuildItem(
                "com.couchbase.client.core.deps.io.netty.internal.tcnative");
    }

    @BuildStep
    RuntimeInitializedPackageBuildItem kqueueRuntimeInit() {
        // On macOS, build-time init stores KQueue.isAvailable() == true in the image, but the kqueue
        // library is not in the image. The executable then fails at runtime with UnsatisfiedLinkError.
        // Runtime init makes the check run at startup, where it fails and the SDK uses NIO.
        return new RuntimeInitializedPackageBuildItem(
                "com.couchbase.client.core.deps.io.netty.channel.kqueue");
    }

    @BuildStep
    NativeImageResourceBuildItem epollNativeLib() {
        // The SDK relocates the condition in the epoll resource-config.json but not the file pattern,
        // which still names the unshaded library. Without this the shaded library is not in the image
        // and Linux silently falls back to NIO.
        return new NativeImageResourceBuildItem(
                "META-INF/native/libcom_couchbase_client_core_deps_netty_transport_native_epoll_x86_64.so",
                "META-INF/native/libcom_couchbase_client_core_deps_netty_transport_native_epoll_aarch_64.so");
    }

    @BuildStep
    NativeImageFeatureBuildItem registerNettyForeignCleanerFeature() {
        // Registers the FFM descriptors for the runtime-init CleanerJava24Linker above.
        return new NativeImageFeatureBuildItem(
                "com.couchbase.quarkus.extension.runtime.graal.NettyForeignCleanerFeature");
    }

    @BuildStep
    NativeImageResourceBuildItem nativeImageResourceBuildItem() {
        return new NativeImageResourceBuildItem("com/couchbase/client/core/env/capella-ca.pem");
    }
}
