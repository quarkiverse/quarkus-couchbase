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

import java.util.function.BooleanSupplier;

import com.couchbase.client.core.deps.io.netty.buffer.ByteBuf;
import com.couchbase.client.core.deps.io.netty.channel.ChannelHandlerContext;
import com.couchbase.client.core.deps.io.netty.handler.codec.compression.JdkZlibDecoder;
import com.couchbase.client.core.deps.io.netty.handler.codec.compression.JdkZlibEncoder;
import com.couchbase.client.core.deps.io.netty.handler.codec.compression.ZlibDecoder;
import com.couchbase.client.core.deps.io.netty.handler.codec.compression.ZlibEncoder;
import com.couchbase.client.core.deps.io.netty.handler.codec.compression.ZlibWrapper;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * Keeps the optional jzlib ({@code com.jcraft.jzlib}) and zstd ({@code com.github.luben.zstd})
 * libraries out of the native image. They are not on the classpath, but Netty's codec factories
 * reference them. Quarkus links all dependency classes at build time, so the build fails with an
 * unresolved type (jzlib) or a {@code <clinit>} that calls the missing library (zstd).
 *
 * <p>
 * Native-image metadata cannot change which class a factory method returns, so these are
 * substitutions. Adapted from the Quarkus Netty extension.
 */
public final class NettyCompressionSubstitutions {

    private NettyCompressionSubstitutions() {
    }
}

@TargetClass(className = "com.couchbase.client.core.deps.io.netty.handler.codec.compression.ZlibCodecFactory")
final class Target_io_netty_handler_codec_compression_ZlibCodecFactory {

    @Substitute
    public static ZlibEncoder newZlibEncoder(int compressionLevel) {
        return new JdkZlibEncoder(compressionLevel);
    }

    @Substitute
    public static ZlibEncoder newZlibEncoder(ZlibWrapper wrapper) {
        return new JdkZlibEncoder(wrapper);
    }

    @Substitute
    public static ZlibEncoder newZlibEncoder(ZlibWrapper wrapper, int compressionLevel) {
        return new JdkZlibEncoder(wrapper, compressionLevel);
    }

    @Substitute
    public static ZlibEncoder newZlibEncoder(ZlibWrapper wrapper, int compressionLevel, int windowBits, int memLevel) {
        return new JdkZlibEncoder(wrapper, compressionLevel);
    }

    @Substitute
    public static ZlibEncoder newZlibEncoder(byte[] dictionary) {
        return new JdkZlibEncoder(dictionary);
    }

    @Substitute
    public static ZlibEncoder newZlibEncoder(int compressionLevel, byte[] dictionary) {
        return new JdkZlibEncoder(compressionLevel, dictionary);
    }

    @Substitute
    public static ZlibEncoder newZlibEncoder(int compressionLevel, int windowBits, int memLevel, byte[] dictionary) {
        return new JdkZlibEncoder(compressionLevel, dictionary);
    }

    @Substitute
    public static ZlibDecoder newZlibDecoder() {
        return new JdkZlibDecoder();
    }

    @Substitute
    public static ZlibDecoder newZlibDecoder(ZlibWrapper wrapper) {
        return new JdkZlibDecoder(wrapper);
    }

    @Substitute
    public static ZlibDecoder newZlibDecoder(ZlibWrapper wrapper, int maxAllocation) {
        return new JdkZlibDecoder(wrapper, maxAllocation);
    }

    @Substitute
    public static ZlibDecoder newZlibDecoder(byte[] dictionary) {
        return new JdkZlibDecoder(dictionary);
    }
}

@TargetClass(className = "com.couchbase.client.core.deps.io.netty.handler.codec.compression.ZstdEncoder", onlyWith = IsZstdAbsent.class)
final class Target_io_netty_handler_codec_compression_ZstdEncoder {

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

/**
 * {@code ZstdConstants.<clinit>} calls {@code com.github.luben.zstd.Zstd}.
 */
@Substitute
@TargetClass(className = "com.couchbase.client.core.deps.io.netty.handler.codec.compression.ZstdConstants", onlyWith = IsZstdAbsent.class)
final class Target_io_netty_handler_codec_compression_ZstdConstants {

    static final int DEFAULT_COMPRESSION_LEVEL = 0;

    static final int MIN_COMPRESSION_LEVEL = 0;

    static final int MAX_COMPRESSION_LEVEL = 0;

    static final int MAX_BLOCK_SIZE = 0;

    static final int DEFAULT_BLOCK_SIZE = 0;
}

final class IsZstdAbsent implements BooleanSupplier {

    private final boolean zstdAbsent;

    IsZstdAbsent() {
        boolean absent;
        try {
            Class.forName("com.github.luben.zstd.Zstd");
            absent = false;
        } catch (Throwable e) {
            // Missing class, or no native binding for this OS and architecture.
            absent = true;
        }
        this.zstdAbsent = absent;
    }

    @Override
    public boolean getAsBoolean() {
        return zstdAbsent;
    }
}
