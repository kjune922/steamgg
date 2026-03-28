variable "steamgg_vpc_id" {
  type = string
}

variable "steamgg_security_id" {
  type = string
}

variable "db_name" {
  type = string
}

variable "db_username" {  
  type = string
}

variable "db_password" {
  type = string
}

variable "steamgg_private_subnet_ids" {
  type = list(string)
}