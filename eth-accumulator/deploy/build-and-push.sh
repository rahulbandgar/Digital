#!/bin/bash
# Build JAR, build Docker image, and push to EC2
# Usage: ./build-and-push.sh <ec2-public-ip> <path-to-pem-key>

set -e

EC2_IP=${1:?Usage: $0 <ec2-ip> <pem-key>}
PEM_KEY=${2:?Usage: $0 <ec2-ip> <pem-key>}
REMOTE_DIR=/opt/eth-accumulator/eth-accumulator

echo "=== Building JAR ==="
cd "$(dirname "$0")/.."
./mvnw clean package -DskipTests

echo "=== Copying JAR to EC2 ==="
scp -i "$PEM_KEY" target/eth-accumulator-1.0.0.jar "ec2-user@${EC2_IP}:${REMOTE_DIR}/target/"

echo "=== Rebuilding Docker image on EC2 ==="
ssh -i "$PEM_KEY" "ec2-user@${EC2_IP}" bash <<REMOTE
  cd ${REMOTE_DIR}
  docker build -t eth-accumulator:latest .
  cd docker
  docker-compose up -d --force-recreate app
REMOTE

echo "=== Deployed! ==="
echo "Logs: ssh -i $PEM_KEY ec2-user@${EC2_IP} 'docker logs -f eth-accumulator'"
