#!/bin/bash
# 에러확인용 로그 설정
exec > >(tee /var/log/user-data.log|logger -t user-data -s 2>/dev/console) 2>&1

echo "PostgreSQL 환경으로 배포 시작중..."

# 1. 패키지 업데이트 및 필요 도구 설치
sudo dnf update -y
sudo dnf install -y python3 python3-pip nginx

# 2. 앱 디렉토리 설정
APP_DIR="/home/ec2-user/app"
mkdir -p $APP_DIR
cd $APP_DIR

# 3. Nginx 설정 (기존과 동일)
cat <<EOF | sudo tee /etc/nginx/conf.d/test_app.conf
server {
    listen 80;
    server_name _;
    location / {
        proxy_pass http://127.0.0.1:8000;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
    }
}
EOF

sudo systemctl restart nginx
sudo systemctl enable nginx

# 4. FastAPI 앱 코드 작성 (PostgreSQL 버전)
cat <<EOF > steamgg_main.py
from fastapi import FastAPI, Request
import uvicorn
import psycopg2
from psycopg2.extras import RealDictCursor
from datetime import datetime
import time

app = FastAPI()

# PostgreSQL 연결 설정
db_config = {
    'host': '${db_endpoint}',
    'user': '${db_username}',
    'password': '${db_password}',
    'dbname': 'steamgg_PostgreSQL', # 초기 연결은 기본 db인 postgres로 시도
    'port': 5432
}

def init_db():
    # RDS가 뜰 때까지 잠시 대기 (Retry 로직)
    for i in range(5):
        try:
            conn = psycopg2.connect(**db_config)
            cur = conn.cursor()
            cur.execute("""
                CREATE TABLE IF NOT EXISTS visitors (
                    id SERIAL PRIMARY KEY,
                    visit_time TIMESTAMP,
                    ip_address VARCHAR(50)
                )
            """)
            conn.commit()
            cur.close()
            conn.close()
            print("DB 테이블 초기화 성공")
            break
        except Exception as e:
            print(f"접속 시도 중... ({i+1}/5): {e}")
            time.sleep(10)

init_db()

@app.get("/")
def root_page(request: Request):
    client_ip = request.client.host
    current_time = datetime.now()

    try:
        # psycopg2 연결
        conn = psycopg2.connect(**db_config)
        cur = conn.cursor(cursor_factory=RealDictCursor)

        # 1. DB에 방문기록 저장
        cur.execute(
            "INSERT INTO visitors (visit_time, ip_address) VALUES (%s, %s)",
            (current_time, client_ip)
        )
        conn.commit()

        # 2. 총 방문자 수 조회
        cur.execute("SELECT COUNT(*) as count FROM visitors")
        result = cur.fetchone()
        total_visits = result['count']

        cur.close()
        conn.close()

        return {
            "message": "환영합니다! PostgreSQL RDS 연동에 성공했습니다 🐘",
            "your_ip": client_ip,
            "total_visits": total_visits
        }
    except Exception as e:
        return {"error": "DB 연동 실패", "detail": str(e)}

if __name__ == "__main__":
    uvicorn.run(app, host="127.0.0.1", port=8000)
EOF

# 5. 가상환경 설정 및 라이브러리 설치
python3 -m venv venv
./venv/bin/pip install --upgrade pip
# 중요: mysql 대신 postgresql용 라이브러리 설치
./venv/bin/pip install fastapi uvicorn psycopg2-binary

# 6. 권한 설정 및 앱 실행
chown -R ec2-user:ec2-user $APP_DIR
sudo ./venv/bin/python3 steamgg_main.py > $APP_DIR/app.log 2>&1 &