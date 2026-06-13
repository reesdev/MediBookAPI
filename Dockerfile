# ================================
# STAGE 1: Build Application
# ================================
# Menggunakan Java 17 sesuai dengan konfigurasi pom.xml Anda
FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /app

# Copy pom.xml dulu supaya dependency bisa di-cache
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy source code
COPY src ./src

# Build jar
RUN mvn clean package -DskipTests

# ================================
# STAGE 2: Runtime
# ================================
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

RUN apk add --no-cache wget

# Copy hasil build dari stage sebelumnya
COPY --from=build /app/target/*.jar app.jar

# Expose port (Disamakan dengan application.yml medibookAPI yaitu 3090)
EXPOSE 3090

# Default command 
ENTRYPOINT ["java", "-jar", "app.jar"]