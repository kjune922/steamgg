#output "steamgg_ecr_repository_url" {
#  value = aws_ecr_repository.steamgg_app_repo.repository_url
#}

#output "github_actions_role_arn" {
#  value = aws_iam_role.github_actions_role.arn
#}

output "steamgg_alb-arn" {
  value = module.alb.steamgg_alb_dns_name
}
output "steamgg_ecr_repository_url" {
  value = aws_ecr_repository.steamgg_app_repo.repository_url
}
output "steamgg_rds_endpoint" {
  value = module.rds.steamgg_rds_instance_address
}