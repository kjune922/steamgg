# 1단계: 빌드 스테이지
FROM maven:3.8.4-openjdk-17-slim AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
# 빌드 후 생성된 긴 이름의 jar 파일을 target/app.jar로 미리 복사
RUN mvn clean package -DskipTests && cp target/*.jar target/app.jar

# 2단계: 실행 스테이지
FROM openjdk:17-jdk-slim
WORKDIR /app
# 1단계에서 이미 이름을 바꿔뒀으므로 와일드카드 없이 가져옴
# --from=build (등호 포함)가 더 정확한 문법임
COPY --from=build /app/target/app.jar app.jar

# 컨테이너 실행
ENTRYPOINT ["java", "-jar", "app.jar"]