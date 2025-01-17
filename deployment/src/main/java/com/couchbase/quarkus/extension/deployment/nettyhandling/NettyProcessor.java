package com.couchbase.quarkus.extension.deployment.nettyhandling;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Random;
import java.util.function.Supplier;

import jakarta.inject.Singleton;

import org.jboss.logging.Logger;
import org.jboss.logmanager.Level;

import com.couchbase.client.core.deps.io.netty.channel.EventLoopGroup;
import com.couchbase.client.core.deps.io.netty.util.internal.PlatformDependent;
import com.couchbase.client.core.deps.io.netty.util.internal.logging.InternalLoggerFactory;
import com.couchbase.quarkus.extension.runtime.nettyhandling.BossEventLoopGroup;
import com.couchbase.quarkus.extension.runtime.nettyhandling.MainEventLoopGroup;
import com.couchbase.quarkus.extension.runtime.nettyhandling.runtime.EmptyByteBufStub;
import com.couchbase.quarkus.extension.runtime.nettyhandling.runtime.NettyRecorder;

import io.netty.resolver.dns.DnsServerAddressStreamProviders;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.SystemPropertyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageConfigBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageSystemPropertyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeReinitializedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.UnsafeAccessedFieldBuildItem;
import io.quarkus.deployment.logging.LogCleanupFilterBuildItem;

class NettyProcessor {

    private static final Logger log = Logger.getLogger(NettyProcessor.class);

    private static final int DEFAULT_NETTY_ALLOCATOR_MAX_ORDER = 3;

    static {
        InternalLoggerFactory.setDefaultFactory(new JBossNettyLoggerFactory());
    }

    @BuildStep
    public NativeImageSystemPropertyBuildItem limitMem() {
        //in native mode we limit the size of the epoll array
        //if the array overflows the selector just moves the overflow to a map
        return new NativeImageSystemPropertyBuildItem("sun.nio.ch.maxUpdateArraySize", "100");
    }

    @BuildStep
    public SystemPropertyBuildItem limitArenaSize(NettyBuildTimeConfig config,
            List<MinNettyAllocatorMaxOrderBuildItem> minMaxOrderBuildItems) {
        String maxOrder = calculateMaxOrder(config.allocatorMaxOrder, minMaxOrderBuildItems, true);

        //in native mode we limit the size of the epoll array
        //if the array overflows the selector just moves the overflow to a map
        return new SystemPropertyBuildItem("com.couchbase.client.core.deps.io.netty.allocator.maxOrder", maxOrder);
    }

    @BuildStep
    public SystemPropertyBuildItem setNettyMachineId() {
        // we set the com.couchbase.client.core.deps.io.netty.machineId system property so to prevent potential
        // slowness when generating/inferring the default machine id in com.couchbase.client.core.deps.io.netty.channel.DefaultChannelId
        // implementation, which iterates over the NetworkInterfaces to determine the "best" machine id

        // borrowed from com.couchbase.client.core.deps.io.netty.util.internal.MacAddressUtil.EUI64_MAC_ADDRESS_LENGTH
        final int EUI64_MAC_ADDRESS_LENGTH = 8;
        final byte[] machineIdBytes = new byte[EUI64_MAC_ADDRESS_LENGTH];
        new Random().nextBytes(machineIdBytes);
        final String nettyMachineId = com.couchbase.client.core.deps.io.netty.util.internal.MacAddressUtil
                .formatAddress(machineIdBytes);
        return new SystemPropertyBuildItem("com.couchbase.client.core.deps.io.netty.machineId", nettyMachineId);
    }

