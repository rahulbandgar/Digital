#!/usr/bin/env bash

PID_FILE=/tmp/erc20-deploy.pids

if [ ! -f "$PID_FILE" ]; then
  echo "No running instance found (no PID file at $PID_FILE)."
  exit 0
fi

read -r BESU_PID BACKEND_PID FRONTEND_PID < "$PID_FILE"

for pid in $BESU_PID $BACKEND_PID $FRONTEND_PID; do
  if kill -0 "$pid" 2>/dev/null; then
    echo "Stopping PID $pid..."
    kill "$pid"
  fi
done

rm -f "$PID_FILE"
echo "All processes stopped."
