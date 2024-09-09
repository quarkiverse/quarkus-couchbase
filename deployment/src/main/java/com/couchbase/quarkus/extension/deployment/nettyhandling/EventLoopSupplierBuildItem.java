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
package com.couchbase.quarkus.extension.deployment.nettyhandling;

import java.util.function.Supplier;

import com.couchbase.client.core.deps.io.netty.channel.EventLoopGroup;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Register EventLoopGroup suppliers to be used to produce main EventLoopGroup and boss EventLoopGroup annotated beans.
 * If not provided, both will be created from a default supplier.
 *
 * See EventLoopGroupBuildItem for actual supplier instances
 */
public final class EventLoopSupplierBuildItem extends SimpleBuildItem {

    private final Supplier<EventLoopGroup> mainSupplier;
    private final Supplier<EventLoopGroup> bossSupplier;

    public EventLoopSupplierBuildItem(Supplier<EventLoopGroup> mainSupplier, Supplier<EventLoopGroup> bossSupplier) {
        this.mainSupplier = mainSupplier;
        this.bossSupplier = bossSupplier;
    }

    public Supplier<EventLoopGroup> getMainSupplier() {
        return mainSupplier;
    }

    public Supplier<EventLoopGroup> getBossSupplier() {
        return bossSupplier;
    }
}
