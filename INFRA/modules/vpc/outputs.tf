output "steamgg_vpc_id" {
  value = aws_vpc.steamgg_vpc.id
}

output "steamgg_public_subnet_ids" {
  value = [aws_subnet.steamgg_public_subnet_1.id, aws_subnet.steamgg_public_subnet_2.id]
}

output "steamgg_private_subnet_ids" {
  value = [aws_subnet.steamgg_private_subnet_1.id, aws_subnet.steamgg_private_subnet_2.id]
}