provider "aws" {
  region = "ap-northeast-2"
}

module "ec2" {
  source = "./modules/ec2"
  steamgg_vpc_id = module.vpc.steamgg_vpc_id
  steamgg_instance_type = terraform.workspace == "default" ? "t3.large" : "t2.medium"
  steamgg_rds_address = module.rds.steamgg_rds_instance_address
  steamgg_alb_sg_id = module.alb.steamgg_alb_sg_id
  steamgg_private_subnet_ids = [module.vpc.steamgg_private_subnet_ids[0],module.vpc.steamgg_private_subnet_ids[1]]
  steamgg_target_group_arn = module.alb.steamgg_target_group_arn
  steamgg_db_username = var.steamgg_db_username
  steamgg_db_password = var.steamgg_db_password
  steamgg_ecr_repository_url = aws_ecr_repository.steamgg_app_repo.repository_url
}

module "vpc" {
  source = "./modules/vpc"
}

module "rds" {
  source = "./modules/rds"
  steamgg_vpc_id = module.vpc.steamgg_vpc_id
  db_name = "steamgg_PostgreSQL"
  db_username = "postgres"
  db_password = "dlrudalswns2!!"
  steamgg_private_subnet_ids = [module.vpc.steamgg_private_subnet_ids[0],module.vpc.steamgg_private_subnet_ids[1]]
  steamgg_security_id = module.ec2.steamgg_sg_id
}

module "alb" {
  source = "./modules/alb"
  steamgg_vpc_id = module.vpc.steamgg_vpc_id
  steamgg_public_subnet_ids = [module.vpc.steamgg_public_subnet_ids[0],module.vpc.steamgg_public_subnet_ids[1]]
}