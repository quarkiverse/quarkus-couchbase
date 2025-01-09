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

counter=1

# Clone repo
echo "$counter - Cloning Quarkus"
((counter++))
git clone --depth=1 --filter=blob:none --sparse --branch "$QUARKUS_VERSION" git@github.com:quarkusio/quarkus.git
cd quarkus
git sparse-checkout set extensions/netty bom/application
cd ..

BOM_FILE="quarkus/bom/application/pom.xml"
NETTY_VERSION=$(sed -n 's/.*<netty.version>\(.*\)<\/netty.version>.*/\1/p' "$BOM_FILE")

if [[ -z "$NETTY_VERSION" ]]; then
    echo "Could not find <netty.version> in the BOM file."
else
    echo "The Netty target version is: $NETTY_VERSION"
fi

# Creating "nettyhandling" directories to keep pulled files separate from our extension's
echo "$counter - Creating nettyhandling directories"
((counter++))
mkdir -p "$DEST_DEPLOYMENT"
mkdir -p "$DEST_RUNTIME"

# Copy files from runtime and deployment directories and overwrite existing
echo "$counter - Copying files"
((counter++))
cp -r "$SRC_RUNTIME"/* "$DEST_RUNTIME"
cp -r "$SRC_DEPLOYMENT"/* "$DEST_DEPLOYMENT"

# Prepend "com.couchbase.client.core.deps." to all "io.netty" occurrences in the copied files
echo "$counter - Replacing shaded netty namespace"
((counter++))
find "$DEST_RUNTIME" "$DEST_DEPLOYMENT" -type f -name "*.java" -exec sed -i '' 's/io\.netty/com.couchbase.client.core.deps.io.netty/g' "{}" +

# Fix the imports and packages
echo "$counter - Fixing imports and packages"
((counter++))
find "$DEST_RUNTIME" "$DEST_DEPLOYMENT" -type f -name "*.java" -exec sed -i '' \
    -e 's/io\.quarkus\.netty\.deployment/com.couchbase.quarkus.extension.deployment.nettyhandling/g' \
    -e 's/io\.quarkus\.netty/com.couchbase.quarkus.extension.runtime.nettyhandling/g' \
    -e 's/io\.quarkus\.netty\.runtime/com.couchbase.quarkus.extension.runtime.nettyhandling.runtime/g' \
    -e 's/io\.quarkus\.netty\.runtime\.virtual/com.couchbase.quarkus.extension.runtime.nettyhandling.runtime.virtual/g' \
    -e 's/io\.quarkus\.netty\.runtime\.graal/com.couchbase.quarkus.extension.runtime.nettyhandling.runtime.graal/g' \
    "{}" +

# Replace a line in the netty config which conflicts with existing netty extension
echo "$counter - Renaming Netty config item"
((counter++))
sed -i '' 's/@ConfigRoot(name = "netty", phase = ConfigPhase.BUILD_TIME)/@ConfigRoot(name = "couchbase.netty", phase = ConfigPhase.BUILD_TIME)/' "$DEST_DEPLOYMENT/NettyBuildTimeConfig.java"

#Delete two methods in NettyProcessor which we don't want to use
echo "$counter - Deleting code we don't want in NettyProcessor"
((counter++))
#./deleteMethod.sh "public RuntimeReinitializedClassBuildItem reinitScheduledFutureTask" "$DEST_DEPLOYMENT/NettyProcessor.java"
./deleteMethod.sh "LogCleanupFilterBuildItem cleanupMacDNSInLog" "$DEST_DEPLOYMENT/NettyProcessor.java"
#./deleteMethod.sh "static SslContext newClientContextInternal" "$DEST_RUNTIME/runtime/graal/NettySubstitutions.java"
#./deleteMethod.sh "final class Target_io_netty_handler_ssl_JdkSslClientContext" "$DEST_RUNTIME/runtime/graal/NettySubstitutions.java"


#This isn't absolutely necessary, as Quarkus will optimise imports and remove unused/missing ones during compilation.
echo "$counter - Deleting missing import"
((counter++))
sed -i '' '/import com.couchbase.client.core.deps.io.netty.resolver.dns.DnsServerAddressStreamProviders;/d' "$DEST_DEPLOYMENT/NettyProcessor.java"
#sed -i '' '/import io.quarkus.deployment.builditem.nativeimage.RuntimeReinitializedClassBuildItem;/d' "$DEST_DEPLOYMENT/NettyProcessor.java"

# Delete the cloned repo
echo "$counter - Deleting cloned repo"
((counter++))
rm -rf "quarkus"

echo "$counter - Done!"
