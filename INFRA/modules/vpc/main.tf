resource "aws_vpc" "steamgg_vpc" {
  cidr_block = "10.0.0.0/16"
  enable_dns_support = true
  enable_dns_hostnames = true
  tags = {
    Name = "test-vpc-${terraform.workspace}"
  }
}

resource "aws_subnet" "steamgg_public_subnet_1" {
  vpc_id = aws_vpc.steamgg_vpc.id
  cidr_block = "10.0.0.0/24"
  availability_zone = "ap-northeast-2a"

  tags = {
    Name = "steamgg-public-subnet-1"
  }
}

resource "aws_subnet" "steamgg_public_subnet_2" {
  vpc_id = aws_vpc.steamgg_vpc.id
  cidr_block = "10.0.1.0/24"
  availability_zone = "ap-northeast-2b"

  tags = {
    Name = "steamgg-public-subnet-2"
  }
}

resource "aws_subnet" "steamgg_private_subnet_1" {
  vpc_id = aws_vpc.steamgg_vpc.id
  cidr_block = "10.0.2.0/24"
  availability_zone = "ap-northeast-2a"

  tags = {
    Name = "steamgg-private-subnet-1"
  }
}

resource "aws_subnet" "steamgg_private_subnet_2" {
  vpc_id = aws_vpc.steamgg_vpc.id
  cidr_block = "10.0.3.0/24"
  availability_zone = "ap-northeast-2b"

  tags = {
    Name = "steamgg-private-subnet-2"
  }
}

resource "aws_internet_gateway" "steamgg_igw" {
  vpc_id = aws_vpc.steamgg_vpc.id
  tags = {
    Name = "steamgg-igw"
  }
}

resource "aws_route_table" "steamgg_public_rt" {
  vpc_id = aws_vpc.steamgg_vpc.id
  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.steamgg_igw.id
  }

  tags = {
    Name = "steamgg-public-rt"
  }
}

resource "aws_route_table" "steamgg_private_rt" {
  vpc_id = aws_vpc.steamgg_vpc.id
  tags = {
    Name = "steamgg-private-rt"
  }
}

resource "aws_route_table_association" "steamgg_public_rt_assoc_1"{
  subnet_id = aws_subnet.steamgg_public_subnet_1.id
  route_table_id = aws_route_table.steamgg_public_rt.id
}

resource "aws_route_table_association" "steamgg_public_rt_assoc_2"{
  subnet_id = aws_subnet.steamgg_public_subnet_2.id
  route_table_id = aws_route_table.steamgg_public_rt.id
}

resource "aws_route_table_association" "steamgg_private_rt_assoc_1" {
  subnet_id = aws_subnet.steamgg_private_subnet_1.id
  route_table_id = aws_route_table.steamgg_private_rt.id
}

resource "aws_route_table_association" "steamgg_private_rt_assoc_2" {
  subnet_id = aws_subnet.steamgg_private_subnet_2.id
  route_table_id = aws_route_table.steamgg_private_rt.id
}

resource "aws_eip" "steamgg_nat_eip" {
  domain = "vpc"
  tags = {
    Name = "steamgg-nat-eip-${terraform.workspace}"
  }
}

resource "aws_nat_gateway" "steamgg_nat_gw" {
  allocation_id = aws_eip.steamgg_nat_eip.id
  subnet_id = aws_subnet.steamgg_public_subnet_1.id
  depends_on = [aws_internet_gateway.steamgg_igw]
  tags = {
    Name = "steamgg-nat-gw-${terraform.workspace}"
  }
}

resource "aws_route" "private_nat_route" {
  route_table_id = aws_route_table.steamgg_private_rt.id
  destination_cidr_block = "0.0.0.0/0"
  nat_gateway_id = aws_nat_gateway.steamgg_nat_gw.id
}