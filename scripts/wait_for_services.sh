#!/usr/bin/env bash
set -e
# Wait for app (backend), db (mysql) and mailhog to be available

echo "Waiting for MySQL on port 3306..."
# loop until MySQL accepts connections
until docker exec scut_mysql mysqladmin ping -h localhost --silent; do
  echo "waiting mysql..."; sleep 1
done

echo "Waiting for backend on http://localhost:8081..."
until curl -sS http://localhost:8081/api/health >/dev/null 2>&1; do
  echo "waiting backend..."; sleep 1
done

echo "Waiting for MailHog API..."
until curl -sS http://localhost:8025/api/v2/messages >/dev/null 2>&1; do
  echo "waiting mailhog..."; sleep 1
done

echo "All services appear up"