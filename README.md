# SteamGG MVP

SteamGG is a Steam game discovery MVP with a Next.js frontend, Spring Boot API, PostgreSQL, and AWS infrastructure.

For the actual MVP deployment sequence, use [docs/mvp-deploy-runbook.md](docs/mvp-deploy-runbook.md).
For the current handoff summary, use [docs/mvp-handoff-summary.md](docs/mvp-handoff-summary.md).
For a short deployment checklist, use [docs/mvp-final-checklist.md](docs/mvp-final-checklist.md).

## Target Architecture

```text
Cloudflare DNS
  -> www domain -> Vercel frontend
  -> api domain -> AWS ALB backend

Vercel
  -> Next.js frontend in frontend/

AWS
  -> ALB :80
  -> Target Group :8080
  -> EC2 Docker Spring Boot app :8080
  -> RDS PostgreSQL
  -> ECR image repository
  -> GitHub Actions deploys through SSM
```

Until a domain is purchased, use the Vercel production URL for the frontend and the ALB DNS name for the API.

## Local Development

Backend:

```powershell
cd demo
$env:DB_ENDPOINT = "localhost"
$env:DB_NAME = "steamgg_postgresql"
$env:DB_USERNAME = "postgres"
$env:DB_PASSWORD = "<local password>"
.\mvnw.cmd spring-boot:run
```

Frontend:

```powershell
cd frontend
copy .env.example .env.local
npm install
npm run dev
```

Set `frontend/.env.local`:

```text
API_BASE_URL=http://localhost:8080
```

## Required API Contract

The frontend expects these backend endpoints:

```text
GET /api/health
GET /api/games?q=&genre=&tag=&sort=&page=&size=
GET /api/games/{id}
GET /api/games/facets
GET /api/games/curations
GET /api/games/{id}/similar
GET /api/admin/sync
```

`GET /api/games` returns a paginated response:

```json
{
  "items": [],
  "totalCount": 0,
  "totalPages": 1,
  "page": 1,
  "size": 12
}
```

## AWS Infrastructure

Create a local Terraform variable file from the example:

```powershell
copy INFRA\terraform.tfvars.example INFRA\terraform.tfvars
```

Set real local values in `INFRA/terraform.tfvars`. Do not commit that file.
For RDS PostgreSQL, keep `steamgg_db_username` at 16 characters or less using only letters, numbers, or underscores, starting with a letter.
Keep `steamgg_db_password` at 8-128 characters and avoid `/`, `'`, `"`, `@`, and spaces.

Validation:

```powershell
terraform -chdir=INFRA init
terraform -chdir=INFRA fmt -check -recursive
terraform -chdir=INFRA validate
terraform -chdir=INFRA plan
```

Apply sequence for an MVP test:

```powershell
terraform -chdir=INFRA apply
terraform -chdir=INFRA output
```

After `terraform apply`, run the `Deploy to Amazon EC2` workflow manually from GitHub Actions. Use `workspace=default` unless you created another Terraform workspace.
Pushes to feature branches run the quality jobs only. Backend deployment runs on manual workflow dispatch, or automatically when `main` is pushed.
The ALB target can be unhealthy immediately after `terraform apply`; it becomes healthy only after GitHub Actions deploys the backend container.

Smoke checks after deploy:

```powershell
$alb = terraform -chdir=INFRA output -raw steamgg_alb_dns_name
curl "http://$alb/api/health"
curl -H "X-Admin-Token: <admin-sync-token>" "http://$alb/api/admin/sync"
curl "http://$alb/api/games"
```

Use the same admin token that you set in `steamgg_admin_sync_token`. The sync call seeds the first batch of Steam game data for the MVP list/detail pages.

To avoid ongoing AWS charges after testing:

```powershell
terraform -chdir=INFRA destroy
terraform -chdir=INFRA state list
```

Resources that can incur noticeable cost while applied include ALB, EC2, RDS, public IPv4 addresses, EBS volumes, and ECR storage.
The MVP app EC2 instances run in public subnets for outbound access without a NAT Gateway, but inbound traffic to the app is still restricted to the ALB security group on `8080`.
The default Terraform workspace uses a `t3.micro` EC2 instance for MVP cost control. If the Spring Boot container runs out of memory during testing, temporarily raise the instance type and destroy the stack after the test.

The backend container listens on `8080`, so the AWS routing must stay aligned:

```text
ALB listener: 80
Target Group: 8080
EC2 Security Group: allow ALB security group to 8080
Target Group health check: /api/health
```

## Backend Deployment

GitHub Actions builds the Docker image and pushes it to ECR. After push, it uses SSM to run the EC2 deploy script:

```text
/opt/steamgg/deploy.sh
```

