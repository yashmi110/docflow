# Docflow - Quick Start Guide

## Step 1: Start MySQL Database

Open a terminal in the project directory and run:

```bash
docker-compose up -d
```

Verify MySQL is running:
```bash
docker-compose ps
```

You should see the `docflow-mysql` container running.

## Step 2: Build the Project

```bash
gradlew.bat clean build
```

This will:
- Download all dependencies
- Compile the code
- Run tests
- Create the application JAR

## Step 3: Run the Application

```bash
gradlew.bat bootRun
```

The application will:
1. Start on port 8080
2. Connect to MySQL
3. Run Flyway migrations
4. Be ready to accept requests

## Step 4: Verify

Open a browser or use curl:

```bash
curl http://localhost:8080/actuator/health
```

Expected response:
```json
{
  "status": "UP"
}
```

## Common Commands

### Stop MySQL
```bash
docker-compose down
```

### Stop MySQL and remove data
```bash
docker-compose down -v
```

### View MySQL logs
```bash
docker-compose logs -f mysql
```

### Connect to MySQL
```bash
docker exec -it docflow-mysql mysql -u root -ppass docflow
```

### Run tests only
```bash
gradlew.bat test
```

### Clean build
```bash
gradlew.bat clean build --refresh-dependencies
```

## Next Steps

1. The application is now running with basic security (temporarily allowing all API requests)
2. CORS is configured for React frontends on ports 3000 and 5173
3. Ready to add domain models, repositories, services, and controllers
4. Flyway migrations are in `src/main/resources/db/migration/`

## Troubleshooting

**Port 3306 already in use:**
- Stop other MySQL instances or change the port in `docker-compose.yml`

**Port 8080 already in use:**
- Change `server.port` in `application-dev.properties`

**Gradle build fails:**
- Ensure Java 17 is installed: `java -version`
- Try: `gradlew.bat clean build --refresh-dependencies`

**Database connection fails:**
- Check Docker is running: `docker ps`
- Check MySQL logs: `docker-compose logs mysql`
- Verify credentials in `application-dev.properties`
