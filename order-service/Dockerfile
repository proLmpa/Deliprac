FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY build/libs/app.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=test"]