    @BuildStep
    NativeImageConfigBuildItem build(
            NettyBuildTimeConfig config,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            List<MinNettyAllocatorMaxOrderBuildItem> minMaxOrderBuildItems) {

        reflectiveClass.produce(
                ReflectiveClassBuildItem.builder("com.couchbase.client.core.deps.io.netty.channel.socket.nio.NioSocketChannel")
                        .build());
        reflectiveClass
                .produce(ReflectiveClassBuildItem
                        .builder("com.couchbase.client.core.deps.io.netty.channel.socket.nio.NioServerSocketChannel")
                        .build());
        reflectiveClass.produce(ReflectiveClassBuildItem
                .builder("com.couchbase.client.core.deps.io.netty.channel.socket.nio.NioDatagramChannel")
                .build());
        reflectiveClass
                .produce(ReflectiveClassBuildItem.builder("java.util.LinkedHashMap").build());
        reflectiveClass.produce(ReflectiveClassBuildItem.builder("sun.nio.ch.SelectorImpl").methods().fields().build());

        String maxOrder = calculateMaxOrder(config.allocatorMaxOrder, minMaxOrderBuildItems, false);

        NativeImageConfigBuildItem.Builder builder = NativeImageConfigBuildItem.builder()
                // Use small chunks to avoid a lot of wasted space. Default is 16mb * arenas (derived from core count)
                // Since buffers are cached to threads, the malloc overhead is temporary anyway
                .addNativeImageSystemProperty("com.couchbase.client.core.deps.io.netty.allocator.maxOrder", maxOrder)
                // Runtime initialize to respect com.couchbase.client.core.deps.io.netty.handler.ssl.conscrypt.useBufferAllocator
                .addRuntimeInitializedClass("com.couchbase.client.core.deps.io.netty.handler.ssl.ConscryptAlpnSslEngine")
                // Runtime initialize due to the use of tcnative in the static initializers?
                .addRuntimeInitializedClass("com.couchbase.client.core.deps.io.netty.handler.ssl.ReferenceCountedOpenSslEngine")
                // Runtime initialize to respect run-time provided values of the following properties:
                // - com.couchbase.client.core.deps.io.netty.handler.ssl.openssl.bioNonApplicationBufferSize
                // - com.couchbase.client.core.deps.io.netty.handler.ssl.openssl.useTasks
                // - jdk.tls.client.enableSessionTicketExtension
                // - com.couchbase.client.core.deps.io.netty.handler.ssl.openssl.sessionCacheServer
                // - com.couchbase.client.core.deps.io.netty.handler.ssl.openssl.sessionCacheClient
                // - jdk.tls.ephemeralDHKeySize
                .addRuntimeInitializedClass(
                        "com.couchbase.client.core.deps.io.netty.handler.ssl.ReferenceCountedOpenSslContext")
                // .addRuntimeInitializedClass("com.couchbase.client.core.deps.io.netty.handler.ssl.ReferenceCountedOpenSslClientContext")
                // Runtime initialize to respect run-time provided values of the following properties:
                // - keystore.type
                // - ssl.KeyManagerFactory.algorithm
                // - ssl.TrustManagerFactory.algorithm
                .addRuntimeInitializedClass("com.couchbase.client.core.deps.io.netty.handler.ssl.JdkSslServerContext")
                // .addRuntimeInitializedClass("com.couchbase.client.core.deps.io.netty.handler.ssl.JdkSslClientContext")
                // Runtime initialize to prevent embedding SecureRandom instances in the native image
                .addRuntimeInitializedClass(
                        "com.couchbase.client.core.deps.io.netty.handler.ssl.util.ThreadLocalInsecureRandom")
                // The default channel id uses the process id, it should not be cached in the native image. This way we
                // also respect the run-time provided value of the com.couchbase.client.core.deps.io.netty.processId property, com.couchbase.client.core.deps.io.netty.machineId
                // property is being hardcoded in setNettyMachineId method
                .addRuntimeInitializedClass("com.couchbase.client.core.deps.io.netty.channel.DefaultChannelId")
                // Disable leak detection by default, it can still be enabled via
                // com.couchbase.client.core.deps.io.netty.util.ResourceLeakDetector.setLevel method
                .addNativeImageSystemProperty("com.couchbase.client.core.deps.io.netty.leakDetection.level", "DISABLED");

        if (QuarkusClassLoader
                .isClassPresentAtRuntime("com.couchbase.client.core.deps.io.netty.handler.codec.http.HttpObjectEncoder")) {
            builder
                    // Runtime initialize due to transitive use of the com.couchbase.client.core.deps.io.netty.util.internal.PlatformDependent class
                    // when initializing CRLF_BUF and ZERO_CRLF_CRLF_BUF
                    .addRuntimeInitializedClass("com.couchbase.client.core.deps.io.netty.handler.codec.http.HttpObjectEncoder")
                    .addRuntimeInitializedClass(
                            "com.couchbase.client.core.deps.io.netty.handler.codec.http.websocketx.extensions.compression.DeflateDecoder")
                    .addRuntimeInitializedClass(
                            "com.couchbase.client.core.deps.io.netty.handler.codec.http.websocketx.WebSocket00FrameEncoder");
            // Zstd is an optional dependency, runtime initialize to avoid IllegalStateException when zstd is not
            // available. This will result in a runtime ClassNotFoundException if the user tries to use zstd.
            if (!QuarkusClassLoader.isClassPresentAtRuntime("com.github.luben.zstd.Zstd")) {
                builder.addRuntimeInitializedClass(
                        "com.couchbase.client.core.deps.io.netty.handler.codec.compression.ZstdOptions")
                        .addRuntimeInitializedClass(
                                "com.couchbase.client.core.deps.io.netty.handler.codec.compression.ZstdConstants");
            }
            // Brotli is an optional dependency, we should only runtime initialize BrotliOptions to avoid
            // IllegalStateException when brotli (e.g. com.aayushatharva.brotli4j.Brotli4jLoader) is not available.
            // This will result in a runtime ClassNotFoundException if the user tries to use Brotli.
            // Due to https://github.com/quarkusio/quarkus/issues/43662 we cannot do this yet though so we always enable
            // runtime initialization of BrotliOptions if the class is present
            if (QuarkusClassLoader.isClassPresentAtRuntime(
                    "com.couchbase.client.core.deps.io.netty.handler.codec.compression.BrotliOptions")) {
                builder.addRuntimeInitializedClass(
                        "com.couchbase.client.core.deps.io.netty.handler.codec.compression.BrotliOptions");
            }
        } else {
            log.debug("Not registering Netty HTTP classes as they were not found");
        }

        if (QuarkusClassLoader
                .isClassPresentAtRuntime("com.couchbase.client.core.deps.io.netty.handler.codec.http2.Http2CodecUtil")) {
            builder
                    // Runtime initialize due to the transitive use of the com.couchbase.client.core.deps.io.netty.util.internal.PlatformDependent
                    // class in the static initializers
                    .addRuntimeInitializedClass("com.couchbase.client.core.deps.io.netty.handler.codec.http2.Http2CodecUtil")
                    .addRuntimeInitializedClass(
                            "com.couchbase.client.core.deps.io.netty.handler.codec.http2.DefaultHttp2FrameWriter")
                    .addRuntimeInitializedClass(
                            "com.couchbase.client.core.deps.io.netty.handler.codec.http2.Http2ConnectionHandler")
                    // Runtime initialize due to dependency on com.couchbase.client.core.deps.io.netty.handler.codec.http2.Http2CodecUtil
                    .addRuntimeInitializedClass(
                            "com.couchbase.client.core.deps.io.netty.handler.codec.http2.Http2ClientUpgradeCodec");
        } else {
            log.debug("Not registering Netty HTTP2 classes as they were not found");
        }

        if (QuarkusClassLoader.isClassPresentAtRuntime("com.couchbase.client.core.deps.io.netty.channel.unix.UnixChannel")) {
            // Runtime initialize to avoid embedding quite a few Strings in the image heap
            builder.addRuntimeInitializedClass("com.couchbase.client.core.deps.io.netty.channel.unix.Errors")
                    // Runtime initialize due to the use of AtomicIntegerFieldUpdater?
                    .addRuntimeInitializedClass("com.couchbase.client.core.deps.io.netty.channel.unix.FileDescriptor")
                    // Runtime initialize due to the use of Buffer.addressSize() in the static initializers
                    .addRuntimeInitializedClass("com.couchbase.client.core.deps.io.netty.channel.unix.IovArray")
                    // Runtime initialize due to the use of native methods in the static initializers?
                    .addRuntimeInitializedClass("com.couchbase.client.core.deps.io.netty.channel.unix.Limits");
        } else {
            log.debug("Not registering Netty native unix classes as they were not found");
        }

        if (QuarkusClassLoader.isClassPresentAtRuntime("com.couchbase.client.core.deps.io.netty.channel.epoll.EpollMode")) {
            // Runtime initialize due to machine dependent native methods being called in static initializer and to
            // respect the run-time provided value of com.couchbase.client.core.deps.io.netty.transport.noNative
            builder.addRuntimeInitializedClass("com.couchbase.client.core.deps.io.netty.channel.epoll.Epoll")
                    // Runtime initialize due to machine dependent native methods being called in static initializer
                    .addRuntimeInitializedClass("com.couchbase.client.core.deps.io.netty.channel.epoll.EpollEventArray")
                    // Runtime initialize due to dependency on Epoll and to respect the run-time provided value of
                    // com.couchbase.client.core.deps.io.netty.channel.epoll.epollWaitThreshold
                    .addRuntimeInitializedClass("com.couchbase.client.core.deps.io.netty.channel.epoll.EpollEventLoop")
                    // Runtime initialize due to InetAddress fields, dependencies on native methods and to transitively
                    // respect a number of properties, e.g. java.nio.channels.spi.SelectorProvider
                    .addRuntimeInitializedClass("com.couchbase.client.core.deps.io.netty.channel.epoll.Native");
        } else {
            log.debug("Not registering Netty native epoll classes as they were not found");
        }

        if (QuarkusClassLoader.isClassPresentAtRuntime("com.couchbase.client.core.deps.io.netty.channel.kqueue.AcceptFilter")) {
            // Runtime initialize due to machine dependent native methods being called in static initializer and to
            // respect the run-time provided value of com.couchbase.client.core.deps.io.netty.transport.noNative
            builder.addRuntimeInitializedClass("com.couchbase.client.core.deps.io.netty.channel.kqueue.KQueue")
                    // Runtime initialize due to machine dependent native methods being called in static initializers
                    .addRuntimeInitializedClass("com.couchbase.client.core.deps.io.netty.channel.kqueue.KQueueEventArray")
                    .addRuntimeInitializedClass("com.couchbase.client.core.deps.io.netty.channel.kqueue.Native")
                    // Runtime initialize due to dependency on Epoll and the use of AtomicIntegerFieldUpdater?
                    .addRuntimeInitializedClass("com.couchbase.client.core.deps.io.netty.channel.kqueue.KQueueEventLoop");
        } else {
            log.debug("Not registering Netty native kqueue classes as they were not found");
        }

        // Runtime initialize due to platform dependent initialization and to respect the run-time provided value of the
        // properties:
        // - com.couchbase.client.core.deps.io.netty.maxDirectMemory
        // - com.couchbase.client.core.deps.io.netty.uninitializedArrayAllocationThreshold
        // - com.couchbase.client.core.deps.io.netty.noPreferDirect
        // - com.couchbase.client.core.deps.io.netty.osClassifiers
        // - com.couchbase.client.core.deps.io.netty.tmpdir
        // - java.io.tmpdir
        // - com.couchbase.client.core.deps.io.netty.bitMode
        // - sun.arch.data.model
        // - com.ibm.vm.bitmode
        builder.addRuntimeReinitializedClass("com.couchbase.client.core.deps.io.netty.util.internal.PlatformDependent")
                // Similarly for properties:
                // - com.couchbase.client.core.deps.io.netty.noUnsafe
                // - sun.misc.unsafe.memory.access
                // - com.couchbase.client.core.deps.io.netty.tryUnsafe
                // - org.jboss.netty.tryUnsafe
                // - com.couchbase.client.core.deps.io.netty.tryReflectionSetAccessible
                .addRuntimeReinitializedClass("com.couchbase.client.core.deps.io.netty.util.internal.PlatformDependent0");

        //        if (QuarkusClassLoader
        //                .isClassPresentAtRuntime("com.couchbase.client.core.deps.io.netty.buffer.UnpooledByteBufAllocator")) {
        //            // Runtime initialize due to the use of the com.couchbase.client.core.deps.io.netty.util.internal.PlatformDependent class
        //            builder.addRuntimeReinitializedClass("com.couchbase.client.core.deps.io.netty.buffer.UnpooledByteBufAllocator")
        //                    .addRuntimeReinitializedClass("com.couchbase.client.core.deps.io.netty.buffer.Unpooled")
        //                    // Runtime initialize due to dependency on com.couchbase.client.core.deps.io.netty.buffer.Unpooled
        //                    .addRuntimeReinitializedClass(
        //                            "com.couchbase.client.core.deps.io.netty.handler.codec.http.HttpObjectAggregator")
        //                    .addRuntimeReinitializedClass(
        //                            "com.couchbase.client.core.deps.io.netty.handler.codec.ReplayingDecoderByteBuf")
        //                    // Runtime initialize to avoid embedding quite a few Strings in the image heap
        //                    .addRuntimeInitializedClass("com.couchbase.client.core.deps.io.netty.buffer.ByteBufUtil$HexUtil")
        //                    // Runtime initialize due to the use of the com.couchbase.client.core.deps.io.netty.util.internal.PlatformDependent class in the
        //                    // static initializers and to respect the run-time provided value of the following properties:
        //                    // - com.couchbase.client.core.deps.io.netty.allocator.directMemoryCacheAlignment
        //                    // - com.couchbase.client.core.deps.io.netty.allocator.pageSize
        //                    // - com.couchbase.client.core.deps.io.netty.allocator.maxOrder
        //                    // - com.couchbase.client.core.deps.io.netty.allocator.numHeapArenas
        //                    // - com.couchbase.client.core.deps.io.netty.allocator.numDirectArenas
        //                    // - com.couchbase.client.core.deps.io.netty.allocator.smallCacheSize
        //                    // - com.couchbase.client.core.deps.io.netty.allocator.normalCacheSize
        //                    // - com.couchbase.client.core.deps.io.netty.allocator.maxCachedBufferCapacity
        //                    // - com.couchbase.client.core.deps.io.netty.allocator.cacheTrimInterval
        //                    // - com.couchbase.client.core.deps.io.netty.allocation.cacheTrimIntervalMillis
        //                    // - com.couchbase.client.core.deps.io.netty.allocator.cacheTrimIntervalMillis
        //                    // - com.couchbase.client.core.deps.io.netty.allocator.useCacheForAllThreads
        //                    // - com.couchbase.client.core.deps.io.netty.allocator.maxCachedByteBuffersPerChunk
        //                    .addRuntimeInitializedClass("com.couchbase.client.core.deps.io.netty.buffer.PooledByteBufAllocator")
        //                    // Runtime initialize due to the use of ByteBufUtil.DEFAULT_ALLOCATOR in the static initializers
        //                    .addRuntimeInitializedClass("com.couchbase.client.core.deps.io.netty.buffer.ByteBufAllocator")
        //                    // Runtime initialize due to the use of the com.couchbase.client.core.deps.io.netty.util.internal.PlatformDependent class in the
        //                    // static initializers and to respect the run-time provided value of the following properties:
        //                    // - com.couchbase.client.core.deps.io.netty.allocator.type
        //                    // - com.couchbase.client.core.deps.io.netty.threadLocalDirectBufferSize
        //                    // - com.couchbase.client.core.deps.io.netty.maxThreadLocalCharBufferSize
        //                    .addRuntimeInitializedClass("com.couchbase.client.core.deps.io.netty.buffer.ByteBufUtil");
        //
        //            if (QuarkusClassLoader
        //                    .isClassPresentAtRuntime("org.jboss.resteasy.reactive.client.impl.multipart.QuarkusMultipartFormUpload")) {
        //                // Runtime initialize due to dependency on com.couchbase.client.core.deps.io.netty.buffer.Unpooled
        //                builder.addRuntimeReinitializedClass(
        //                        "org.jboss.resteasy.reactive.client.impl.multipart.QuarkusMultipartFormUpload");
        //            }
        //        }

        return builder //TODO: make configurable
                .build();
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    public void eagerlyInitClass(NettyRecorder recorder) {
        //see https://github.com/quarkusio/quarkus/issues/3663
        //this class is slow to initialize, we make sure that we do it eagerly
        //before it blocks the IO thread and causes a warning
        recorder.eagerlyInitChannelId();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void registerEventLoopBeans(BuildProducer<SyntheticBeanBuildItem> syntheticBeans,
            Optional<EventLoopSupplierBuildItem> loopSupplierBuildItem,
            NettyRecorder recorder,
            BuildProducer<EventLoopGroupBuildItem> eventLoopGroups) {
        Supplier<EventLoopGroup> boss;
        Supplier<EventLoopGroup> main;
        if (loopSupplierBuildItem.isPresent()) {
            boss = (Supplier) loopSupplierBuildItem.get().getBossSupplier();
            main = (Supplier) loopSupplierBuildItem.get().getMainSupplier();
        } else {
            boss = recorder.createEventLoop(1);
            main = recorder.createEventLoop(0);
        }

        // IMPLEMENTATION NOTE:
        // We use Singleton scope for both beans. ApplicationScoped causes problems with EventLoopGroup.next()
        // which overrides the EventExecutorGroup.next() method but since Netty 4 is compiled with JDK6 the corresponding bridge method
        // is not generated and the invocation upon the client proxy results in an AbstractMethodError
        syntheticBeans.produce(SyntheticBeanBuildItem.configure(EventLoopGroup.class)
                .supplier(boss)
                .scope(Singleton.class)
                .addQualifier(BossEventLoopGroup.class)
                .unremovable()
                .setRuntimeInit()
                .done());
        syntheticBeans.produce(SyntheticBeanBuildItem.configure(EventLoopGroup.class)
                .supplier(main)
                .scope(Singleton.class)
                .addQualifier(MainEventLoopGroup.class)
                .unremovable()
                .setRuntimeInit()
                .done());

        eventLoopGroups.produce(new EventLoopGroupBuildItem(boss, main));
    }

    @BuildStep
    AdditionalBeanBuildItem registerQualifiers() {
        // We need to register the qualifiers manually because they're not part of the index
        // Previously they were indexed because we indexed the "uber-producer-class" generated for RuntimeBeanBuildItems
        return AdditionalBeanBuildItem.builder().addBeanClasses(BossEventLoopGroup.class, MainEventLoopGroup.class).build();
    }

    @BuildStep
    public RuntimeReinitializedClassBuildItem reinitScheduledFutureTask() {
        return new RuntimeReinitializedClassBuildItem(
                "com.couchbase.quarkus.extension.runtime.nettyhandling.runtime.graal.Holder_io_netty_util_concurrent_ScheduledFutureTask");
    }

    @BuildStep
    public List<UnsafeAccessedFieldBuildItem> unsafeAccessedFields() {
        return Arrays.asList(
                new UnsafeAccessedFieldBuildItem("sun.nio.ch.SelectorImpl", "selectedKeys"),
                new UnsafeAccessedFieldBuildItem("sun.nio.ch.SelectorImpl", "publicSelectedKeys"),
                new UnsafeAccessedFieldBuildItem(
                        "com.couchbase.client.core.deps.io.netty.util.internal.shaded.org.jctools.util.UnsafeRefArrayAccess",
                        "REF_ELEMENT_SHIFT"));
    }

    @BuildStep
    RuntimeInitializedClassBuildItem runtimeInitBcryptUtil() {
        // this holds a direct allocated byte buffer that needs to be initialised at run time
        return new RuntimeInitializedClassBuildItem(EmptyByteBufStub.class.getName());
    }

    //if debug logging is enabled netty logs lots of exceptions
    //see https://github.com/quarkusio/quarkus/issues/5213
    @BuildStep
    LogCleanupFilterBuildItem cleanupUnsafeLog() {
        return new LogCleanupFilterBuildItem(PlatformDependent.class.getName() + "0", Level.TRACE, "direct buffer constructor",
                "jdk.internal.misc.Unsafe", "sun.misc.Unsafe");
    }

    /**
     * On mac, if you do not have the `MacOSDnsServerAddressStreamProvider` class, Netty prints a warning saying it
     * falls back to the default system DNS provider. This is not a problem and generates tons of questions.
     *
     * @return the log cleanup item removing the message
     */
    @BuildStep
    LogCleanupFilterBuildItem cleanupMacDNSInLog() {
        return new LogCleanupFilterBuildItem(DnsServerAddressStreamProviders.class.getName(), Level.WARN,
                "Can not find com.couchbase.client.core.deps.io.netty.resolver.dns.macos.MacOSDnsServerAddressStreamProvider in the classpath");
    }

    /**
     * `Version.identify()` in netty-common uses the resource to determine the version of netty.
     */
    @BuildStep
    NativeImageResourceBuildItem nettyVersions() {
        return new NativeImageResourceBuildItem("META-INF/com.couchbase.client.core.deps.io.netty.versions.properties");
    }

    private String calculateMaxOrder(OptionalInt userConfig, List<MinNettyAllocatorMaxOrderBuildItem> minMaxOrderBuildItems,
            boolean shouldWarn) {
        int result = DEFAULT_NETTY_ALLOCATOR_MAX_ORDER;
        for (MinNettyAllocatorMaxOrderBuildItem minMaxOrderBuildItem : minMaxOrderBuildItems) {
            if (minMaxOrderBuildItem.getMaxOrder() > result) {
                result = minMaxOrderBuildItem.getMaxOrder();
            }
        }

        if (userConfig.isPresent()) {
            int v = userConfig.getAsInt();
            if (result > v && shouldWarn) {
                log.warnf(
                        "The configuration set `quarkus.netty.allocator-max-order` to %d. This value is lower than the value requested by the extensions (%d). %d will be used anyway.",
                        v, result, v);

            }
            return Integer.toString(v);
        }

        return Integer.toString(result);
    }
}
