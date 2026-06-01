# SteamGG MVP Final Checklist

급하게 배포 확인할 때 이 파일만 위에서 아래로 체크한다.
상세 설명은 `docs/mvp-deploy-runbook.md`와 `docs/mvp-handoff-summary.md`를 본다.

## 1. Commit

- [ ] `.idea/java_project.iml` 제외
- [ ] `terraform.tfstate`, `*.tfvars`, `.env`, `.env.local` 제외
- [ ] `INFRA/.terraform.lock.hcl` 포함
- [ ] `INFRA/terraform.tfvars.example` 포함
- [ ] `frontend/.env.example` 포함
- [ ] `docs/` 문서 포함

권장 staging 명령:

```powershell
git add .github .gitattributes .gitignore README.md docs INFRA demo frontend
git reset -- .idea/java_project.iml
git status --short
```

## 2. GitHub Settings

- [ ] `AWS_ACCESS_KEY_ID` secret 설정
- [ ] `AWS_SECRET_ACCESS_KEY` secret 설정
- [ ] `AWS_REGION=ap-northeast-2` secret 설정
- [ ] `ECR_REPOSITORY=steamgg-app-repo-default` secret 설정
- [ ] 필요 시 `TERRAFORM_WORKSPACE=default` variable 설정
- [ ] GitHub Actions AWS principal에 ECR/EC2/SSM/ELB 권한 부여

## 3. Terraform Vars

`INFRA/terraform.tfvars`는 로컬에만 존재해야 한다.

- [ ] `steamgg_db_username` 설정
- [ ] `steamgg_db_password` 설정
- [ ] `steamgg_admin_sync_token` 설정
- [ ] `steamgg_cors_allowed_origins`에 Vercel URL 포함
- [ ] `steamgg_steam_auto_sync_enabled=false`

## 4. Local Verification

```powershell
demo\mvnw.cmd test
demo\mvnw.cmd clean package -DskipTests
npm --prefix frontend run lint
$env:API_BASE_URL = "http://localhost:8080"; npm --prefix frontend run build
terraform -chdir=INFRA init
terraform -chdir=INFRA fmt -check -recursive
terraform -chdir=INFRA validate
terraform -chdir=INFRA plan
actionlint .github/workflows/deploy.yml
```

- [ ] Backend test 통과
- [ ] Backend package 통과
- [ ] Frontend lint 통과
- [ ] Frontend build 통과
- [ ] Terraform fmt/validate/plan 통과
- [ ] GitHub Actions 문법 통과

## 5. AWS Deploy

비용 발생 가능 항목:

```text
ALB, EC2, RDS, public IPv4, EBS, ECR storage
```

- [ ] 비용 발생 승인
- [ ] `terraform -chdir=INFRA apply`
- [ ] `terraform -chdir=INFRA output -raw steamgg_alb_dns_name`
- [ ] `terraform -chdir=INFRA output -raw steamgg_ecr_repository_url`
- [ ] GitHub Actions `Deploy to Amazon EC2` 수동 실행
- [ ] `workspace=default`

## 6. Backend Runtime Check

```powershell
$alb = terraform -chdir=INFRA output -raw steamgg_alb_dns_name
curl "http://$alb/api/health"
curl -H "X-Admin-Token: <admin-sync-token>" "http://$alb/api/admin/sync"
curl "http://$alb/api/games"
```

- [ ] `/api/health` 200
- [ ] `/api/admin/sync` 200
- [ ] `/api/games` returns paginated JSON
- [ ] ALB target healthy

## 7. Vercel

- [ ] Project root: `frontend`
- [ ] `API_BASE_URL=http://<alb-dns-name>`
- [ ] Vercel deploy 성공
- [ ] Home page loads
- [ ] List page loads
- [ ] Search works
- [ ] Genre/tag filters work
- [ ] Detail page loads

## 8. Destroy After Test

테스트만 하고 끝낼 경우 비용 방지를 위해 삭제한다.

```powershell
terraform -chdir=INFRA destroy
terraform -chdir=INFRA state list
```

- [ ] `destroy` 완료
- [ ] `state list` 비어 있음

## 9. Domain Later

Cloudflare 도메인 구매 후:

- [ ] `www.<domain>` -> Vercel frontend
- [ ] `api.<domain>` -> AWS ALB backend
- [ ] Vercel `API_BASE_URL=https://api.<domain>`
- [ ] Terraform `steamgg_cors_allowed_origins`에 `https://www.<domain>` 추가
- [ ] 추후 backend HTTPS 필요 시 ACM + ALB 443 listener 추가
