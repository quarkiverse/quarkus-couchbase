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
