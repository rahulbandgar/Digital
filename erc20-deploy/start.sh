#!/usr/bin/env bash
set -e

BESU_DATA_DIR=/tmp/besu-dev-data
BESU_LOG=/tmp/besu-dev.log
BACKEND_LOG=/tmp/erc20-backend.log
FRONTEND_LOG=/tmp/erc20-frontend.log
PID_FILE=/tmp/erc20-deploy.pids

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# ── helpers ────────────────────────────────────────────────────────────────────
wait_for_http() {
  local url="$1" name="$2"
  echo -n "Waiting for $name ."
  for i in $(seq 1 60); do
    if curl -sf "$url" > /dev/null 2>&1; then
      echo " ready"
      return 0
    fi
    echo -n "."
    sleep 2
  done
  echo " TIMEOUT"
  return 1
}

# ── Besu ───────────────────────────────────────────────────────────────────────
if ! command -v besu &> /dev/null; then
  echo "ERROR: 'besu' not found. Install Besu 24.x and ensure it is on PATH."
  echo "  Download: https://github.com/hyperledger/besu/releases"
  exit 1
fi

echo "==> Starting Besu dev node..."
mkdir -p "$BESU_DATA_DIR"
besu \
  --network=dev \
  --miner-enabled \
  --miner-coinbase=0xfe3b557e8fb62b89f4916b721be55ceb828dbd73 \
  --rpc-http-enabled \
  --rpc-http-host=0.0.0.0 \
  --rpc-http-port=8545 \
  --rpc-http-cors-origins="*" \
  --rpc-http-api=ETH,NET,WEB3,MINER \
  --host-allowlist="*" \
  --min-gas-price=0 \
  --data-path="$BESU_DATA_DIR" \
  --logging=WARN \
  > "$BESU_LOG" 2>&1 &
BESU_PID=$!

wait_for_http "http://localhost:8545" "Besu RPC"

# ── Spring Boot backend ────────────────────────────────────────────────────────
echo "==> Building & starting Spring Boot backend..."
cd "$SCRIPT_DIR/backend"
mvn -q package -DskipTests
java -jar target/*.jar > "$BACKEND_LOG" 2>&1 &
BACKEND_PID=$!

wait_for_http "http://localhost:8080/actuator/health" "Backend" 2>/dev/null || \
  wait_for_http "http://localhost:8080" "Backend"

# ── React frontend ─────────────────────────────────────────────────────────────
echo "==> Starting React frontend..."
cd "$SCRIPT_DIR/frontend"
npm install --silent
npm run dev > "$FRONTEND_LOG" 2>&1 &
FRONTEND_PID=$!

sleep 3

# ── save PIDs ──────────────────────────────────────────────────────────────────
echo "$BESU_PID $BACKEND_PID $FRONTEND_PID" > "$PID_FILE"

echo ""
echo "╔══════════════════════════════════════════════════════╗"
echo "║  ERC20 Deploy App is running                         ║"
echo "╠══════════════════════════════════════════════════════╣"
echo "║  Frontend  →  http://localhost:3000                  ║"
echo "║  Backend   →  http://localhost:8080                  ║"
echo "║  Besu RPC  →  http://localhost:8545                  ║"
echo "╠══════════════════════════════════════════════════════╣"
echo "║  Deployer  0xfe3b557e8fb62b89f4916b721be55ceb828dbd73 ║"
echo "╚══════════════════════════════════════════════════════╝"
echo ""
echo "Run ./stop.sh to shut everything down."
