resource "aws_security_group" "steamgg_rds_sg" {
  name   = "steamgg-rds-sg-${terraform.workspace}"
  vpc_id = var.steamgg_vpc_id
  tags = {
    Name = "steamgg-rds-sg"
  }
}


resource "aws_security_group_rule" "steamgg_rds_ingress" {
  type      = "ingress"
  from_port = 5432
  to_port   = 5432
  protocol  = "tcp"

  # cidr_blocks가 아니라 보안그룹 지정
  security_group_id        = aws_security_group.steamgg_rds_sg.id
  source_security_group_id = var.steamgg_security_id
}

resource "aws_security_group_rule" "steamgg_rds_egress" {
  type              = "egress"
  from_port         = 0
  to_port           = 0
  protocol          = "-1"
  cidr_blocks       = ["0.0.0.0/0"]
  security_group_id = aws_security_group.steamgg_rds_sg.id
}

resource "aws_db_instance" "steamgg_main" {
  allocated_storage      = 20
  engine                 = "postgres"
  engine_version         = "16.8"
  instance_class         = "db.t3.micro"
  db_name                = var.db_name
  username               = var.db_username
  password               = var.db_password
  skip_final_snapshot    = true
  port                   = 5432
  multi_az               = false # 비용절감용
  db_subnet_group_name   = aws_db_subnet_group.steamgg_db_subnet_group.name
  vpc_security_group_ids = [aws_security_group.steamgg_rds_sg.id]
}

resource "aws_db_subnet_group" "steamgg_db_subnet_group" {
  name       = "steamgg-db-subnet-group-${terraform.workspace}"
  subnet_ids = var.steamgg_private_subnet_ids
  tags = {
    Name = "steamgg-db-subnet-group"
  }
}