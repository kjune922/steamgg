output "steamgg_alb_sg_id" {
  value = aws_security_group.steamgg_alb_sg.id
}

output "steamgg_target_group_arn" {
  value = aws_lb_target_group.steamgg_target.arn
}

output "steamgg_alb_dns_name" {
  value = aws_lb.steamgg_alb.dns_name
}