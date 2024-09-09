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
package com.couchbase.quarkus.extension.runtime.jacksonhandling.spi;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * BuildItem used to signal that some Jackson module has been detected on the classpath
 *
 * The modules are then registered with the ObjectMapper.
 *
 * Note: Modules are assumed to have a default constructor
 */
public final class ClassPathJacksonModuleBuildItem extends MultiBuildItem {

    private final String moduleClassName;

    public ClassPathJacksonModuleBuildItem(String moduleClassName) {
        this.moduleClassName = moduleClassName;
    }

    public String getModuleClassName() {
        return moduleClassName;
    }
}
