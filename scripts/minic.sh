#!/usr/bin/env bash
# Runs the compiler:  scripts/minic.sh [options] file.mini      (see --help)
cd "$(dirname "$0")/.."
[ -d build/classes ] || ./scripts/build.sh >/dev/null
exec java -cp build/classes compiler.Main "$@"