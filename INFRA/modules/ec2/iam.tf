# 1. ec2 서비스가 가져갈 역할들 허용
resource "aws_iam_role" "ec2_ssm_role" {
  name = "ec2-ssm-role-${terraform.workspace}"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "ec2.amazonaws.com"
        }
      }
    ]
  })
}

# 2. AWS가 제공하는 SSM 표준 권한(열쇠)을 역할에 부여
resource "aws_iam_role_policy_attachment" "ssm_policy" {
  role       = aws_iam_role.ec2_ssm_role.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore"
}

# 3. 인스턴스에 실제로 '부착'하기 위한 케이스(Profile) 생성
resource "aws_iam_instance_profile" "ec2_profile" {
  name = "ec2-ssm-profile-${terraform.workspace}"
  role = aws_iam_role.ec2_ssm_role.name
}

# EC2가 ECR에서 이미지를 읽어올 수 있도록 권한 추가
resource "aws_iam_role_policy_attachment" "ecr_read_only" {
  role       = aws_iam_role.ec2_ssm_role.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonEC2ContainerRegistryReadOnly"
}