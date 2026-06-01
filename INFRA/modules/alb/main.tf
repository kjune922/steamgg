resource "aws_lb" "steamgg_alb" {
  name               = "steamgg-alb-${terraform.workspace}"
  internal           = false # 외부 노출용이라는 뜻
  load_balancer_type = "application"
  security_groups    = [aws_security_group.steamgg_alb_sg.id]
  subnets            = var.steamgg_public_subnet_ids # ALB는 퍼블릭 서브넷에 있어야함
  idle_timeout       = 300

  tags = {
    Name = "steamgg-alb"
  }
}

# 2. 대상 그룹(Target_group)

resource "aws_lb_target_group" "steamgg_target" {
  name_prefix = "sgtg-"
  port        = 8080
  protocol    = "HTTP"
  vpc_id      = var.steamgg_vpc_id

  health_check {
    path                = "/api/health"
    interval            = 30
    timeout             = 5
    healthy_threshold   = 2
    unhealthy_threshold = 2
  }

  lifecycle {
    create_before_destroy = true
  }
}

resource "aws_lb_listener" "steam_gg_listener" {
  load_balancer_arn = aws_lb.steamgg_alb.arn
  port              = "80"
  protocol          = "HTTP"

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.steamgg_target.arn
  }
}

# ALB전용 보안그룹


resource "aws_security_group" "steamgg_alb_sg" {
  name   = "steamgg-alb-sg-${terraform.workspace}"
  vpc_id = var.steamgg_vpc_id

  # 인바운드 -> 전세계에서 포트80으로 들어오는거 허용
  ingress {
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "steamgg-alb-sg"
  }
}
