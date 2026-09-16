/*
 * Copyright (c) 2026 Couchbase, Inc.
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
package com.couchbase.quarkus.extension.runtime.graal;

import javax.net.ssl.SSLEngine;

import com.couchbase.client.core.deps.io.netty.buffer.ByteBufAllocator;
import com.couchbase.client.core.deps.io.netty.handler.ssl.JdkApplicationProtocolNegotiator;
import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * Netty's JDK ALPN wrapper can call Conscrypt ({@code org.conscrypt}), which is not on the
 * classpath. Quarkus links all dependency classes at build time, so the build fails with an
 * unresolved method. This substitution always builds the JDK ALPN engine, so the Conscrypt branch
 * is not linked. Adapted from the Quarkus Netty extension. Signatures verified against Netty 4.2.19.
 */
public final class NettySslSubstitutions {

    private NettySslSubstitutions() {
    }
}

@TargetClass(className = "com.couchbase.client.core.deps.io.netty.handler.ssl.JdkAlpnApplicationProtocolNegotiator$AlpnWrapper")
final class Target_io_netty_handler_ssl_JdkAlpnApplicationProtocolNegotiator_AlpnWrapper {

    @Substitute
    public SSLEngine wrapSslEngine(SSLEngine engine, ByteBufAllocator alloc,
            JdkApplicationProtocolNegotiator applicationNegotiator, boolean isServer) {
        return (SSLEngine) (Object) new Target_io_netty_handler_ssl_JdkAlpnSslEngine(engine, applicationNegotiator,
                isServer);
    }
}

@TargetClass(className = "com.couchbase.client.core.deps.io.netty.handler.ssl.JdkAlpnSslEngine")
final class Target_io_netty_handler_ssl_JdkAlpnSslEngine {

    @Alias
    Target_io_netty_handler_ssl_JdkAlpnSslEngine(final SSLEngine engine,
            final JdkApplicationProtocolNegotiator applicationNegotiator, final boolean isServer) {
    }
}
