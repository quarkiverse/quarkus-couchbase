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

import java.util.function.Supplier;

import org.jboss.logging.Logger;

import com.couchbase.client.core.deps.io.netty.channel.DefaultChannelId;
import com.couchbase.client.core.deps.io.netty.channel.EventLoopGroup;
import com.couchbase.client.core.deps.io.netty.channel.nio.NioEventLoopGroup;

import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class NettyRecorder {

    private static final Logger log = Logger.getLogger(NettyRecorder.class);

    // TODO: Remove this method (maybe in 1.6.x of Quarkus or later) if there are no user reports
    // of the WARN message issued from this method. See comments in https://github.com/quarkusio/quarkus/pull/9246
    // for details
    public void eagerlyInitChannelId() {
        //this class is slow to init and can block the IO thread and cause a warning
        //we init it from a throwaway thread to stop this
        //we do it from another thread so as not to affect start time
        new Thread(new Runnable() {
            @Override
            public void run() {
                long start = System.currentTimeMillis();
                DefaultChannelId.newInstance();
                if (System.currentTimeMillis() - start > 1000) {
                    log.warn("Netty DefaultChannelId initialization (with io.netty.machineId" +
                            " system property set to " + System.getProperty("io.netty.machineId")
                            + ") took more than a second");
                }
            }
        }).start();
    }

    public Supplier<EventLoopGroup> createEventLoop(int nThreads) {
        return new Supplier<EventLoopGroup>() {

            volatile EventLoopGroup val;

            @Override
            public EventLoopGroup get() {
                if (val == null) {
                    synchronized (this) {
                        if (val == null) {
                            val = new NioEventLoopGroup(nThreads);
                        }
                    }
                }
                return val;
            }
        };
    }
}
