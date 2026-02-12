# Use lightweight Java 21 runtime
FROM eclipse-temurin:21-jre
# Set working directory inside container
WORKDIR /app

# Copy the Spring Boot fat jar built by Maven
COPY modules/erp-app/target/*.jar app.jar

# Expose application port
EXPOSE 8080

# Start Spring Boot app
ENTRYPOINT ["java","-jar","app.jar"]
