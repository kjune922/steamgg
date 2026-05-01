variable "steamgg_vpc_id" {
  type = string
}

variable "steamgg_instance_type"{
  type = string
}

variable "steamgg_db_username"{
  type = string
}

variable "steamgg_db_password" {
  type =string
}

variable "steamgg_rds_address" {
  type = string
}

variable "steamgg_alb_sg_id" {
  type = string
}
variable "steamgg_private_subnet_ids"{
  type = list(string)
}

variable "steamgg_target_group_arn"{
  type = string
}
