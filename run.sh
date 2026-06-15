#!/bin/bash

# Database configurations (Supabase)
export DB_URL="jdbc:postgresql://db.cwmoldkvrpleubesesuv.supabase.co:6543/postgres?prepareThreshold=0"
export DB_USERNAME="postgres"

export DB_PASSWORD="QWmNnncudl8VptRm"

# Server configuration
export PORT=8082

echo "Starting Spring Boot Application..."
echo "Database URL: $DB_URL"
echo "Active Port: $PORT"
echo "URL: http://localhost:8082"
echo "----------------------------------------------------"

./mvnw clean spring-boot:run
