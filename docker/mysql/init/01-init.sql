-- Initial database setup for Docflow
-- This script runs automatically when the MySQL container starts for the first time
-- Flyway will handle all schema creation, so this file is intentionally minimal

-- Grant privileges to the docflow_user
GRANT ALL PRIVILEGES ON docflow.* TO 'docflow_user'@'%';
FLUSH PRIVILEGES;
