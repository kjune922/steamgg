variable "steamgg_db_username" {
  type = string

  validation {
    condition     = can(regex("^[A-Za-z][A-Za-z0-9_]{0,15}$", var.steamgg_db_username))
    error_message = "steamgg_db_username must start with a letter and contain only letters, numbers, or underscores, with a maximum length of 16."
  }
}
variable "steamgg_db_password" {
  type      = string
  sensitive = true

  validation {
    condition = (
      length(var.steamgg_db_password) >= 8 &&
      length(var.steamgg_db_password) <= 128 &&
      !can(regex("[/\"'@ ]", var.steamgg_db_password))
    )
    error_message = "steamgg_db_password must be 8-128 characters and must not contain slash, single quote, double quote, at sign, or spaces."
  }
}

variable "steamgg_cors_allowed_origins" {
  type        = string
  default     = "http://localhost:3000"
  description = "Comma-separated origins allowed to call the Spring Boot API."
}

variable "steamgg_admin_sync_token" {
  type        = string
  default     = ""
  sensitive   = true
  description = "Optional token required for GET /api/admin/sync."
}

variable "steamgg_steam_auto_sync_enabled" {
  type        = bool
  default     = false
  description = "Whether the backend should run hourly Steam sync automatically."
}
