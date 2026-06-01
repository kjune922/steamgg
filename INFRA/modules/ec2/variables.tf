variable "steamgg_vpc_id" {
  type = string
}

variable "steamgg_instance_type" {
  type = string
}

variable "steamgg_db_username" {
  type = string
}

variable "steamgg_db_password" {
  type      = string
  sensitive = true
}

variable "steamgg_rds_address" {
  type = string
}

variable "steamgg_alb_sg_id" {
  type = string
}
variable "steamgg_app_subnet_ids" {
  type = list(string)
}

variable "steamgg_target_group_arn" {
  type = string
}

variable "steamgg_ecr_repository_url" {
  type        = string
  description = "ECR Repository URL"
}

variable "steamgg_cors_allowed_origins" {
  type        = string
  description = "Comma-separated origins allowed to call the Spring Boot API."
}

variable "steamgg_admin_sync_token" {
  type        = string
  sensitive   = true
  description = "Optional token required for GET /api/admin/sync."
}

variable "steamgg_steam_auto_sync_enabled" {
  type        = bool
  description = "Whether the backend should run hourly Steam sync automatically."
}

