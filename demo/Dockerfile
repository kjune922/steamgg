# 1단계: 빌드 스테이지
# maven 공식 이미지 중 eclipse-temurin 기반 버전을 사용합니다.
FROM maven:3.8.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
# 빌드 후 생성된 jar 파일을 target/app.jar로 미리 복사
RUN mvn clean package -DskipTests && cp target/*.jar target/app.jar

# 2단계: 실행 스테이지
# 에러가 났던 openjdk:17-jdk-slim 대신 공식 지원되는 temurin 이미지를 사용합니다.
FROM eclipse-temurin:17-jdk-jammy
WORKDIR /app
# 1단계에서 빌드된 jar 파일만 가져옵니다.
COPY --from=build /app/target/app.jar app.jar

EXPOSE 8080

# 컨테이너 실행
ENTRYPOINT ["java", "-jar", "app.jar"]
