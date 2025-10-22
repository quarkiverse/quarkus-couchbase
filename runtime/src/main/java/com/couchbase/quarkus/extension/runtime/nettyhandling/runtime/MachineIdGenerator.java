package com.couchbase.quarkus.extension.runtime.nettyhandling.runtime;

import java.util.Random;
import java.util.function.Supplier;

public final class MachineIdGenerator implements Supplier<String> {

    /**
     * We have our own implementation to generate the com.couchbase.client.core.deps.io.netty.machineId to prevent potential
     * slowness when generating/inferring the default machine id in
     * com.couchbase.client.core.deps.io.netty.channel.DefaultChannelId
     * implementation, which iterates over the NetworkInterfaces to determine the "best" machine id
     */
    public String get() {
        // borrowed from com.couchbase.client.core.deps.io.netty.util.internal.MacAddressUtil.EUI64_MAC_ADDRESS_LENGTH
        final int EUI64_MAC_ADDRESS_LENGTH = 8;
        final byte[] machineIdBytes = new byte[EUI64_MAC_ADDRESS_LENGTH];
        new Random().nextBytes(machineIdBytes);
        return com.couchbase.client.core.deps.io.netty.util.internal.MacAddressUtil.formatAddress(machineIdBytes);
    }
}
