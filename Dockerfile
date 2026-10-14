# Stage 1: Build the React frontend
FROM node:22-alpine AS frontend
WORKDIR /frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

# Stage 2: Build the Spring Boot app with the React build bundled as static content
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src ./src
COPY --from=frontend /frontend/dist ./src/main/resources/static
RUN mvn -q clean package -DskipTests

# Stage 3: Run the app
# Required env vars: DB_URL, DB_USERNAME, DB_PASSWORD, CLOUDINARY_CLOUD_NAME, CLOUDINARY_API_KEY, CLOUDINARY_API_SECRET
# Set FLYWAY_LOCATIONS=classpath:db/migration to skip the demo data (demo accounts share a public password)
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/ArtConnect-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
