package com.couchbase.quarkus.extension.runtime;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;

import com.couchbase.client.core.deps.io.netty.util.NetUtil;
import com.couchbase.client.core.deps.io.netty.util.internal.PlatformDependent;
import com.couchbase.client.core.encryption.CryptoManager;
import com.couchbase.client.java.codec.JsonSerializer;
import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.InjectAccessors;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

import io.quarkus.arc.Arc;

@TargetClass(className = "com.couchbase.client.java.env.ClusterEnvironment")
final class Target_ClusterEnvironment_newDefaultSerializer {

    @Substitute
    private JsonSerializer newDefaultSerializer(CryptoManager cryptoManager) {
        return Arc.container().instance(JsonSerializer.class).get();
    }

    @Substitute
    private boolean nonShadowedJacksonPresent() {
        return false;
    }
}

@TargetClass(NetUtil.class)
final class NetUtilSubstitutions {
    @Alias
    @InjectAccessors(NetUtilLocalhost4Accessor.class)
    public static Inet4Address LOCALHOST4;

    @Alias
    @InjectAccessors(NetUtilLocalhost6Accessor.class)
    public static Inet6Address LOCALHOST6;

    private static class NetUtilLocalhost4Accessor {
        static Inet4Address get() {
            // using https://en.wikipedia.org/wiki/Initialization-on-demand_holder_idiom
            return NetUtilLocalhost4LazyHolder.LOCALHOST4;
        }

        static void set(Inet4Address ignored) {
            // a no-op setter to avoid exceptions when NetUtil is initialized at run-time
        }
    }

    private static class NetUtilLocalhost4LazyHolder {
        private static final Inet4Address LOCALHOST4;

        static {
            byte[] LOCALHOST4_BYTES = { 127, 0, 0, 1 };
            // Create IPv4 loopback address.
            try {
                LOCALHOST4 = (Inet4Address) InetAddress.getByAddress("localhost", LOCALHOST4_BYTES);
            } catch (Exception e) {
                // We should not get here as long as the length of the address is correct.
                PlatformDependent.throwException(e);
                throw new IllegalStateException("Should not reach here");
            }
        }
    }

    private static class NetUtilLocalhost6Accessor {
        static Inet6Address get() {
            // using https://en.wikipedia.org/wiki/Initialization-on-demand_holder_idiom
            return NetUtilLocalhost6LazyHolder.LOCALHOST6;
        }

        static void set(Inet6Address ignored) {
            // a no-op setter to avoid exceptions when NetUtil is initialized at run-time
        }
    }

    private static class NetUtilLocalhost6LazyHolder {
        private static final Inet6Address LOCALHOST6;

        static {
            byte[] LOCALHOST6_BYTES = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1 };
            // Create IPv6 loopback address.
            try {
                LOCALHOST6 = (Inet6Address) InetAddress.getByAddress("localhost", LOCALHOST6_BYTES);
            } catch (Exception e) {
                // We should not get here as long as the length of the address is correct.
                PlatformDependent.throwException(e);
                throw new IllegalStateException("Should not reach here");
            }
        }
    }
}
