# GreenLeaf - Plant & Terrarium Shop

Multi-module Maven project with React frontend and Spring Boot WebFlux backend.

## Project Structure

```
ClaudeTest/
├── pom.xml                  # Parent POM
├── frontend/                # React application (TypeScript + Bootstrap)
│   ├── pom.xml
│   ├── package.json
│   └── src/
└── backend/                 # Spring Boot WebFlux API (Kotlin + Coroutines)
    ├── pom.xml
    └── src/
```

## Technologies

### Frontend
- React 18
- TypeScript
- Bootstrap 5 & React-Bootstrap
- Axios
- Vite

### Backend
- Spring Boot 3.2.3
- Spring WebFlux (reactive)
- Kotlin 1.9.22
- Coroutines
- OpenAPI/Swagger UI

## Build & Run

### Build entire project
```bash
mvn clean install
```

This will:
1. Build frontend (npm install + vite build)
2. Copy frontend dist files to backend static resources
3. Build backend with embedded frontend

### Run application
```bash
cd backend
mvn spring-boot:run
```

Or run the JAR:
```bash
java -jar backend/target/backend-1.0.0.jar
```

## Endpoints

- **`http://localhost:8080/`** - React frontend (Plant shop homepage)
- **`http://localhost:8080/api/hello`** - API endpoint (returns text)
- **`http://localhost:8080/api/users`** - API users endpoint (returns JSON)
- **`http://localhost:8080/swagger-ui.html`** - Swagger UI (API documentation)
- **`http://localhost:8080/api-docs`** - OpenAPI JSON spec

## Design Theme

The frontend features a **plant shop aesthetic** with:
- Natural green color palette (#2d5016, #4a7c2c, #7fa650)
- Warm earth tones (#8b7355)
- Soft cream backgrounds (#f4f1ea)
- Nature-inspired design elements

## Development

### Frontend only (hot reload)
```bash
cd frontend
npm install
npm run dev
```

### Backend only
```bash
cd backend
mvn spring-boot:run
```

## Requirements

- Java 17+
- Maven 3.6+
- Node.js 20.11.0 (installed automatically by frontend-maven-plugin)
- npm 10.2.4 (installed automatically)
