/*
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
package com.couchbase.quarkus.extension.deployment.nettyhandling;

import java.util.*;
import java.util.function.Supplier;

import jakarta.inject.Singleton;

import org.jboss.logging.Logger;
import org.jboss.logmanager.Level;

import com.couchbase.client.core.deps.io.netty.channel.EventLoopGroup;
import com.couchbase.client.core.deps.io.netty.util.internal.PlatformDependent;
import com.couchbase.client.core.deps.io.netty.util.internal.logging.InternalLoggerFactory;
import com.couchbase.quarkus.extension.runtime.nettyhandling.BossEventLoopGroup;
import com.couchbase.quarkus.extension.runtime.nettyhandling.EmptyByteBufStub;
import com.couchbase.quarkus.extension.runtime.nettyhandling.MainEventLoopGroup;
import com.couchbase.quarkus.extension.runtime.nettyhandling.NettyRecorder;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.SystemPropertyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageConfigBuildItem;
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
        return new SystemPropertyBuildItem("io.netty.allocator.maxOrder", maxOrder);
    }

    @BuildStep
    public SystemPropertyBuildItem setNettyMachineId() {
        // we set the io.netty.machineId system property so to prevent potential
        // slowness when generating/inferring the default machine id in io.netty.channel.DefaultChannelId
        // implementation, which iterates over the NetworkInterfaces to determine the "best" machine id

        // borrowed from io.netty.util.internal.MacAddressUtil.EUI64_MAC_ADDRESS_LENGTH
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
                .addRuntimeInitializedClass(
                        "com.couchbase.client.core.deps.io.netty.handler.ssl.JdkNpnApplicationProtocolNegotiator")
                .addRuntimeInitializedClass("com.couchbase.client.core.deps.io.netty.handler.ssl.ConscryptAlpnSslEngine")
                .addRuntimeInitializedClass("com.couchbase.client.core.deps.io.netty.handler.ssl.ReferenceCountedOpenSslEngine")
                .addRuntimeInitializedClass(
                        "com.couchbase.client.core.deps.io.netty.handler.ssl.ReferenceCountedOpenSslContext")
                .addRuntimeInitializedClass(
                        "com.couchbase.client.core.deps.io.netty.handler.ssl.ReferenceCountedOpenSslClientContext")
                .addRuntimeInitializedClass("com.couchbase.client.core.deps.io.netty.handler.ssl.JdkSslServerContext")
                .addRuntimeInitializedClass("com.couchbase.client.core.deps.io.netty.handler.ssl.JdkSslClientContext")
                .addRuntimeInitializedClass(
                        "com.couchbase.client.core.deps.io.netty.handler.ssl.util.ThreadLocalInsecureRandom")
                .addRuntimeInitializedClass("com.couchbase.client.core.deps.io.netty.buffer.ByteBufUtil$HexUtil")
                .addRuntimeInitializedClass("com.couchbase.client.core.deps.io.netty.buffer.PooledByteBufAllocator")
                .addRuntimeInitializedClass("com.couchbase.client.core.deps.io.netty.buffer.ByteBufAllocator")
                .addRuntimeInitializedClass("com.couchbase.client.core.deps.io.netty.buffer.ByteBufUtil")
                // The default channel id uses the process id, it should not be cached in the native image.
                .addRuntimeInitializedClass("com.couchbase.client.core.deps.io.netty.channel.DefaultChannelId")
                .addNativeImageSystemProperty("com.couchbase.client.core.deps.io.netty.leakDetection.level", "DISABLED");

        if (QuarkusClassLoader
                .isClassPresentAtRuntime("com.couchbase.client.core.deps.io.netty.handler.codec.http.HttpObjectEncoder")) {
            builder
                    .addRuntimeInitializedClass("com.couchbase.client.core.deps.io.netty.handler.codec.http.HttpObjectEncoder")
                    .addRuntimeInitializedClass(
                            "com.couchbase.client.core.deps.io.netty.handler.codec.http.websocketx.extensions.compression.DeflateDecoder")
                    .addRuntimeInitializedClass(
                            "com.couchbase.client.core.deps.io.netty.handler.codec.http.websocketx.WebSocket00FrameEncoder")
                    .addRuntimeInitializedClass("com.couchbase.client.core.deps.io.netty.handler.codec.compression.ZstdOptions")
                    .addRuntimeInitializedClass(
                            "com.couchbase.client.core.deps.io.netty.handler.codec.compression.ZstdConstants")
                    .addRuntimeInitializedClass(
                            "com.couchbase.client.core.deps.io.netty.handler.codec.compression.BrotliOptions");
        } else {
            log.debug("Not registering Netty HTTP classes as they were not found");
        }

        if (QuarkusClassLoader
                .isClassPresentAtRuntime("com.couchbase.client.core.deps.io.netty.handler.codec.http2.Http2CodecUtil")) {
            builder
                    .addRuntimeInitializedClass("com.couchbase.client.core.deps.io.netty.handler.codec.http2.Http2CodecUtil")
                    .addRuntimeInitializedClass(
                            "com.couchbase.client.core.deps.io.netty.handler.codec.http2.Http2ClientUpgradeCodec")
                    .addRuntimeInitializedClass(
                            "com.couchbase.client.core.deps.io.netty.handler.codec.http2.DefaultHttp2FrameWriter")
                    .addRuntimeInitializedClass(
                            "com.couchbase.client.core.deps.io.netty.handler.codec.http2.Http2ConnectionHandler");
        } else {
            log.debug("Not registering Netty HTTP2 classes as they were not found");
        }

        if (QuarkusClassLoader.isClassPresentAtRuntime("com.couchbase.client.core.deps.io.netty.channel.unix.UnixChannel")) {
            builder.addRuntimeInitializedClass("com.couchbase.client.core.deps.io.netty.channel.unix.Errors")
                    .addRuntimeInitializedClass("com.couchbase.client.core.deps.io.netty.channel.unix.FileDescriptor")
                    .addRuntimeInitializedClass("com.couchbase.client.core.deps.io.netty.channel.unix.IovArray")
                    .addRuntimeInitializedClass("com.couchbase.client.core.deps.io.netty.channel.unix.Limits");
        } else {
            log.debug("Not registering Netty native unix classes as they were not found");
        }

        if (QuarkusClassLoader.isClassPresentAtRuntime("com.couchbase.client.core.deps.io.netty.channel.epoll.EpollMode")) {
            builder.addRuntimeInitializedClass("com.couchbase.client.core.deps.io.netty.channel.epoll.Epoll")
                    .addRuntimeInitializedClass("com.couchbase.client.core.deps.io.netty.channel.epoll.EpollEventArray")
                    .addRuntimeInitializedClass("com.couchbase.client.core.deps.io.netty.channel.epoll.EpollEventLoop")
                    .addRuntimeInitializedClass("com.couchbase.client.core.deps.io.netty.channel.epoll.Native");
        } else {
            log.debug("Not registering Netty native epoll classes as they were not found");
        }

        if (QuarkusClassLoader.isClassPresentAtRuntime("com.couchbase.client.core.deps.io.netty.channel.kqueue.AcceptFilter")) {
            builder.addRuntimeInitializedClass("com.couchbase.client.core.deps.io.netty.channel.kqueue.KQueue")
                    .addRuntimeInitializedClass("com.couchbase.client.core.deps.io.netty.channel.kqueue.KQueueEventArray")
                    .addRuntimeInitializedClass("com.couchbase.client.core.deps.io.netty.channel.kqueue.KQueueEventLoop")
                    .addRuntimeInitializedClass("com.couchbase.client.core.deps.io.netty.channel.kqueue.Native");
        } else {
            log.debug("Not registering Netty native kqueue classes as they were not found");
        }

        builder.addRuntimeReinitializedClass("com.couchbase.client.core.deps.io.netty.util.internal.PlatformDependent")
                .addRuntimeReinitializedClass("com.couchbase.client.core.deps.io.netty.util.internal.PlatformDependent0");

        if (QuarkusClassLoader
                .isClassPresentAtRuntime("com.couchbase.client.core.deps.io.netty.buffer.UnpooledByteBufAllocator")) {
            builder.addRuntimeReinitializedClass("com.couchbase.client.core.deps.io.netty.buffer.UnpooledByteBufAllocator")
                    .addRuntimeReinitializedClass("com.couchbase.client.core.deps.io.netty.buffer.Unpooled")
                    .addRuntimeReinitializedClass(
                            "com.couchbase.client.core.deps.io.netty.handler.codec.http.HttpObjectAggregator")
                    .addRuntimeReinitializedClass(
                            "com.couchbase.client.core.deps.io.netty.handler.codec.ReplayingDecoderByteBuf");

            if (QuarkusClassLoader
                    .isClassPresentAtRuntime("org.jboss.resteasy.reactive.client.impl.multipart.QuarkusMultipartFormUpload")) {
                builder.addRuntimeReinitializedClass(
                        "org.jboss.resteasy.reactive.client.impl.multipart.QuarkusMultipartFormUpload");
            }
        }

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
                "com.couchbase.quarkus.extension.runtime.nettyhandling.Holder_io_netty_util_concurrent_ScheduledFutureTask");
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
    //    @BuildStep
    //    LogCleanupFilterBuildItem cleanupMacDNSInLog() {
    //        return new LogCleanupFilterBuildItem(DnsServerAddressStreamProviders.class.getName(), Level.WARN,
    //                "Can not find io.netty.resolver.dns.macos.MacOSDnsServerAddressStreamProvider in the classpath");
    //    }

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
