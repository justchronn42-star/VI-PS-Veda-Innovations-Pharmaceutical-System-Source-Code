@echo off
REM ─────────────────────────────────────────────────────────────────
REM  VIPS Pharma — Windows launch script
REM
REM  Usage:  run.bat
REM
REM  Requirements:
REM    • Java 17+ on PATH  (java -version to check)
REM    • Run "mvn package" first to build target/vips-pharma.jar
REM      and populate target/lib/
REM
REM  How it works:
REM    JavaFX 11+ splits its native libraries into platform JARs.
REM    Running "java -jar" alone doesn't load those natives.
REM    We point --module-path at target/lib where Maven copied all
REM    the JavaFX JARs, then --add-modules tells the JVM to load them.
REM    The app JAR itself goes on the regular -cp classpath.
REM ─────────────────────────────────────────────────────────────────

set APP_JAR=target\vips-pharma.jar
set LIB_DIR=target\lib

if not exist "%APP_JAR%" (
    echo [ERROR] %APP_JAR% not found.
    echo         Run:  mvn package
    echo         then re-run this script.
    pause
    exit /b 1
)

java ^
  --module-path "%LIB_DIR%" ^
  --add-modules javafx.controls,javafx.fxml,javafx.graphics,javafx.base ^
  -cp "%APP_JAR%;%LIB_DIR%\*" ^
  com.vips.pharma.MainApp

pause
