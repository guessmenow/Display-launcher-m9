#!/usr/bin/env bash
set -euo pipefail
WRAPPER_JAR="$(cd "$(dirname "$0")" && pwd)/gradle/wrapper/gradle-wrapper.jar"
if [[ ! -f "$WRAPPER_JAR" ]]; then
  echo "gradle-wrapper.jar not found."
  echo "Open the project in Android Studio and run the Gradle task 'wrapper',"
  echo "or install Gradle and run: gradle wrapper"
  exit 1
fi
JAVA_CMD=${JAVA_CMD:-java}
"$JAVA_CMD" -classpath "$WRAPPER_JAR" org.gradle.wrapper.GradleWrapperMain "$@"
