FROM eclipse-temurin:17-jre

WORKDIR /app

# 빌드 산출물 복사 (fat JAR)
COPY build/libs/finmate-server.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]