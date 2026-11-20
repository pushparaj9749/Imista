#!/usr/bin/env sh
set -e
APP_HOME=$(dirname "$0")
GRADLE_WRAPPER_JAR="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"
if [ -f "$GRADLE_WRAPPER_JAR" ] && command -v java >/dev/null 2>&1; then
  exec java -jar "$GRADLE_WRAPPER_JAR" "$@"
else
  echo "Wrapper jar not found or java missing, trying system gradle..."
  if command -v gradle >/dev/null 2>&1; then
    exec gradle "$@"
  else
    echo "ERROR: No gradle found. Install JDK and Gradle or provide gradle-wrapper.jar"
    exit 1
  fi
fi
