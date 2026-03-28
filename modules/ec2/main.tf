data "aws_ami" "amazon_linux_2023" {
  most_recent = true
  owners = ["amazon"]

  filter {
    name = "name"
    values = ["al2023-ami-2023.*-x86_64"]
  }
}

resource "aws_launch_template" "steamgg_launch_template" {
  name_prefix = "steamgg-app-lt-${terraform.workspace}"
  image_id = data.aws_ami.amazon_linux_2023.id

  # prod면 t2.micro, 아니면 t3.micro
  instance_type = var.steamgg_instance_type
  
  vpc_security_group_ids = [aws_security_group.steamgg_sg.id]

  user_data = base64encode(templatefile("${path.module}/userdata_steamgg.sh",{db_endpoint = var.steamgg_rds_address,
  db_username = var.steamgg_db_username,
  db_password = var.steamgg_db_password 
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
    tags = { Name = "steamgg-asg-instance-${terraform.workspace}" }
  }
}

# 단일ec2대신에 ASG 선언
resource "aws_autoscaling_group" "steamgg_asg" {
  vpc_zone_identifier = var.steamgg_private_subnet_ids

  desired_capacity = terraform.workspace == "prod" ? 2 : 1 #  평소유지할 서버수
  max_size = terraform.workspace == "prod" ? 4 : 2
  min_size = 1

  target_group_arns = [var.steamgg_target_group_arn]

  launch_template {
  id = aws_launch_template.steamgg_launch_template.id
  version = "$Latest"
  }
}



resource "aws_security_group" "steamgg_sg"{
  name = "steamgg-sg"
  vpc_id = var.steamgg_vpc_id

  ingress {
  from_port = 80
  to_port = 80
  protocol = "tcp"
  security_groups = [var.steamgg_alb_sg_id] # 이제 모든 주소범위는 보안그룹id로 바뀌면서, alb한테 받아함야
  }

  ingress {
  from_port = 443
  to_port = 443
  protocol = "tcp"
  self = true
  }
  egress {
    from_port = 0
    to_port = 0
    protocol = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
  tags = {
    Name = "steamgg-sg"
  }
}