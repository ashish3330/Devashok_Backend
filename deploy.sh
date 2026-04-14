#!/bin/bash
set -e

echo "=========================================="
echo "  DevAshok Enclave — Production Deploy"
echo "=========================================="
echo ""

# Load env
if [ -f .env.prod ]; then
  export $(grep -v '^#' .env.prod | xargs)
  echo "[✓] Loaded .env.prod"
else
  echo "[✗] .env.prod not found. Create it first."
  exit 1
fi

# Check Docker
if ! command -v docker &> /dev/null; then
  echo "[✗] Docker not installed"
  exit 1
fi
if ! command -v docker compose &> /dev/null && ! command -v docker-compose &> /dev/null; then
  echo "[✗] Docker Compose not installed"
  exit 1
fi
echo "[✓] Docker ready"

# Build and start
echo ""
echo "Building images..."
docker compose -f docker-compose.prod.yml --env-file .env.prod build --no-cache

echo ""
echo "Starting services..."
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d

echo ""
echo "Waiting for backend health..."
for i in $(seq 1 30); do
  if docker exec devashok-backend wget -qO- http://localhost:8090/actuator/health 2>/dev/null | grep -q "UP"; then
    echo "[✓] Backend healthy"
    break
  fi
  if [ $i -eq 30 ]; then
    echo "[✗] Backend failed to start. Check logs: docker logs devashok-backend"
    exit 1
  fi
  sleep 2
done

echo ""
echo "=========================================="
echo "  Deployment Complete!"
echo "=========================================="
echo ""
echo "  UI:      http://$(hostname -I 2>/dev/null | awk '{print $1}' || echo 'YOUR_VPS_IP')"
echo "  API:     http://$(hostname -I 2>/dev/null | awk '{print $1}' || echo 'YOUR_VPS_IP')/api/"
echo "  Swagger: http://$(hostname -I 2>/dev/null | awk '{print $1}' || echo 'YOUR_VPS_IP')/swagger-ui.html"
echo ""
echo "  Login:   http://YOUR_VPS_IP/login/DEVASHOK"
echo "  Creds:   admin / admin123 (CHANGE IN PRODUCTION)"
echo ""
echo "  Logs:    docker compose -f docker-compose.prod.yml logs -f"
echo "  Stop:    docker compose -f docker-compose.prod.yml down"
echo "  DB:      docker exec -it devashok-db psql -U devashok -d realestate_emi_db"
echo ""
