# File Reader

Backend Spring Boot (`backend/`) + frontend Angular (`frontend/`). Documentação completa em [backend/README.md](backend/README.md).

## Com Docker (recomendado)

Pré-requisito: Docker Desktop aberto.

```bash
docker compose up --build
```

Acesse http://localhost:8088. Sobe Postgres, backend e frontend; front e back são buildados em paralelo.

Parar: `docker compose down` (banco e uploads ficam nos volumes; `docker compose down -v` apaga tudo).

Buildar só uma imagem:

```bash
docker build --target backend  -t filereader-backend  .
docker build --target frontend -t filereader-frontend .
```

## Sem Docker

Pré-requisitos: Java 21, Node 24 e PostgreSQL com o banco `filereader` (usuário/senha `postgres`).

1. Banco (se não tiver Postgres local):
   ```bash
   docker run --name filereader-db -e POSTGRES_DB=filereader -e POSTGRES_PASSWORD=postgres -p 5432:5432 -d postgres:17
   ```
2. Backend — API em http://localhost:8080:
   ```bash
   cd backend
   ./mvnw spring-boot:run        # Windows: mvnw.cmd spring-boot:run
   ```
3. Frontend — app em http://localhost:4200 (repassa `/api` para o backend):
   ```bash
   cd frontend
   npm ci
   npm start
   ```

Builds de produção: `./mvnw package` (jar em `backend/target/`) e `npm run build` (em `frontend/dist/`).
