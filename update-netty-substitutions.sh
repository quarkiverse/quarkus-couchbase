#!/bin/bash

echo "1 - Cloning Quarkus"

# Clone repo
git clone --depth=1 --filter=blob:none --sparse git@github.com:quarkusio/quarkus.git
cd quarkus
git sparse-checkout set extensions/netty
cd ..

# Set directories as variables
SRC_RUNTIME="quarkus/extensions/netty/runtime/src/main/java/io/quarkus/netty"
DEST_RUNTIME="runtime/src/main/java/com/couchbase/quarkus/extension"

SRC_DEPLOYMENT="quarkus/extensions/netty/deployment/src/main/java/io/quarkus/netty/deployment"
DEST_DEPLOYMENT="deployment/src/main/java/com/couchbase/quarkus/extension/deployment/nettyhandling"

# The runtime directory will exist but not necessarily the "nettyhandling" deployment one
mkdir -p "$DEST_DEPLOYMENT"

echo "2 - Copying files"
# Copy files from runtime and deployment directories and overwrite existing
cp -r "$SRC_RUNTIME"/* "$DEST_RUNTIME"
cp -r "$SRC_DEPLOYMENT"/* "$DEST_DEPLOYMENT"

echo "3 - Replacing shaded netty namespace"
# Prepend "com.couchbase.client.core.deps." to all "io.netty" occurrences in the copied files
find "$DEST_RUNTIME" "$DEST_DEPLOYMENT" -type f -name "*.java" -exec sed -i '' 's/io\.netty/com.couchbase.client.core.deps.io.netty/g' "{}" +

echo "4 - Fixing imports and packages"
# Fix the imports and packages
find "$DEST_RUNTIME" "$DEST_DEPLOYMENT" -type f -name "*.java" -exec sed -i '' \
    -e 's/io\.quarkus\.netty\.deployment/com.couchbase.quarkus.extension.deployment.nettyhandling/g' \
    -e 's/io\.quarkus\.netty/com.couchbase.quarkus.extension/g' \
    -e 's/io\.quarkus\.netty\.runtime/com.couchbase.quarkus.extension.runtime/g' \
    -e 's/io\.quarkus\.netty\.runtime\.virtual/com.couchbase.quarkus.extension.runtime.virtual/g' \
    -e 's/io\.quarkus\.netty\.runtime\.graal/com.couchbase.quarkus.extension.runtime.graal/g' \
    "{}" +

echo "5 - Replacing NettyProcessor"
# Replace copied NettyProcessor.java with our modified one
cp -f ModifiedNettyProcessor.txt "$DEST_DEPLOYMENT"/NettyProcessor.java


echo "6 - Deleting cloned repo"
# Delete the cloned repo
rm -rf "quarkus"

echo "7 - Done!"
