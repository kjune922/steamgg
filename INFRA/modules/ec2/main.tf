data "aws_ami" "amazon_linux_2023" {
  most_recent = true
  owners      = ["amazon"]

  filter {
    name   = "name"
    values = ["al2023-ami-2023.*-x86_64"]
  }
}

resource "aws_launch_template" "steamgg_launch_template" {
  name_prefix = "steamgg-app-lt-${terraform.workspace}"
  image_id    = data.aws_ami.amazon_linux_2023.id

  # The root module selects a smaller instance type for the default MVP workspace.
  instance_type = var.steamgg_instance_type

  vpc_security_group_ids = [aws_security_group.steamgg_sg.id]

  user_data = base64encode(templatefile("${path.module}/userdata_steamgg.sh", {
    DB_USERNAME_B64          = base64encode(var.steamgg_db_username),
    DB_PASSWORD_B64          = base64encode(var.steamgg_db_password),
    DB_ENDPOINT_B64          = base64encode(var.steamgg_rds_address),
    ECR_URL_B64              = base64encode(var.steamgg_ecr_repository_url),
    CORS_ALLOWED_ORIGINS_B64 = base64encode(var.steamgg_cors_allowed_origins),
    ADMIN_SYNC_TOKEN_B64     = base64encode(var.steamgg_admin_sync_token),
    STEAM_AUTO_SYNC_ENABLED  = tostring(var.steamgg_steam_auto_sync_enabled)
  }))

  iam_instance_profile {
    name = aws_iam_instance_profile.ec2_profile.name
  }

  # 디스크용량추가
  block_device_mappings {
    device_name = "/dev/xvda"
    ebs {
      volume_size = 30
      volume_type = "gp3"
    }
  }

  tag_specifications {
    resource_type = "instance"
    tags          = { Name = "steamgg-asg-instance-${terraform.workspace}" }
  }
}

# 단일ec2대신에 ASG 선언
resource "aws_autoscaling_group" "steamgg_asg" {
  vpc_zone_identifier = var.steamgg_app_subnet_ids

  desired_capacity = terraform.workspace == "prod" ? 2 : 1 #  평소유지할 서버수
  max_size         = terraform.workspace == "prod" ? 4 : 2
  min_size         = 1

  target_group_arns = [var.steamgg_target_group_arn]

  launch_template {
    id      = aws_launch_template.steamgg_launch_template.id
    version = "$Latest"
  }
}



resource "aws_security_group" "steamgg_sg" {
  name   = "steamgg-sg"
  vpc_id = var.steamgg_vpc_id

  ingress {
    from_port       = 8080
    to_port         = 8080
    protocol        = "tcp"
    security_groups = [var.steamgg_alb_sg_id]
  }

  ingress {
    from_port = 443
    to_port   = 443
    protocol  = "tcp"
    self      = true
  }
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
  tags = {
    Name = "steamgg-sg"
  }
}
