# Stage 1: Build dự án bằng Maven và JDK 25
FROM maven:3-eclipse-temurin-25 AS build
WORKDIR /app

# Copy toàn bộ mã nguồn vào trong Container
COPY . .

# Đóng gói ứng dụng (Bỏ qua chạy test để build nhanh hơn trên Render)
RUN mvn clean package -DskipTests

# Stage 2: Tạo môi trường chạy siêu nhẹ với JDK 25 JRE
FROM eclipse-temurin:25-jre-noble
WORKDIR /app

# Tự động bốc file đóng gói (*.jar hoặc *.war) từ Stage 1 sang và đổi tên thành app.jar
COPY --from=build /app/target/*.?ar app.jar

# Mở cổng 8080 để đón request từ Internet
EXPOSE 8080

# Lệnh kích hoạt server khi deploy lên Render
ENTRYPOINT ["java","-jar","app.jar"]