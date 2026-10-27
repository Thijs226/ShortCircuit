#!/usr/bin/env bash
set -euo pipefail
JAVAC="${JAVA_HOME:-$PWD/tools/jdk}/bin/javac"
JAVA="${JAVA_HOME:-$PWD/tools/jdk}/bin/java"
mkdir -p build/tests
"$JAVAC" --release 8 -Xlint:-options -d build/tests src/com/thijs226/shortcircuit/{BlockPolicy,MazeModel,ShortsSignals,ScrollSignal,FeedTracker,BreakTimers}.java tests/com/thijs226/shortcircuit/CoreTests.java
"$JAVA" -cp build/tests com.thijs226.shortcircuit.CoreTests
