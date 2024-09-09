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

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.nativeimage.ExcludeConfigBuildItem;

public class NettyOverrideMetadata {

    static final String NETTY_CODEC_JAR_MATCH_REGEX = "io\\.netty\\.netty-codec";
    static final String NETTY_CODEC_REFLECT_CONFIG_MATCH_REGEX = "/META-INF/native-image/io\\.netty/netty-codec/generated/handlers/reflect-config\\.json";
    static final String NETTY_HANDLER_JAR_MATCH_REGEX = "io\\.netty\\.netty-handler";
    static final String NETTY_HANDLER_REFLECT_CONFIG_MATCH_REGEX = "/META-INF/native-image/io\\.netty/netty-handler/generated/handlers/reflect-config\\.json";

    @BuildStep
    void excludeNettyDirectives(BuildProducer<ExcludeConfigBuildItem> nativeImageExclusions) {
        nativeImageExclusions
                .produce(new ExcludeConfigBuildItem(NETTY_CODEC_JAR_MATCH_REGEX, NETTY_CODEC_REFLECT_CONFIG_MATCH_REGEX));
        nativeImageExclusions
                .produce(new ExcludeConfigBuildItem(NETTY_HANDLER_JAR_MATCH_REGEX, NETTY_HANDLER_REFLECT_CONFIG_MATCH_REGEX));
    }
}
