#!/bin/bash
set -e
USER="act_noexcl_${RANDOM}_$(date +%s)"
EMAIL="${USER}@example.test"
PASSWORD="Password123"
echo "Registering $USER $EMAIL"
REG=$(curl -s -X POST "http://localhost:3000/api/auth/register" -H "Content-Type: application/json" -d "{\"username\":\"$USER\",\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}")
echo "Register response: $REG"
# poll MailHog
TOKEN=""
for i in $(seq 1 20); do
  echo "poll $i"
  DATA=$(curl -s "http://localhost:8025/api/v2/messages" || true)
  if echo "$DATA" | grep -q "$EMAIL"; then
    echo "Found message entry"
    TOKEN=$(echo "$DATA" | sed -n "/$EMAIL/,+200p" | sed -n 's/.*activate?token=\([A-Za-z0-9-]*\).*/\1/p' | head -n1)
    if [ -n "$TOKEN" ]; then break; fi
  fi
  sleep 1
done

echo "Token: $TOKEN"
if [ -z "$TOKEN" ]; then echo "No token found"; exit 1; fi
# activate
ACT=$(curl -s -X GET "http://localhost:8081/api/auth/activate?token=$TOKEN")
echo "Activate response: $ACT"
# login
LOGIN=$(curl -s -X POST "http://localhost:8081/api/auth/login" -H "Content-Type: application/json" -d "{\"username\":\"$USER\",\"password\":\"$PASSWORD\"}")
echo "Login response: $LOGIN"
echo SUCCESS
