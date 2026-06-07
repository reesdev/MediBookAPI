# STAGE 1: Build Image
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app

# Menyalin file Maven wrapper
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Memberikan hak eksekusi pada wrapper dan mendownload dependency
RUN chmod +x ./mvnw
RUN ./mvnw dependency:go-offline -B

# Menyalin source code dan melakukan proses build (tanpa menjalankan test agar cepat)
COPY src src
RUN ./mvnw clean package -DskipTests

# STAGE 2: Run Image (Hanya menggunakan JRE agar ukuran kontainer sangat kecil)
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Meng-copy file JAR hasil build dari Stage 1
COPY --from=builder /app/target/medibook-0.0.1-SNAPSHOT.jar app.jar

# Membuka port sesuai aplikasi
EXPOSE 9000

# Menjalankan aplikasi
ENTRYPOINT ["java", "-jar", "app.jar"]
