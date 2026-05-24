FROM eclipse-temurin:24-jre-noble
WORKDIR /app
COPY build/libs/app.jar app.jar
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS:-} -jar app.jar"]
