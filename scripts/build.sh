#!/usr/bin/env bash
# Builds the compiler with plain javac (only a JDK 17+ is needed - no Maven, no downloads).
set -euo pipefail
cd "$(dirname "$0")/.."
rm -rf build && mkdir -p build/classes build/test-classes
javac --release 17 -Xlint:all -d build/classes $(find src/main -name '*.java')
javac --release 17 -cp build/classes -d build/test-classes $(find src/test -name '*.java')
echo "Build OK  ->  build/classes"