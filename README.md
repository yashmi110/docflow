# Docflow - Document Management System

A Spring Boot 3.3 application for managing business documents and workflows including invoices, expense claims, and reimbursements.

## Tech Stack

- **Java 17**
- **Spring Boot 3.3.5**
- **Spring Security** (JWT + OAuth2)
- **Spring Data JPA**
- **MySQL 8**
- **Flyway** (Database migrations)
- **MapStruct** (Object mapping)
- **Lombok** (Boilerplate reduction)
- **Testcontainers** (Integration testing)

## Prerequisites

- Java 17 or higher
- Docker & Docker Compose (for MySQL)
- Gradle 8.5+ (wrapper included)

## Getting Started

### 1. Start MySQL Database

```bash
docker-compose up -d
```

This will start a MySQL 8 instance on `localhost:3306` with:
- Database: `docflow`
- Username: `root`
- Password: `pass`

### 2. Build the Application

```bash
./gradlew clean build
```

For Windows:
```cmd
gradlew.bat clean build
```

### 3. Run the Application

```bash
./gradlew bootRun
```

For Windows:
```cmd
gradlew.bat bootRun
```

The application will start on `http://localhost:8080`

### 4. Verify Application

Check the health endpoint:
```bash
curl http://localhost:8080/actuator/health
```

## Configuration

### Profiles

- **dev** (default): Development profile with debug logging and SQL output
- **prod**: Production profile with optimized settings

To run with a specific profile:
```bash
./gradlew bootRun --args='--spring.profiles.active=prod'
```

### Database Configuration

Development settings are in `src/main/resources/application-dev.properties`:
- URL: `jdbc:mysql://localhost:3306/docflow`
- Username: `root`
- Password: `pass`

Production settings use environment variables (see `application-prod.properties`).

## Project Structure

```
docflow/
├── src/
│   ├── main/
│   │   ├── java/com/docflow/
│   │   │   ├── DocflowApplication.java
│   │   │   └── config/
│   │   │       ├── SecurityConfig.java
│   │   │       └── WebConfig.java
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── application-dev.properties
│   │       ├── application-prod.properties
│   │       └── db/migration/
│   └── test/
├── build.gradle
├── settings.gradle
├── docker-compose.yml
└── README.md
```

## CORS Configuration

The application allows CORS requests from:
- `http://localhost:5173` (Vite/React)
- `http://localhost:3000` (Create React App)

## API Documentation

Once implemented, API documentation will be available at:
- Swagger UI: `http://localhost:8080/swagger-ui.html`

## Testing

Run all tests:
```bash
./gradlew test
```

Run with coverage report:
```bash
./gradlew test jacocoTestReport
```

Coverage report will be available at: `build/reports/jacoco/test/html/index.html`

## Database Migrations

Flyway migrations are located in `src/main/resources/db/migration/`

Migration naming convention: `V{version}__{description}.sql`

Example: `V1__create_users_table.sql`

## Development

### Adding Dependencies

Edit `build.gradle` and run:
```bash
./gradlew build --refresh-dependencies
```

### Code Style

The project uses `.editorconfig` for consistent code formatting across IDEs.

## Troubleshooting

### Database Connection Issues

1. Ensure Docker is running: `docker ps`
2. Check MySQL container: `docker-compose ps`
3. Verify MySQL is accessible: `docker-compose logs mysql`

### Build Issues

Clean and rebuild:
```bash
./gradlew clean build --refresh-dependencies
```

## License

Proprietary - All rights reserved

## Contact

For questions or support, contact the development team.
