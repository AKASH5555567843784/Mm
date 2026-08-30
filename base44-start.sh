#!/bin/bash
set -e

RESULTS_DIR="/app/base44-results"
mkdir -p "$RESULTS_DIR"

echo "=== Generating Gradle wrapper ==="
cd /app
gradle wrapper --gradle-version 9.3.1 --distribution-type bin 2>&1 | tee "$RESULTS_DIR/wrapper.log" || true

echo "=== Building project and running unit tests ==="
cd /app
./gradlew testDebugUnitTest --no-daemon --stacktrace 2>&1 | tee "$RESULTS_DIR/build.log"
BUILD_EXIT=$?

# Generate status HTML
if [ $BUILD_EXIT -eq 0 ]; then
    STATUS="✅ BUILD & TESTS PASSED"
    STATUS_COLOR="#22c55e"
else
    STATUS="❌ BUILD OR TESTS FAILED"
    STATUS_COLOR="#ef4444"
fi

# Extract test results
TEST_SUMMARY=""
TEST_REPORT_DIR="/app/app/build/test-results/testDebugUnitTest"
if [ -d "$TEST_REPORT_DIR" ]; then
    TESTS=$(grep -oh 'tests="[0-9]*"' "$TEST_REPORT_DIR"/*.xml 2>/dev/null | sed 's/tests="//;s/"//' | paste -sd+ | bc 2>/dev/null || echo "?")
    FAILURES=$(grep -oh 'failures="[0-9]*"' "$TEST_REPORT_DIR"/*.xml 2>/dev/null | sed 's/failures="//;s/"//' | paste -sd+ | bc 2>/dev/null || echo "?")
    ERRORS=$(grep -oh 'errors="[0-9]*"' "$TEST_REPORT_DIR"/*.xml 2>/dev/null | sed 's/errors="//;s/"//' | paste -sd+ | bc 2>/dev/null || echo "?")
    SKIPPED=$(grep -oh 'skipped="[0-9]*"' "$TEST_REPORT_DIR"/*.xml 2>/dev/null | sed 's/skipped="//;s/"//' | paste -sd+ | bc 2>/dev/null || echo "?")
    TEST_SUMMARY="<tr><td>Tests Run</td><td><b>$TESTS</b></td></tr>
        <tr><td>Failures</td><td><b>$FAILURES</b></td></tr>
        <tr><td>Errors</td><td><b>$ERRORS</b></td></tr>
        <tr><td>Skipped</td><td><b>$SKIPPED</b></td></tr>"
fi

# Get last 80 lines of build log
BUILD_LOG_TAIL=$(tail -80 "$RESULTS_DIR/build.log" | sed 's/</\&lt;/g;s/>/\&gt;/g' | sed 's/$/<br>/')

cat > "$RESULTS_DIR/index.html" << HTMLEOF
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>MM Assistant — Build Status</title>
<style>
  * { margin: 0; padding: 0; box-sizing: border-box; }
  body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background: #0f172a; color: #e2e8f0; min-height: 100vh; padding: 2rem; }
  .container { max-width: 800px; margin: 0 auto; }
  h1 { font-size: 1.75rem; margin-bottom: 0.5rem; background: linear-gradient(135deg, #818cf8, #c084fc); -webkit-background-clip: text; -webkit-text-fill-color: transparent; }
  .subtitle { color: #94a3b8; margin-bottom: 2rem; font-size: 0.95rem; }
  .status-badge { display: inline-block; padding: 0.75rem 1.5rem; border-radius: 12px; font-size: 1.1rem; font-weight: 700; margin-bottom: 2rem; background: ${STATUS_COLOR}22; border: 1px solid ${STATUS_COLOR}; color: ${STATUS_COLOR}; }
  .card { background: #1e293b; border-radius: 16px; padding: 1.5rem; margin-bottom: 1.5rem; border: 1px solid #334155; }
  .card h2 { font-size: 1.1rem; margin-bottom: 1rem; color: #f1f5f9; }
  table { width: 100%; border-collapse: collapse; }
  td { padding: 0.5rem 0; border-bottom: 1px solid #334155; }
  td:first-child { color: #94a3b8; }
  td:last-child { text-align: right; }
  .log { background: #0f172a; border: 1px solid #334155; border-radius: 12px; padding: 1rem; font-family: 'SF Mono', Monaco, monospace; font-size: 0.8rem; line-height: 1.5; max-height: 400px; overflow-y: auto; white-space: pre-wrap; color: #94a3b8; }
  .info { color: #64748b; font-size: 0.85rem; margin-top: 1rem; }
</style>
</head>
<body>
<div class="container">
  <h1>MM Assistant</h1>
  <p class="subtitle">Zero-touch, real-time voice assistant with a sassy persona, wake-word listening, and deep device tool execution.</p>
  <div class="status-badge">${STATUS}</div>

  <div class="card">
    <h2>📋 Project Info</h2>
    <table>
      <tr><td>Type</td><td>Native Android App (Kotlin + Jetpack Compose)</td></tr>
      <tr><td>Package</td><td>com.aistudio.mmassistant.voxai</td></tr>
      <tr><td>Min SDK</td><td>24 (Android 7.0)</td></tr>
      <tr><td>Target SDK</td><td>36 (Android 16)</td></tr>
      <tr><td>AI Backend</td><td>Gemini Live API (gemini-2.0-flash-exp)</td></tr>
    </table>
  </div>

  <div class="card">
    <h2>🧪 Unit Test Results (Robolectric)</h2>
    <table>
      ${TEST_SUMMARY}
    </table>
  </div>

  <div class="card">
    <h2>📜 Build Log (last 80 lines)</h2>
    <div class="log">${BUILD_LOG_TAIL}</div>
  </div>

  <p class="info">ℹ️ This is a native Android app — it builds to an APK for Android devices/emulators and cannot run in a web browser. The build and unit tests above verify the code compiles and works correctly. To run the full app, build the APK and install it on an Android device or emulator.</p>
</div>
</body>
</html>
HTMLEOF

echo "=== Status page generated at $RESULTS_DIR/index.html ==="

# Serve the status page on port 3000
echo "=== Serving status page on port 3000 ==="
cd "$RESULTS_DIR"
exec python3 -m http.server 3000 --bind 0.0.0.0
