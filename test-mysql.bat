@echo off
echo Testing MySQL Connection...
echo.

REM Try to connect to MySQL
mysql -u root -ppass -e "SELECT 'Connection successful!' AS Status;"

if %ERRORLEVEL% EQU 0 (
    echo.
    echo SUCCESS: MySQL connection works!
    echo.
    echo Creating database if not exists...
    mysql -u root -ppass -e "CREATE DATABASE IF NOT EXISTS docflow CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
    echo.
    echo Checking database...
    mysql -u root -ppass -e "SHOW DATABASES LIKE 'docflow';"
    echo.
    echo Database setup complete!
) else (
    echo.
    echo FAILED: Cannot connect to MySQL
    echo.
    echo Possible issues:
    echo 1. MySQL is not running
    echo 2. Password is incorrect (current: pass)
    echo 3. MySQL is not in PATH
    echo.
    echo Please check:
    echo - MySQL service is running
    echo - Root password is 'pass'
    echo - MySQL bin directory is in PATH
)

pause
