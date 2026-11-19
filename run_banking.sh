#!/usr/bin/env bash
set -e

mkdir -p lib src/model src/dao src/controller src/view out

if [ ! -f "lib/sqlite-jdbc.jar" ]; then
  echo "Downloading sqlite-jdbc.jar..."
  wget -q -O lib/sqlite-jdbc.jar https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/3.45.1.0/sqlite-jdbc-3.45.1.0.jar
fi

# Detect OpenJFX module path in Codespaces
JAVAFX_LIB_DIR=""
for candidate in /usr/share/openjfx/lib /usr/lib/jvm/*/lib; do
  if [ -f "$candidate/javafx.controls.jar" ] && [ -f "$candidate/javafx.fxml.jar" ]; then
    JAVAFX_LIB_DIR="$candidate"
    break
  fi
done

if [ -z "$JAVAFX_LIB_DIR" ]; then
  echo "Installing OpenJFX and Xvfb/noVNC tools..."
  sudo apt-get update && sudo apt-get install -y openjfx xvfb x11vnc novnc websockify wget
  JAVAFX_LIB_DIR="/usr/share/openjfx/lib"
fi

echo "Compiling JavaFX & DAO/Model/Controller sources..."
rm -rf out/*
javac --module-path "$JAVAFX_LIB_DIR" \
      --add-modules javafx.controls,javafx.fxml \
      -cp "lib/sqlite-jdbc.jar" \
      -d out \
      $(find src -name "*.java")

# Copy FXML and other non-Java resources into out/
find src -type f ! -name "*.java" | while read -r res; do
  rel="${res#src/}"
  mkdir -p "out/$(dirname "$rel")"
  cp "$res" "out/$rel"
done

# Start virtual display + noVNC if not already running
if ! pgrep -f "Xvfb :99" >/dev/null 2>&1; then
  Xvfb :99 -screen 0 1280x800x24 >/tmp/xvfb.log 2>&1 &
  sleep 1
fi

if ! pgrep -f "x11vnc.*:99" >/dev/null 2>&1; then
  x11vnc -display :99 -nopw -forever -shared >/tmp/x11vnc.log 2>&1 &
  sleep 1
fi

if ! pgrep -f "novnc_proxy.*6080" >/dev/null 2>&1; then
  if [ -x /usr/share/novnc/utils/novnc_proxy ]; then
    /usr/share/novnc/utils/novnc_proxy --vnc localhost:5900 --listen 6080 >/tmp/novnc.log 2>&1 &
  elif command -v websockify >/dev/null 2>&1; then
    websockify --web=/usr/share/novnc 6080 localhost:5900 >/tmp/novnc.log 2>&1 &
  fi
  sleep 1
fi

export DISPLAY=:99
echo "Launching BankingApp on DISPLAY=:99 (open forwarded port 6080 / vnc.html to view UI)..."
java --module-path "$JAVAFX_LIB_DIR" \
     --add-modules javafx.controls,javafx.fxml \
     -Dprism.order=sw \
     -Dglass.platform=Gtk \
     -cp "out:lib/sqlite-jdbc.jar" \
     BankingApp