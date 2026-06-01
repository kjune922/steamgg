# SteamGG MVP Deploy Runbook

이 문서는 도메인 구매 전까지 MVP를 실제로 띄우는 순서만 정리한다.
Cloudflare 도메인은 마지막에 연결한다.

## Current Target

```text
Vercel frontend
  -> API_BASE_URL=http://<alb-dns-name>

AWS backend
  -> ALB :80
  -> Target Group :8080
  -> EC2 Docker container :8080
  -> RDS PostgreSQL
  -> ECR image

GitHub Actions
  -> Docker build/push to ECR
  -> SSM command to EC2
  -> /opt/steamgg/deploy.sh
```

도메인 구매 후 목표 구조:

```text
www.<domain> -> Vercel frontend
api.<domain> -> AWS ALB backend
```

## Before Commit

커밋에 포함할 것:

```text
.github/workflows/deploy.yml
.gitattributes
.gitignore
README.md
docs/mvp-deploy-runbook.md
INFRA/.terraform.lock.hcl
INFRA/terraform.tfvars.example
INFRA/**/*.tf
INFRA/modules/ec2/userdata_steamgg.sh
demo/Dockerfile
demo/.dockerignore
demo/src/main/**
demo/src/test/**
frontend/.env.example
frontend/.gitignore
frontend/app/**
```

커밋에서 제외할 것:

```text
.idea/
demo/target/
frontend/.next/
frontend/node_modules/
INFRA/.terraform/
terraform.tfstate
terraform.tfstate.*
*.tfvars
.env
.env.local
```

현재 작업트리에 `.idea/java_project.iml`이 보이면 이번 MVP 커밋에서 제외한다.

## Required Local Files

`INFRA/terraform.tfvars`는 로컬에만 만든다.

```powershell
copy INFRA\terraform.tfvars.example INFRA\terraform.tfvars
```

필수 값:

```text
steamgg_db_username
steamgg_db_password
steamgg_admin_sync_token
steamgg_cors_allowed_origins
steamgg_steam_auto_sync_enabled
```

권장 MVP 값:

```text
steamgg_steam_auto_sync_enabled = false
steamgg_cors_allowed_origins = "http://localhost:3000,https://<vercel-app>.vercel.app"
```

RDS password 제약:

```text
8-128 characters
no slash
no single quote
no double quote
no at sign
no spaces
```

## GitHub Settings

GitHub Secrets:

```text
AWS_ACCESS_KEY_ID
AWS_SECRET_ACCESS_KEY
AWS_REGION
ECR_REPOSITORY
```

Default workspace 기준:

```text
AWS_REGION=ap-northeast-2
ECR_REPOSITORY=steamgg-app-repo-default
```

선택 GitHub variable:

```text
TERRAFORM_WORKSPACE=default
```

GitHub Actions AWS principal에는 ECR push, EC2 describe, SSM send/get, ELB describe 권한이 필요하다.
구체적인 IAM policy 예시는 `README.md`의 Backend Deployment 섹션을 따른다.

## Preflight

로컬에서 배포 전 확인:

```powershell
demo\mvnw.cmd test
demo\mvnw.cmd clean package -DskipTests
npm --prefix frontend run lint
$env:API_BASE_URL = "http://localhost:8080"; npm --prefix frontend run build
terraform -chdir=INFRA init
terraform -chdir=INFRA fmt -check -recursive
terraform -chdir=INFRA validate
terraform -chdir=INFRA plan -refresh=false
actionlint .github/workflows/deploy.yml
```

Docker Desktop이 없으면 로컬 Docker build 검증은 생략 가능하다.
GitHub Actions runner에서 Docker build/push가 수행된다.

## Deploy Order

1. 변경사항을 커밋하고 원격 브랜치에 push한다.
2. GitHub Actions의 quality jobs가 통과하는지 확인한다.
3. Terraform plan을 확인한다.

