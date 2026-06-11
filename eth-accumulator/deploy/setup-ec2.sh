#!/bin/bash
# EC2 Amazon Linux 2023 setup script
# Run once after launching your instance: bash setup-ec2.sh

set -e

echo "=== Installing Docker ==="
sudo dnf update -y
sudo dnf install -y docker git
sudo systemctl enable --now docker
sudo usermod -aG docker ec2-user

echo "=== Installing Docker Compose ==="
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" \
    -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

echo "=== Cloning repository ==="
git clone https://github.com/rahulbandgar/digital.git /opt/eth-accumulator
cd /opt/eth-accumulator/eth-accumulator/docker

echo "=== Creating .env from example ==="
cp .env.example .env
echo ""
echo "ACTION REQUIRED: Edit /opt/eth-accumulator/eth-accumulator/docker/.env"
echo "  Set WALLET_ADDRESS, DB_PASSWORD, API_PASSWORD at minimum"
echo ""

echo "=== Installing systemd service ==="
sudo tee /etc/systemd/system/eth-accumulator.service > /dev/null <<'SERVICE'
[Unit]
Description=ETH Faucet Accumulator
Requires=docker.service
After=docker.service network-online.target

[Service]
Type=oneshot
RemainAfterExit=yes
WorkingDirectory=/opt/eth-accumulator/eth-accumulator/docker
ExecStart=/usr/local/bin/docker-compose up -d
ExecStop=/usr/local/bin/docker-compose down
TimeoutStartSec=120
Restart=on-failure

[Install]
WantedBy=multi-user.target
SERVICE

sudo systemctl daemon-reload
sudo systemctl enable eth-accumulator

echo "=== Done! ==="
echo "After editing .env, start with: sudo systemctl start eth-accumulator"
echo "View logs: docker logs -f eth-accumulator"
echo "Dashboard: http://<your-ec2-ip>:8080/api/claims/stats"
