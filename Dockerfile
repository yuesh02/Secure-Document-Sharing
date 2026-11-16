FROM eclipse-temurin:17-jre
WORKDIR /app
COPY target/secure-print-0.0.1-SNAPSHOT.jar app.jar
RUN mkdir -p /app/storage
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
