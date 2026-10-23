# Build do File Reader (backend Spring Boot + frontend Angular) num unico Dockerfile.
# Os estagios de build do front e do back sao independentes, entao o BuildKit roda os dois em paralelo.
#
#   docker compose up --build                         -> builda os dois e sobe tudo (com Postgres)
#   docker build --target backend  -t filereader-backend  .
#   docker build --target frontend -t filereader-frontend .

# ---------- Build do frontend ----------
FROM node:24-alpine AS frontend-build
WORKDIR /app

COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci

COPY frontend/ ./
RUN npm run build

# ---------- Build do backend ----------
FROM eclipse-temurin:21-jdk AS backend-build
WORKDIR /app

COPY backend/mvnw backend/pom.xml ./
COPY backend/.mvn .mvn
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

COPY backend/src src
RUN ./mvnw -B -q package -DskipTests && cp target/*.jar app.jar

# ---------- Imagem final do backend ----------
FROM eclipse-temurin:21-jre AS backend
WORKDIR /app

COPY --from=backend-build /app/app.jar app.jar

ENV FILEREADER_UPLOAD_DIR=/app/uploads
VOLUME /app/uploads
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]

# ---------- Imagem final do frontend (nginx servindo o build e fazendo proxy de /api) ----------
FROM nginx:1.27-alpine AS frontend

COPY infra/docker/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=frontend-build /app/dist/frontend/browser /usr/share/nginx/html

EXPOSE 80
