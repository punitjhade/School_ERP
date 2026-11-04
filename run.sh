#!/usr/bin/env bash
# Compiles and runs VSVPS Connect. Usage: ./run.sh [port]
set -e
cd "$(dirname "$0")"
PORT="${1:-8080}"

echo "Compiling..."
mkdir -p bin
find src -name "*.java" -print0 | xargs -0 javac -d bin

echo "Starting server on port $PORT ..."
java -cp bin vsvps.Main "$PORT"
