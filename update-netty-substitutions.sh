#!/bin/bash

# Set directories as variables
SRC_RUNTIME="quarkus/extensions/netty/runtime/src/main/java/io/quarkus/netty"
DEST_RUNTIME="runtime/src/main/java/com/couchbase/quarkus/extension/runtime/nettyhandling"

SRC_DEPLOYMENT="quarkus/extensions/netty/deployment/src/main/java/io/quarkus/netty/deployment"
DEST_DEPLOYMENT="deployment/src/main/java/com/couchbase/quarkus/extension/deployment/nettyhandling"

# Get Quarkus version from parent pom.xml and use it to checkout the correct Quarkus branch for netty substitutions
POM_FILE=$(find . -maxdepth 1 -name "pom.xml" -print -quit)
QUARKUS_VERSION=$(sed -n 's/.*<quarkus.version>\(.*\)<\/quarkus.version>.*/\1/p' "$POM_FILE")

if [[ -z "$QUARKUS_VERSION" ]]; then
    echo "Could not find <quarkus.version> in pom.xml."
    exit 1
fi
echo "Found Quarkus version: $QUARKUS_VERSION"

# Clone repo
echo "1 - Cloning Quarkus"
git clone --depth=1 --filter=blob:none --sparse --branch "$QUARKUS_VERSION" git@github.com:quarkusio/quarkus.git
cd quarkus
git sparse-checkout set extensions/netty bom/application
cd ..

BOM_FILE="quarkus/bom/application/pom.xml"
NETTY_VERSION=$(sed -n 's/.*<netty.version>\(.*\)<\/netty.version>.*/\1/p' "$BOM_FILE")

if [[ -z "$NETTY_VERSION" ]]; then
    echo "Could not find <netty.version> in the BOM file."
    exit 1
fi
echo "The Netty target version is: $NETTY_VERSION"

# Creating "nettyhandling" directories to keep pulled files separate from our extension's
echo "2 - Creating nettyhandling directories"
mkdir -p "$DEST_DEPLOYMENT"
mkdir -p "$DEST_RUNTIME"

# Copy files from runtime and deployment directories and overwrite existing
echo "3 - Copying files"
cp -r "$SRC_RUNTIME"/* "$DEST_RUNTIME"
cp -r "$SRC_DEPLOYMENT"/* "$DEST_DEPLOYMENT"

# Prepend "com.couchbase.client.core.deps." to all "io.netty" occurrences in the copied files
echo "4 - Replacing shaded netty namespace"
find "$DEST_RUNTIME" "$DEST_DEPLOYMENT" -type f -name "*.java" -exec sed -i '' 's/io\.netty/com.couchbase.client.core.deps.io.netty/g' "{}" +

# Fix the imports and packages
echo "5 - Fixing imports and packages"
find "$DEST_RUNTIME" "$DEST_DEPLOYMENT" -type f -name "*.java" -exec sed -i '' \
    -e 's/io\.quarkus\.netty\.deployment/com.couchbase.quarkus.extension.deployment.nettyhandling/g' \
    -e 's/io\.quarkus\.netty/com.couchbase.quarkus.extension.runtime.nettyhandling/g' \
    -e 's/io\.quarkus\.netty\.runtime/com.couchbase.quarkus.extension.runtime.nettyhandling.runtime/g' \
    -e 's/io\.quarkus\.netty\.runtime\.virtual/com.couchbase.quarkus.extension.runtime.nettyhandling.runtime.virtual/g' \
    -e 's/io\.quarkus\.netty\.runtime\.graal/com.couchbase.quarkus.extension.runtime.nettyhandling.runtime.graal/g' \
    "{}" +

# Replace a line in the netty config which conflicts with existing netty extension
echo "6 - Renaming Netty config item"
sed -i '' 's/@ConfigRoot(name = "netty", phase = ConfigPhase.BUILD_TIME)/@ConfigRoot(name = "couchbase.netty", phase = ConfigPhase.BUILD_TIME)/' "$DEST_DEPLOYMENT/NettyBuildTimeConfig.java"

#Delete two methods in NettyProcessor which we don't want to use
echo "7 - Deleting code we don't want in NettyProcessor"
./deleteMethod.sh "public RuntimeReinitializedClassBuildItem reinitScheduledFutureTask" "$DEST_DEPLOYMENT/NettyProcessor.java"
./deleteMethod.sh "LogCleanupFilterBuildItem cleanupMacDNSInLog" "$DEST_DEPLOYMENT/NettyProcessor.java"
./deleteMethod.sh "static SslContext newClientContextInternal" "$DEST_RUNTIME/runtime/graal/NettySubstitutions.java"
./deleteMethod.sh "final class Target_io_netty_handler_ssl_JdkSslClientContext" "$DEST_RUNTIME/runtime/graal/NettySubstitutions.java"


#This isn't absolutely necessary, as Quarkus will optimise imports and remove unused/missing ones during compilation.
echo "8 - Deleting missing import"
sed -i '' '/import com.couchbase.client.core.deps.io.netty.resolver.dns.DnsServerAddressStreamProviders;/d' "$DEST_DEPLOYMENT/NettyProcessor.java"
sed -i '' '/import io.quarkus.deployment.builditem.nativeimage.RuntimeReinitializedClassBuildItem;/d' "$DEST_DEPLOYMENT/NettyProcessor.java"

# Delete the cloned repo
echo "9 - Deleting cloned repo"
rm -rf "quarkus"

echo "10 - Done!"
