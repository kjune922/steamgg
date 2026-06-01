#!/bin/bash
set -euo pipefail

dnf update -y
dnf install docker curl -y
systemctl enable --now docker
systemctl enable --now amazon-ssm-agent || true
usermod -a -G docker ec2-user

mkdir -p /opt/steamgg

cat >/opt/steamgg/deploy.sh <<'DEPLOY'
#!/bin/bash
set -euo pipefail

decode_value() {
  printf '%s' "$1" | base64 -d
}

ECR_URL="$(decode_value '${ECR_URL_B64}')"
DB_ENDPOINT="$(decode_value '${DB_ENDPOINT_B64}')"
DB_USERNAME="$(decode_value '${DB_USERNAME_B64}')"
DB_PASSWORD="$(decode_value '${DB_PASSWORD_B64}')"
CORS_ALLOWED_ORIGINS="$(decode_value '${CORS_ALLOWED_ORIGINS_B64}')"
ADMIN_SYNC_TOKEN="$(decode_value '${ADMIN_SYNC_TOKEN_B64}')"
STEAM_AUTO_SYNC_ENABLED="${STEAM_AUTO_SYNC_ENABLED}"
AWS_REGION="ap-northeast-2"
CONTAINER_NAME="steamgg-app"
ECR_REGISTRY="$${ECR_URL%%/*}"

aws ecr get-login-password --region "$${AWS_REGION}" \
  | docker login --username AWS --password-stdin "$${ECR_REGISTRY}"

docker pull "$${ECR_URL}:latest"
docker stop "$${CONTAINER_NAME}" || true
docker rm "$${CONTAINER_NAME}" || true

docker run -d \
  --restart unless-stopped \
  --name "$${CONTAINER_NAME}" \
  -p 8080:8080 \
  -e DB_ENDPOINT="$${DB_ENDPOINT}" \
  -e DB_USERNAME="$${DB_USERNAME}" \
  -e DB_PASSWORD="$${DB_PASSWORD}" \
  -e CORS_ALLOWED_ORIGINS="$${CORS_ALLOWED_ORIGINS}" \
  -e ADMIN_SYNC_TOKEN="$${ADMIN_SYNC_TOKEN}" \
  -e STEAM_AUTO_SYNC_ENABLED="$${STEAM_AUTO_SYNC_ENABLED}" \
  "$${ECR_URL}:latest"

for attempt in 1 2 3 4 5 6 7 8 9 10; do
  if curl --fail --silent --show-error http://127.0.0.1:8080/api/health; then
    exit 0
  fi
  echo "Container health check failed on attempt $${attempt}; retrying..."
  sleep 5
done

docker logs --tail 120 "$${CONTAINER_NAME}" || true
exit 1
DEPLOY

chmod +x /opt/steamgg/deploy.sh
