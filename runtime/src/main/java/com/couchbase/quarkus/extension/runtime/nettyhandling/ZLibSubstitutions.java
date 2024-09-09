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
package com.couchbase.quarkus.extension.runtime.nettyhandling;

import com.couchbase.client.core.deps.io.netty.handler.codec.compression.JdkZlibDecoder;
import com.couchbase.client.core.deps.io.netty.handler.codec.compression.JdkZlibEncoder;
import com.couchbase.client.core.deps.io.netty.handler.codec.compression.ZlibDecoder;
import com.couchbase.client.core.deps.io.netty.handler.codec.compression.ZlibEncoder;
import com.couchbase.client.core.deps.io.netty.handler.codec.compression.ZlibWrapper;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * This substitution avoid having jcraft zlib added to the build
 */
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
    public static ZlibDecoder newZlibDecoder(byte[] dictionary) {
        return new JdkZlibDecoder(dictionary);
    }
}

class ZLibSubstitutions {

}
