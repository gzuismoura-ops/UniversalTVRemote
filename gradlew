#!/usr/bin/env sh
set -eu
APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
GRADLE_VERSION=8.9
GRADLE_HOME="$HOME/.gradle/universal-wrapper/gradle-$GRADLE_VERSION"
if [ -x "$GRADLE_HOME/bin/gradle" ]; then
  exec "$GRADLE_HOME/bin/gradle" -p "$APP_HOME" "$@"
fi
if command -v gradle >/dev/null 2>&1; then
  exec gradle -p "$APP_HOME" "$@"
fi
mkdir -p "$HOME/.gradle/universal-wrapper"
ARCHIVE="$HOME/.gradle/universal-wrapper/gradle-$GRADLE_VERSION-bin.zip"
if [ ! -f "$ARCHIVE" ]; then
  if command -v curl >/dev/null 2>&1; then
    curl -fL "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip" -o "$ARCHIVE"
  elif command -v wget >/dev/null 2>&1; then
    wget "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip" -O "$ARCHIVE"
  else
    echo "Install Gradle $GRADLE_VERSION or curl/wget to bootstrap it." >&2
    exit 1
  fi
fi
unzip -q -o "$ARCHIVE" -d "$HOME/.gradle/universal-wrapper"
exec "$GRADLE_HOME/bin/gradle" -p "$APP_HOME" "$@"
