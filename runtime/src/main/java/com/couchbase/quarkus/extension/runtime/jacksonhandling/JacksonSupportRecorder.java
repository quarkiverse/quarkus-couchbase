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
package com.couchbase.quarkus.extension.runtime.jacksonhandling;

import java.util.Optional;
import java.util.function.Supplier;

import com.couchbase.client.core.deps.com.fasterxml.jackson.databind.PropertyNamingStrategies;

import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class JacksonSupportRecorder {

    public Supplier<JacksonSupport> supplier(Optional<String> propertyNamingStrategyClassName) {
        return new Supplier<>() {
            @Override
            public JacksonSupport get() {
                return new JacksonSupport() {
                    @Override
                    public Optional<PropertyNamingStrategies.NamingBase> configuredNamingStrategy() {
                        if (propertyNamingStrategyClassName.isPresent()) {
                            try {
                                var value = (PropertyNamingStrategies.NamingBase) Class
                                        .forName(propertyNamingStrategyClassName.get(), true,
                                                Thread.currentThread()
                                                        .getContextClassLoader())
                                        .getDeclaredConstructor().newInstance();
                                return Optional.of(value);
                            } catch (Exception e) {
                                // shouldn't happen as propertyNamingStrategyClassName is validated at build time
                                throw new RuntimeException(e);
                            }
                        }
                        return Optional.empty();
                    }
                };
            }
        };
    }
}
