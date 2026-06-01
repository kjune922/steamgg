# SteamGG MVP Handoff Summary

이 문서는 현재 브랜치의 MVP 작업 내용을 팀원에게 넘기기 위한 요약이다.
실제 AWS/Vercel 배포 확인은 아직 수행하지 않았고, 배포 순서는 `docs/mvp-deploy-runbook.md`를 따른다.

## Current State

코드와 문서 기준으로 MVP 배포 준비는 끝난 상태다.
남은 작업은 실제 인프라 생성, GitHub Actions 배포, Vercel 배포, 실 URL 확인이다.

```text
Not yet done:
terraform apply
GitHub Actions Deploy to Amazon EC2 run
ALB runtime check
Vercel deployment check
Cloudflare domain purchase/DNS connection
```

## Biggest Previous Gaps

기존 상태에서 가장 큰 미완성 지점은 다음이었다.

```text
Frontend expected richer game APIs than backend provided.
ALB/EC2 app port and health check were not aligned for Spring Boot on 8080.
Terraform apply and GitHub Actions deploy responsibilities were unclear.
Vercel/Cloudflare deployment path was not documented.
Sensitive local files and examples were not fully separated.
```

## Backend Changes

Spring Boot backend now provides the MVP API contract used by the frontend.

Implemented or reinforced:

```text
GET /api/health
GET /api/games?q=&genre=&tag=&sort=&page=&size=
GET /api/games/{id}
GET /api/games/facets
GET /api/games/curations
GET /api/games/{id}/similar
GET /api/admin/sync
```

Important backend behavior:

```text
/api/games returns paginated JSON with items, totalCount, totalPages, page, size.
/api/admin/sync requires X-Admin-Token or token query param.
Scheduled Steam sync is disabled by default.
CORS allowed origins are controlled by CORS_ALLOWED_ORIGINS.
Backend listens on 8080.
Database name is steamgg_postgresql.
```

Tests added:

```text
demo/src/test/java/com/example/demo/HelloControllerTest.java
demo/src/test/java/com/example/demo/HelloControllerWebTest.java
```

## Frontend Changes

Frontend API integration is now aligned with the backend contract.

Changed behavior:

```text
Home uses /api/games/curations.
List uses /api/games and /api/games/facets.
Detail uses /api/games/{id} and /api/games/{id}/similar.
API_BASE_URL is normalized by removing trailing slashes.
Production build fails loudly if API_BASE_URL is missing.
API responses are normalized defensively before rendering.
Home route is force-dynamic so it does not freeze empty build-time data.
Open Graph image uses only absolute http image URLs.
```

Frontend deployment requirement:

```text
Root Directory: frontend
API_BASE_URL=http://<alb-dns-name>
```

After domain setup:

```text
API_BASE_URL=https://api.<domain>
```

## Terraform and AWS Changes

Infrastructure is aligned around Spring Boot container port 8080.

Important alignment:

```text
ALB listener: 80
Target Group: 8080
Target Group health check: /api/health
EC2 Security Group: allows ALB SG -> 8080
Backend container: 8080
```

Cost-sensitive choices:

```text
No NAT Gateway for MVP.
Default workspace EC2 instance type is t3.micro.
RDS is db.t3.micro.
EC2 instances run in public subnets for outbound image pulls and AWS APIs.
App inbound access is still restricted to ALB SG -> 8080.
```

Terraform variables:

```text
steamgg_db_username
steamgg_db_password
steamgg_cors_allowed_origins
steamgg_admin_sync_token
steamgg_steam_auto_sync_enabled
```

Sensitive values are not committed. Use `INFRA/terraform.tfvars` locally.

## GitHub Actions Deployment

Workflow:

```text
.github/workflows/deploy.yml
```

Jobs:

```text
backend-quality
frontend-quality
deploy
```

Deploy job behavior:

```text
Build Docker image from demo/
Push image to ECR with github sha and latest tags
Find running EC2 instances by Name=steamgg-asg-instance-<workspace>
Use SSM send-command to run /opt/steamgg/deploy.sh
Verify ALB /api/health
```

Deployment runs:

```text
Manual workflow_dispatch
Push to main
```

Feature branches run quality jobs but do not deploy automatically.

## Required GitHub Settings

Secrets:

```text
AWS_ACCESS_KEY_ID
AWS_SECRET_ACCESS_KEY
AWS_REGION
ECR_REPOSITORY
```

Default values:

```text
AWS_REGION=ap-northeast-2
ECR_REPOSITORY=steamgg-app-repo-default
```

Optional variable:

```text
TERRAFORM_WORKSPACE=default
```

## Vercel and Cloudflare

Before domain purchase:

```text
Vercel frontend uses API_BASE_URL=http://<alb-dns-name>
```

After Cloudflare domain purchase:

```text
www.<domain> -> Vercel frontend
api.<domain> -> AWS ALB backend
```

Then update:

```text
Vercel API_BASE_URL=https://api.<domain>
Terraform steamgg_cors_allowed_origins includes https://www.<domain>
```

Backend HTTPS requires a later Terraform change:

```text
ACM certificate for api.<domain>
ALB 443 listener
80 -> 443 redirect
```

## Verification Already Run Locally

These checks have passed locally:

```text
demo\mvnw.cmd test
demo\mvnw.cmd clean package -DskipTests
npm run lint
npm run build with API_BASE_URL=http://localhost:8080/
terraform -chdir=INFRA init
terraform -chdir=INFRA fmt -check -recursive
terraform -chdir=INFRA validate
terraform plan -refresh=false with dummy non-secret vars
actionlint .github/workflows/deploy.yml
bash -n INFRA/modules/ec2/userdata_steamgg.sh
git diff --check
```

Backend tests currently pass with:

```text
11 tests
0 failures
0 errors
```

Known local limitation:

```text
Docker CLI is not installed locally, so local Docker image build was not verified here.
Docker build is expected to run in GitHub Actions.
```

## Cost Risk

Do not leave the MVP stack running longer than needed.

Resources that can cost money after `terraform apply`:

```text
ALB
EC2
RDS
public IPv4
EBS
ECR storage
```

Destroy after testing:

```powershell
terraform -chdir=INFRA destroy
terraform -chdir=INFRA state list
```

## Commit Warning

Do not include unrelated local IDE files.

Currently known unrelated file:

```text
.idea/java_project.iml
```

Do not commit:

```text
terraform.tfstate
terraform.tfstate.*
*.tfvars
.env
.env.local
INFRA/.terraform/
frontend/.next/
frontend/node_modules/
demo/target/
```

Commit:

```text
INFRA/.terraform.lock.hcl
INFRA/terraform.tfvars.example
frontend/.env.example
docs/mvp-deploy-runbook.md
docs/mvp-handoff-summary.md
```
