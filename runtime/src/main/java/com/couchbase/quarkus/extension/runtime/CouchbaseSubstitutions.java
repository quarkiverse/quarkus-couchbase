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
package com.couchbase.quarkus.extension.runtime;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

import com.couchbase.client.core.deps.io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import com.couchbase.client.core.deps.org.xbill.DNS.Name;
import com.couchbase.client.core.deps.org.xbill.DNS.ResolverConfig;
import com.couchbase.client.core.deps.org.xbill.DNS.SimpleResolver;
import com.couchbase.client.core.deps.org.xbill.DNS.config.FallbackPropertyResolverConfigProvider;
import com.couchbase.client.core.deps.org.xbill.DNS.config.InitializationException;
import com.couchbase.client.core.deps.org.xbill.DNS.config.JndiContextResolverConfigProvider;
import com.couchbase.client.core.deps.org.xbill.DNS.config.PropertyResolverConfigProvider;
import com.couchbase.client.core.deps.org.xbill.DNS.config.ResolvConfResolverConfigProvider;
import com.couchbase.client.core.deps.org.xbill.DNS.config.ResolverConfigProvider;
import com.couchbase.client.core.deps.org.xbill.DNS.config.SunJvmResolverConfigProvider;
import com.couchbase.client.core.encryption.CryptoManager;
import com.couchbase.client.java.codec.DefaultJsonSerializer;
import com.couchbase.client.java.codec.JsonSerializer;
import com.couchbase.client.java.env.ClusterEnvironment;
import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

public class CouchbaseSubstitutions {
}

@TargetClass(value = ClusterEnvironment.class, onlyWith = TargetClusterEnvironment.IsJacksonAbsent.class)
final class TargetClusterEnvironment {
    @Substitute
    private JsonSerializer newDefaultSerializer(CryptoManager cryptoManager) {
        return DefaultJsonSerializer.create(cryptoManager);
    }

    public static class IsJacksonAbsent implements BooleanSupplier {

        @Override
        public boolean getAsBoolean() {
            try {
                Class.forName("com.fasterxml.jackson.databind.ObjectMapper");
                return false;
            } catch (ClassNotFoundException ignored) {
                return true;
            }
        }
    }
}

@TargetClass(value = InsecureTrustManagerFactory.class)
final class Target_InsecureTrustManagerFactory {

}

@TargetClass(value = ResolverConfig.class)
final class Target_ResolverConfig {
    @Alias
    private List<InetSocketAddress> servers = new ArrayList<>(2);
    @Alias
    private List<Name> searchlist = new ArrayList<>(0);
    @Alias
    private int ndots = 1;
    @Alias
    private static ResolverConfig currentConfig;
    @Alias
    private static List<ResolverConfigProvider> configProviders;

    @Substitute
    public Target_ResolverConfig() {
        synchronized (ResolverConfig.class) {
            if (configProviders == null) {
                configProviders = new ArrayList<>(5);
                configProviders.add(new PropertyResolverConfigProvider());
                configProviders.add(new ResolvConfResolverConfigProvider());
                configProviders.add(new JndiContextResolverConfigProvider());
                configProviders.add(new SunJvmResolverConfigProvider());
                configProviders.add(new FallbackPropertyResolverConfigProvider());

            }
        }

        for (ResolverConfigProvider provider : configProviders) {
            if (provider.isEnabled()) {
                try {
                    provider.initialize();
                    if (servers.isEmpty()) {
                        servers.addAll(provider.servers());
                    }

                    if (searchlist.isEmpty()) {
                        List<Name> lsearchPaths = provider.searchPaths();
                        if (!lsearchPaths.isEmpty()) {
                            searchlist.addAll(lsearchPaths);
                            ndots = provider.ndots();
                        }
                    }

                    if (!servers.isEmpty() && !searchlist.isEmpty()) {
                        // found both servers and search path, we're done
                        return;
                    }
                } catch (InitializationException e) {
                    //Ignore
                }
            }
        }

        if (servers.isEmpty()) {
            servers.add(
                    new InetSocketAddress(InetAddress.getLoopbackAddress(), SimpleResolver.DEFAULT_PORT));
        }
    }
}