#!/usr/bin/env bash
# Builds, then runs the tests.
#   scripts/test.sh            run everything
#   scripts/test.sh scanner    run one phase: scanner | parser | semantic | ir | codegen | optimizer | examples
set -euo pipefail
cd "$(dirname "$0")/.."
./scripts/build.sh >/dev/null
# Java separates classpath entries with ';' on Windows (Git Bash) and ':' elsewhere
case "$(uname -s)" in
  MINGW*|MSYS*|CYGWIN*) SEP=";" ;;
  *) SEP=":" ;;
esac
CP="build/classes${SEP}build/test-classes"
case "${1:-all}" in
  all)       java -cp "$CP" compiler.tests.AllTests ;;
  scanner)   java -cp "$CP" compiler.tests.ScannerTests ;;
  parser)    java -cp "$CP" compiler.tests.ParserTests ;;
  semantic)  java -cp "$CP" compiler.tests.SemanticTests ;;
  ir)        java -cp "$CP" compiler.tests.IrTests ;;
  codegen)   java -cp "$CP" compiler.tests.CodegenVmTests ;;
  optimizer) java -cp "$CP" compiler.tests.OptimizerTests ;;
  examples)  java -cp "$CP" compiler.tests.ExampleTests ;;
  *) echo "unknown suite '$1'"; exit 64 ;;
esac