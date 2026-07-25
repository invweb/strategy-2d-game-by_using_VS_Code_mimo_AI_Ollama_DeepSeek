#!/bin/bash
export JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.20/libexec/openjdk.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"

cd "$(dirname "$0")"

echo "Starting app..." > /tmp/strategy_out.log
./gradlew --stop 2>&1 >> /tmp/strategy_out.log
./gradlew clean desktop:run 2>&1 | tee -a /tmp/strategy_out.log
echo "EXIT: $?" >> /tmp/strategy_out.log
