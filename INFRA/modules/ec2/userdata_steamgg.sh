#!/bin/bash
# Amazon Linux 2023 기준 설치 명령어
dnf update -y
dnf install docker -y
systemctl start docker
systemctl enable docker
usermod -a -G docker ec2-user

# 2. AWS CLI 로그인을 위한 인증 정보 갱신
# (EC2가 본인의 권한으로 ECR에 로그인하도록 합니다)
aws ecr get-login-password --region ap-northeast-2 | docker login --username AWS --password-stdin ${ECR_URL}

# 3. 최신 이미지 받아오기
docker pull ${ECR_URL}:latest

# 4. 기존 컨테이너가 있다면 중지 및 삭제 (재배포 시 필요)
docker stop steamgg-app || true
docker rm steamgg-app || true

# 5. 새 컨테이너 실행 (RDS 정보 등은 환경 변수로 주입)
docker run -d \
  --name steamgg-app \
  -p 8080:8080 \
  -e DB_ENDPOINT=${DB_ENDPOINT} \
  -e DB_USERNAME=${DB_USERNAME} \
  -e DB_PASSWORD=${DB_PASSWORD} \
  -e DB_NAME=${DB_NAME} \
  ${ECR_URL}:latest