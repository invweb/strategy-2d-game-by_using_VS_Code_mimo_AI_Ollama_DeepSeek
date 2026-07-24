#!/bin/bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17
export PATH="$JAVA_HOME/bin:$PATH"

cd "$(dirname "$0")"

echo "Starting app..." > /tmp/strategy_out.log
./gradlew desktop:run 2>&1 | tee -a /tmp/strategy_out.log
echo "EXIT: $?" >> /tmp/strategy_out.log
