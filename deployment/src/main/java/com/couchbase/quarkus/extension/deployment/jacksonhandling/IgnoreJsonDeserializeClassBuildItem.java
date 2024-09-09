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
package com.couchbase.quarkus.extension.deployment.jacksonhandling;

import java.util.List;

import org.jboss.jandex.DotName;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Used when an extension needs to inform the Jackson extension that a class should not
 * be registered for reflection even if it annotated with @JsonDeserialize
 */
public final class IgnoreJsonDeserializeClassBuildItem extends MultiBuildItem {

    private final List<DotName> dotNames;

    public IgnoreJsonDeserializeClassBuildItem(DotName dotName) {
        this.dotNames = List.of(dotName);
    }

    public IgnoreJsonDeserializeClassBuildItem(List<DotName> dotNames) {
        this.dotNames = dotNames;
    }

    @Deprecated(forRemoval = true)
    public DotName getDotName() {
        return dotNames.size() > 0 ? dotNames.get(0) : null;
    }

    public List<DotName> getDotNames() {
        return dotNames;
    }
}
