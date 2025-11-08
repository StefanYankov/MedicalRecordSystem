#!/usr/bin/env bash
# A dependency-free wait-for-it script.

set -e

TARGET=$1
shift
CMD=("$@")

HOST=$(echo $TARGET | cut -d: -f1)
PORT=$(echo $TARGET | cut -d: -f2)

echo "Waiting for $HOST:$PORT to be available..."

# Use bash's built-in /dev/tcp for checking the port.
# This avoids the need for netcat (nc).
while ! (echo > /dev/tcp/$HOST/$PORT) >/dev/null 2>&1; do
  sleep 1
done

echo "$HOST:$PORT is available. Executing command: ${CMD[*]}"

# Use exec to replace the script process with the application process.
exec "${CMD[@]}"
