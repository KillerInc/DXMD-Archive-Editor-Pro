#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
BUILD="$ROOT/build"
CLASSES="$BUILD/classes"
rm -rf "$BUILD"
mkdir -p "$CLASSES" "$ROOT/dist"
find "$ROOT/src/main/java" -name '*.java' -print0 | xargs -0 javac --release 21 -encoding UTF-8 -d "$CLASSES"
cp "$ROOT/src/main/resources/"*.b64 "$CLASSES/"
jar cfm "$ROOT/dist/DXMD-Archive-Editor-Pro-v0.7.6.jar" "$ROOT/src/main/resources/MANIFEST.MF" -C "$CLASSES" .
echo "Built: $ROOT/dist/DXMD-Archive-Editor-Pro-v0.7.6.jar"
