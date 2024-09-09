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
package com.couchbase.quarkus.extension.runtime.nettyhandling;

import java.util.function.BooleanSupplier;

import com.couchbase.client.core.deps.io.netty.buffer.ByteBuf;
import com.couchbase.client.core.deps.io.netty.channel.ChannelHandlerContext;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

public class HttpContentCompressorSubstitutions {

    @TargetClass(className = "com.couchbase.client.core.deps.io.netty.handler.codec.compression.ZstdEncoder", onlyWith = IsZstdAbsent.class)
    public static final class ZstdEncoderFactorySubstitution {

        @Substitute
        protected ByteBuf allocateBuffer(ChannelHandlerContext ctx, ByteBuf msg, boolean preferDirect) throws Exception {
            throw new UnsupportedOperationException();
        }

        @Substitute
        protected void encode(ChannelHandlerContext ctx, ByteBuf in, ByteBuf out) {
            throw new UnsupportedOperationException();
        }

        @Substitute
        public void flush(final ChannelHandlerContext ctx) {
            throw new UnsupportedOperationException();
        }
    }

    @Substitute
    @TargetClass(className = "com.couchbase.client.core.deps.io.netty.handler.codec.compression.ZstdConstants", onlyWith = IsZstdAbsent.class)
    public static final class ZstdConstants {

        // The constants make <clinit> calls to com.github.luben.zstd.Zstd so we cut links with that substitution.

        static final int DEFAULT_COMPRESSION_LEVEL = 0;

        static final int MIN_COMPRESSION_LEVEL = 0;

        static final int MAX_COMPRESSION_LEVEL = 0;

        static final int MAX_BLOCK_SIZE = 0;

        static final int DEFAULT_BLOCK_SIZE = 0;
    }

    public static class IsZstdAbsent implements BooleanSupplier {

        private boolean zstdAbsent;

        public IsZstdAbsent() {
            try {
                Class.forName("com.github.luben.zstd.Zstd");
                zstdAbsent = false;
            } catch (Exception e) {
                // It can be a classloading issue (the library is not available), or a native issue
                // (the library for the current OS/arch is not available)
                zstdAbsent = true;
            }
        }

        @Override
        public boolean getAsBoolean() {
            return zstdAbsent;
        }
    }
}
