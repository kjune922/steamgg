# Local DB Setup

로컬 PostgreSQL은 `infra/docker-compose.yml`과 `infra/init.sql` 기준으로 생성한다.

주의

- `infra/docker-compose.yml`
- `infra/init.sql`

위 두 파일은 로컬 전용이며 Git 추적 대상이 아니다.

## 1. 사용자명, 비밀번호, DB명 정하기

PowerShell 기준으로 먼저 환경변수를 지정한다.

```powershell
$env:POSTGRES_DB="steamgg"
$env:POSTGRES_USER="여기에_원하는_유저명"
$env:POSTGRES_PASSWORD="여기에_원하는_비밀번호"
```

## 2. PostgreSQL 컨테이너 실행

`infra` 디렉터리에서 실행한다.

```powershell
docker compose up -d
```

최초 실행 시 `infra/init.sql`이 자동 적용되어 아래 테이블이 생성된다.

- `games`
- `genres`
- `tags`
- `game_genres`
- `game_tags`

## 3. 생성 확인

```powershell
docker compose exec postgres psql -U $env:POSTGRES_USER -d $env:POSTGRES_DB
```

접속 후 확인

```sql
\dt
```

## 4. 백엔드 연결

백엔드 실행 전 아래 환경변수를 같은 값으로 맞춘다.

```powershell
$env:DB_URL="jdbc:postgresql://localhost:5432/$env:POSTGRES_DB"
$env:DB_USERNAME=$env:POSTGRES_USER
$env:DB_PASSWORD=$env:POSTGRES_PASSWORD
```

그 다음 `backend`에서 실행한다.

```powershell
.\gradlew.bat bootRun
```

## 5. 주의사항

- 이미 생성된 `postgres_data` 볼륨이 있으면 `init.sql`은 다시 자동 실행되지 않는다.
- 스키마를 처음부터 다시 만들려면 컨테이너와 볼륨을 같이 내리고 다시 올린다.

```powershell
docker compose down -v
docker compose up -d
```
