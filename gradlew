#!/bin/sh

#
# Gradle wrapper script for UNIX
#

# Determine the Java command to use
if [ -n "$JAVA_HOME" ] ; then
    JAVACMD="$JAVA_HOME/bin/java"
else
    JAVACMD="java"
fi

# Check if Java is installed
if ! command -v $JAVACMD &> /dev/null; then
    echo "Error: JAVA_HOME is not set and java is not in PATH"
    exit 1
fi

# Determine Gradle wrapper JAR location
APP_HOME=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )
WRAPPER_JAR="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"

# Check if gradle-wrapper.jar exists
if [ ! -f "$WRAPPER_JAR" ]; then
    echo "Downloading Gradle wrapper..."
    mkdir -p gradle/wrapper

    # Download gradle-wrapper.jar from GitHub
    curl -fsSL \
        "https://raw.githubusercontent.com/gradle/gradle/master/gradle/wrapper/gradle-wrapper.jar" \
        -o "$WRAPPER_JAR" || {
        echo "Failed to download gradle-wrapper.jar"
        exit 1
    }
fi

# Execute Gradle
exec "$JAVACMD" \
    -classpath "$WRAPPER_JAR" \
    org.gradle.wrapper.GradleWrapperMain \
    "$@"
