package com.couchbase.quarkus.extension.deployment.nettyhandling;

import java.util.OptionalInt;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;

@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
@ConfigMapping(prefix = "couchbase.netty")
public interface NettyBuildTimeConfig {

    /**
     * The value configuring the {@code com.couchbase.client.core.deps.io.netty.allocator.maxOrder} system property of Netty.
     * The default value is {@code 3}.
     *
     * Configuring this property overrides the minimum {@code maxOrder} requested by the extensions.
     *
     * This property affects the memory consumption of the application.
     * It must be used carefully.
     * More details on https://programmer.group/pool-area-of-netty-memory-pool.html.
     */
    OptionalInt allocatorMaxOrder();
}
