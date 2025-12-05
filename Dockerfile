# Sử dụng image JDK 17 chính thức
FROM eclipse-temurin:17-jdk AS builder

# Thư mục làm việc trong container
WORKDIR /app

# Copy file Maven config trước để cache dependency
COPY pom.xml mvnw* ./
COPY .mvn .mvn

# Cấp quyền thực thi cho mvnw (nếu cần)
RUN chmod +x mvnw

# Tải dependency (để lần sau build nhanh hơn)
RUN ./mvnw dependency:go-offline

# Copy toàn bộ source vào
COPY src src

# Build project, skip test cho nhanh
RUN ./mvnw clean package -DskipTests

# ----------------- STAGE RUNNER -----------------
FROM eclipse-temurin:17-jre

WORKDIR /app

# Copy file JAR từ stage builder
COPY --from=builder /app/target/*.jar app.jar

# Expose port BE (Spring Boot dùng 8080)
EXPOSE 8080

# Biến môi trường cho Spring Boot (tuỳ cậu cấu hình)
ENV JAVA_OPTS=""
ENV SPRING_PROFILES_ACTIVE=prod

# Lệnh chạy ứng dụng
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
