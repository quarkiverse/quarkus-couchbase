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

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Build item to specify the minimal required `io.netty.allocator.maxOrder`.
 *
 * Quarkus by default uses `maxOrder == 1`. Extensions that require a larger value
 * can use this build item to specify it.
 */
public final class MinNettyAllocatorMaxOrderBuildItem extends MultiBuildItem {
    private final int maxOrder;

    public MinNettyAllocatorMaxOrderBuildItem(int maxOrder) {
        this.maxOrder = maxOrder;
    }

    public int getMaxOrder() {
        return maxOrder;
    }
}
