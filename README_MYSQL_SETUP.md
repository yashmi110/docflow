# MySQL Connection Troubleshooting

## Current Issue

Your application cannot connect to MySQL. Here's how to fix it:

## Quick Diagnosis

You have MySQL running on port 3306 (PID 6612), but the application can't connect.

### Possible Causes

1. **Wrong password** - Application uses `pass`, but MySQL root password might be different
2. **Database doesn't exist** - `docflow` database not created
3. **Authentication plugin** - MySQL 8.0 uses `caching_sha2_password` by default

## Solution Options

### Option 1: Use Existing MySQL (Recommended if you know the password)

#### Step 1: Find MySQL Installation

Common locations:
```
C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe
C:\xampp\mysql\bin\mysql.exe
C:\wamp64\bin\mysql\mysql8.0.x\bin\mysql.exe
```

#### Step 2: Connect to MySQL

Open Command Prompt and navigate to MySQL bin directory, then:

```bash
# If you know the root password:
mysql -u root -p

# Enter your password when prompted
```

#### Step 3: Create Database and User

```sql
-- Create database
CREATE DATABASE IF NOT EXISTS docflow CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Create user (optional - for better security)
CREATE USER IF NOT EXISTS 'docflow_user'@'localhost' IDENTIFIED BY 'docflow_pass';

-- Grant privileges
GRANT ALL PRIVILEGES ON docflow.* TO 'docflow_user'@'localhost';
FLUSH PRIVILEGES;

-- Verify
SHOW DATABASES LIKE 'docflow';
```

#### Step 4: Update application-dev.properties

If you created a dedicated user:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/docflow?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true
spring.datasource.username=docflow_user
spring.datasource.password=docflow_pass
```

Or if using root with different password:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/docflow?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=YOUR_ACTUAL_PASSWORD
```

### Option 2: Stop Native MySQL and Use Docker

#### Step 1: Stop MySQL Service

```powershell
# Find MySQL service name
Get-Service | Where-Object {$_.Name -like '*mysql*'}

# Stop the service (replace MySQL80 with actual name)
Stop-Service MySQL80
```

#### Step 2: Start Docker MySQL

```bash
docker-compose up -d
```

#### Step 3: Wait for MySQL to be ready

```bash
docker-compose logs -f mysql
# Wait for "ready for connections" message
```

### Option 3: Change Docker MySQL Port

If you want to keep both running:

#### Edit docker-compose.yml

```yaml
services:
  mysql:
    ports:
      - "3307:3306"  # Changed from 3306:3306
```

#### Update application-dev.properties

```properties
spring.datasource.url=jdbc:mysql://localhost:3307/docflow?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true
```

#### Start Docker MySQL

```bash
docker-compose up -d
```

## Testing the Connection

### Using MySQL Workbench (if installed)

1. Open MySQL Workbench
2. Create new connection
3. Host: `localhost`, Port: `3306`
4. Username: `root` (or `docflow_user`)
5. Password: Your password
6. Test Connection

### Using Command Line

```bash
# Navigate to MySQL bin directory
cd "C:\Program Files\MySQL\MySQL Server 8.0\bin"

# Connect
mysql -u root -p

# Or if password is 'pass'
mysql -u root -ppass
```

### Using Adminer (Already Running)

You have Adminer running on port 8081:

1. Open browser: http://localhost:8081
2. System: MySQL
3. Server: host.docker.internal (or localhost if not using Docker)
4. Username: root
5. Password: Your password
6. Database: docflow

## Common Errors and Fixes

### Error: Access denied for user 'root'@'localhost'

**Fix**: Wrong password. Find your actual MySQL root password.

### Error: Unknown database 'docflow'

**Fix**: Database doesn't exist. Create it:

```sql
CREATE DATABASE docflow CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### Error: Public Key Retrieval is not allowed

**Fix**: Already handled in connection URL with `allowPublicKeyRetrieval=true`

### Error: Communications link failure

**Fix**: MySQL is not running or wrong port.

## Recommended Approach

Since you have MySQL already running on port 3306:

1. **Find your MySQL root password** (check installation notes, config files, or password manager)
2. **Connect using Adminer** at http://localhost:8081
3. **Create the `docflow` database**
4. **Update `application-dev.properties`** with correct password
5. **Run the application**

## Quick Test

Try connecting with Adminer first:

1. Open: http://localhost:8081
2. Try these combinations:
   - Username: `root`, Password: `pass`
   - Username: `root`, Password: `root`
   - Username: `root`, Password: (empty)
   - Username: `root`, Password: `password`

Once you find the correct password, update `application-dev.properties` and restart the application.

## Next Steps

After fixing the connection:

```bash
# Clean and rebuild
.\gradlew.bat clean build -x test

# Run application
.\gradlew.bat bootRun
```

The application will:
1. Connect to MySQL
2. Run Flyway migrations (create all tables)
3. Insert seed data
4. Start successfully on port 8080
