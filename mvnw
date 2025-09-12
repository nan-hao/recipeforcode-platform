#!/bin/sh
# ----------------------------------------------------------------------------
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#    https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
# ----------------------------------------------------------------------------

set -euf

native_path() {
  # Convert Windows path to Unix when running Git Bash/Cygwin/MSYS
  if command -v cygpath >/dev/null 2>&1; then
    cygpath -au "$1"
  else
    printf %s "$1"
  fi
}

WRAPPER_DIR=$(cd "$(dirname "$0")"; pwd -P)
MVNW_REPOURL="https://repo.maven.apache.org/maven2"
WRAPPER_JAR="${WRAPPER_DIR}/.mvn/wrapper/maven-wrapper.jar"
WRAPPER_PROPERTIES="${WRAPPER_DIR}/.mvn/wrapper/maven-wrapper.properties"

# Download wrapper jar if missing
if [ ! -f "$WRAPPER_JAR" ]; then
  if [ -r "$WRAPPER_PROPERTIES" ]; then
    . "$WRAPPER_PROPERTIES" >/dev/null 2>&1 || true
  fi
  WRAPPER_URL=${wrapperUrl:-${MVNW_REPOURL}/org/apache/maven/wrapper/maven-wrapper/3.3.2/maven-wrapper-3.3.2.jar}
  mkdir -p "$(dirname "$WRAPPER_JAR")"
  if command -v curl >/dev/null 2>&1; then
    curl -fsSL -o "$WRAPPER_JAR" "$WRAPPER_URL"
  elif command -v wget >/dev/null 2>&1; then
    wget -q -O "$WRAPPER_JAR" "$WRAPPER_URL"
  else
    echo "ERROR: 'curl' or 'wget' is required to download maven-wrapper.jar" >&2
    exit 1
  fi
fi

JAVA_EXE="${JAVA_HOME:-}/bin/java"
if [ ! -x "$JAVA_EXE" ]; then
  JAVA_EXE=$(command -v java || true)
fi
if [ ! -x "$JAVA_EXE" ]; then
  echo "ERROR: Java executable not found; set JAVA_HOME or ensure 'java' is on PATH." >&2
  exit 1
fi

WRAPPER_LAUNCHER=org.apache.maven.wrapper.MavenWrapperMain
exec "$JAVA_EXE" \
  -Dmaven.multiModuleProjectDirectory="${WRAPPER_DIR}" \
  -cp "$(native_path "$WRAPPER_JAR")" \
  "$WRAPPER_LAUNCHER" "$@"