```powershell
terraform -chdir=INFRA plan
```

4. 비용 발생을 승인한 뒤 AWS 인프라를 생성한다.

```powershell
terraform -chdir=INFRA apply
```

5. ALB DNS와 ECR URL을 확인한다.

```powershell
terraform -chdir=INFRA output
terraform -chdir=INFRA output -raw steamgg_alb_dns_name
terraform -chdir=INFRA output -raw steamgg_ecr_repository_url
```

6. GitHub Actions에서 `Deploy to Amazon EC2` workflow를 수동 실행한다.

```text
workflow_dispatch input:
workspace=default
```

7. ALB health를 확인한다.

```powershell
$alb = terraform -chdir=INFRA output -raw steamgg_alb_dns_name
curl "http://$alb/api/health"
```

8. Steam 데이터 seed를 실행한다.

```powershell
curl -H "X-Admin-Token: <admin-sync-token>" "http://$alb/api/admin/sync"
```

9. API 목록을 확인한다.

```powershell
curl "http://$alb/api/games"
```

10. Vercel에서 `frontend/`를 배포한다.

Vercel setting:

```text
Root Directory: frontend
API_BASE_URL=http://<alb-dns-name>
```

11. Vercel URL에서 확인한다.

```text
home page loads
list page loads
search works
genre/tag filters work
detail page loads
similar games section works when data exists
```

## Expected Temporary States

`terraform apply` 직후:

```text
ALB target can be unhealthy
```

정상이다. 아직 GitHub Actions가 Docker image를 ECR에 push하고 EC2에서 deploy script를 실행하기 전이다.

GitHub Actions deploy 직후:

```text
/api/health should return 200
ALB target should become healthy
```

Steam sync 전:

```text
/api/games can return an empty page
```

admin sync 실행 후 데이터가 채워지는지 확인한다.

## Common Failures

`No running EC2 instances found`:

```text
Terraform apply가 끝났는지 확인한다.
workspace 이름이 default인지 확인한다.
EC2 tag Name=steamgg-asg-instance-default가 있는지 확인한다.
```

`SSM send-command failed`:

```text
EC2 IAM role에 AmazonSSMManagedInstanceCore가 붙었는지 확인한다.
EC2가 public subnet에서 outbound internet을 쓸 수 있는지 확인한다.
amazon-ssm-agent가 실행 중인지 확인한다.
```

`ALB 504`:

```text
GitHub Actions deploy가 끝났는지 확인한다.
Target Group port가 8080인지 확인한다.
EC2 Security Group이 ALB SG -> 8080을 허용하는지 확인한다.
컨테이너가 8080으로 떠 있는지 확인한다.
RDS 접속 env가 맞는지 확인한다.
```

`Vercel page shows no data`:

```text
API_BASE_URL이 http://<alb-dns-name>인지 확인한다.
backend CORS에 Vercel URL이 포함됐는지 확인한다.
/api/admin/sync를 실행했는지 확인한다.
```

## Cost Notes

`terraform apply` 이후 비용이 발생할 수 있는 항목:

```text
ALB
EC2
RDS
public IPv4
EBS
ECR storage
```

MVP는 NAT Gateway를 만들지 않는다.
테스트가 끝나면 비용 방지를 위해 destroy한다.

```powershell
terraform -chdir=INFRA destroy
terraform -chdir=INFRA state list
```

`state list`가 비어 있으면 Terraform이 관리하던 리소스는 삭제된 상태다.

## Domain Later

Cloudflare 도메인을 산 뒤:

```text
www.<domain> -> Vercel frontend
api.<domain> -> AWS ALB backend
```

그 다음 변경:

```text
Vercel API_BASE_URL=https://api.<domain>
Terraform steamgg_cors_allowed_origins includes https://www.<domain>
```

백엔드 HTTPS까지 하려면 ACM 인증서와 ALB 443 listener를 추가한다.
