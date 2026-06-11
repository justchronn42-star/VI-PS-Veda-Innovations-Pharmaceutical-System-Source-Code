#!/bin/bash
# ─────────────────────────────────────────────────────────────────
#  VIPS Pharma — Linux / macOS launch script
#
#  Usage:  chmod +x run.sh && ./run.sh
#
#  Requirements:
#    • Java 17+ on PATH  (java -version to check)
#    • Run "mvn package" first to build target/vips-pharma.jar
#      and populate target/lib/
# ─────────────────────────────────────────────────────────────────

APP_JAR="target/vips-pharma.jar"
LIB_DIR="target/lib"

if [ ! -f "$APP_JAR" ]; then
    echo "[ERROR] $APP_JAR not found."
    echo "        Run:  mvn package"
    echo "        then re-run this script."
    exit 1
fi

java \
  --module-path "$LIB_DIR" \
  --add-modules javafx.controls,javafx.fxml,javafx.graphics,javafx.base \
  -cp "$APP_JAR:$LIB_DIR/*" \
  com.vips.pharma.MainApp