The EC2 user-data prepares Docker and writes that deploy script. Application deployment is triggered by GitHub Actions, not by Terraform apply.

Required GitHub secrets:

```text
AWS_ACCESS_KEY_ID
AWS_SECRET_ACCESS_KEY
AWS_REGION
ECR_REPOSITORY
```

`ECR_REPOSITORY` is the repository name, not the full ECR URL. For the default Terraform workspace, set it to:

```text
steamgg-app-repo-default
```

The AWS principal used by GitHub Actions must be allowed to:

```text
ecr:GetAuthorizationToken
ecr:BatchCheckLayerAvailability
ecr:InitiateLayerUpload
ecr:UploadLayerPart
ecr:CompleteLayerUpload
ecr:PutImage
ec2:DescribeInstances
ssm:SendCommand
ssm:GetCommandInvocation
elasticloadbalancing:DescribeLoadBalancers
sts:GetCallerIdentity
```

MVP IAM policy example for the GitHub Actions AWS principal:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": "ecr:GetAuthorizationToken",
      "Resource": "*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "ecr:BatchCheckLayerAvailability",
        "ecr:InitiateLayerUpload",
        "ecr:UploadLayerPart",
        "ecr:CompleteLayerUpload",
        "ecr:PutImage"
      ],
      "Resource": "arn:aws:ecr:ap-northeast-2:<account-id>:repository/steamgg-app-repo-*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "ec2:DescribeInstances",
        "elasticloadbalancing:DescribeLoadBalancers",
        "sts:GetCallerIdentity"
      ],
      "Resource": "*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "ssm:SendCommand",
        "ssm:GetCommandInvocation"
      ],
      "Resource": "*"
    }
  ]
}
```

Optional GitHub repository variable:

```text
TERRAFORM_WORKSPACE=default
```

## Frontend Deployment

Deploy `frontend/` to Vercel.

Vercel environment variable:

```text
API_BASE_URL=http://<alb-dns-name>
```

After domain setup, change it to:

```text
API_BASE_URL=https://api.<your-domain>
```

The backend CORS origins are controlled by Terraform variable:

```text
steamgg_cors_allowed_origins
```

The manual Steam sync endpoint is disabled unless `ADMIN_SYNC_TOKEN` is set through Terraform variable `steamgg_admin_sync_token`. Call it with:

```text
GET /api/admin/sync
X-Admin-Token: <token>
```

The scheduled Steam sync is disabled by default for MVP deployments. Use the token-protected admin sync endpoint to seed data after deploy. If the team later wants hourly automatic sync, set Terraform variable `steamgg_steam_auto_sync_enabled=true`.

For Vercel and a later Cloudflare domain, use a comma-separated value similar to:

```text
http://localhost:3000,https://steamgg.vercel.app,https://www.<your-domain>
```

## Cloudflare Domain Plan

When the domain is purchased in Cloudflare:

```text
www.<your-domain> -> Vercel frontend
api.<your-domain> -> AWS ALB backend
```

For HTTPS on the backend, issue an ACM certificate for `api.<your-domain>`, attach it to an ALB `443` listener, and redirect ALB `80` to `443`.

## Sensitive Files

Do not commit:

```text
terraform.tfstate
*.tfvars
.env
.env.local
AWS credentials
DB passwords
```

Terraform state can contain rendered user-data and variable values. Keep state local and uncommitted for MVP testing, or move it to a properly protected remote backend before sharing credentials.

## Commit Checklist

Commit the application, infrastructure, workflow, docs, and example environment files for this MVP pass.
Do not include local IDE changes or generated runtime output.

Files that should stay out of the MVP commit include:

```text
.idea/
demo/target/
frontend/.next/
frontend/node_modules/
INFRA/.terraform/
terraform.tfstate
*.tfvars
.env
.env.local
```

`INFRA/.terraform.lock.hcl` should be committed so the Terraform provider versions are reproducible.

## MVP Verification Checklist

```text
demo/mvnw.cmd test
demo/mvnw.cmd clean package -DskipTests
npm --prefix frontend run lint
$env:API_BASE_URL = "http://localhost:8080"; npm --prefix frontend run build
terraform -chdir=INFRA fmt -check -recursive
terraform -chdir=INFRA validate
terraform -chdir=INFRA plan -refresh=false
actionlint .github/workflows/deploy.yml
```

Runtime checks after deployment:

```text
GET /api/health = 200
GET /api/games = 200
ALB target health = healthy
Vercel frontend loads home/list/detail pages
GitHub Actions deploy updates the EC2 container
terraform destroy removes AWS MVP resources
```

Local verification limits:

```text
Docker image build requires Docker Desktop or another Docker engine.
Full ALB/Vercel runtime verification requires terraform apply, GitHub Actions deploy, and Vercel deployment.
```
