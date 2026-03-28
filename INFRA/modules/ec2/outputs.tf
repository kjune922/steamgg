output "steamgg_sg_id" {
  value = aws_security_group.steamgg_sg.id
}

output "steamgg_asg_name" {
  value = aws_autoscaling_group.steamgg_asg.name
}