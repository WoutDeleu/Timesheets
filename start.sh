#!/usr/bin/env bash
set -e

cd "$(dirname "$0")"

# Load correct Java version for this project via SDKMAN, without changing the global default
if [[ -f "$HOME/.sdkman/bin/sdkman-init.sh" && -f ".sdkmanrc" ]]; then
    source "$HOME/.sdkman/bin/sdkman-init.sh"
    sdk env
fi

APP_PORT=8080
APP_URL="http://localhost:${APP_PORT}"

echo "=== Timesheets ==="
echo ""

# Build if no JAR exists or if source changed
JAR_FILE=$(find target -name "timesheets-*.jar" -not -name "*-sources.jar" 2>/dev/null | head -1)
if [ -z "$JAR_FILE" ]; then
    echo "Building project..."
    ./mvnw -q package -DskipTests
    JAR_FILE=$(find target -name "timesheets-*.jar" -not -name "*-sources.jar" | head -1)
fi

echo "Starting application..."
java -jar "$JAR_FILE" &
APP_PID=$!

# Wait for app to be ready
echo "Waiting for application to start..."
for i in $(seq 1 30); do
    if curl -s -o /dev/null "$APP_URL" 2>/dev/null; then
        break
    fi
    sleep 1
done

# Open browser
if command -v open &>/dev/null; then
    open "$APP_URL"
elif command -v xdg-open &>/dev/null; then
    xdg-open "$APP_URL"
fi

echo ""
echo "Application running at $APP_URL"
echo "Press Ctrl+C to stop"

# Wait for process and handle Ctrl+C
trap "kill $APP_PID 2>/dev/null; exit 0" INT TERM
wait $APP_PID
