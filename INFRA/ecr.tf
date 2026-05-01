resource "aws_ecr_repository" "steamgg_app_repo" {
  name                 = "steamgg-app-repo-${terraform.workspace}"
  image_tag_mutability = "MUTABLE"

  image_scanning_configuration {
    scan_on_push = true
  }

  tags = {
    Name = "steamgg-ecr"
  }
}