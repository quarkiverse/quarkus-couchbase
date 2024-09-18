package com.couchbase.quarkus.extension.deployment.nettyhandling;

import java.util.function.Supplier;

import com.couchbase.client.core.deps.io.netty.channel.EventLoopGroup;
import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Provides suppliers that return EventLoopGroup used by the application.
 *
 * See EventLoopSupplierBuildItem to register custom EventLoopGroup
 */
public final class EventLoopGroupBuildItem extends SimpleBuildItem {
    private final Supplier<EventLoopGroup> bossEventLoopGroup;
    private final Supplier<EventLoopGroup> mainEventLoopGroup;

    public EventLoopGroupBuildItem(Supplier<EventLoopGroup> boss, Supplier<EventLoopGroup> main) {
        this.bossEventLoopGroup = boss;
        this.mainEventLoopGroup = main;
    }

    public Supplier<EventLoopGroup> getBossEventLoopGroup() {
        return bossEventLoopGroup;
    }

    public Supplier<EventLoopGroup> getMainEventLoopGroup() {
        return mainEventLoopGroup;
    }

}
