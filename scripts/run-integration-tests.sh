#!/usr/bin/env bash
# Archived: this script moved to /archive/scripts/run-integration-tests.sh
# To run integration tests, refer to the archived copy or run tests manually.

echo "This script has been archived and is intentionally disabled to keep the repository minimal."
echo "See archive/scripts/run-integration-tests.sh for original content." 
exit 0

if [ -z "$NETWORK" ]; then
  echo "Could not find a compose network (looked for: ${CANDIDATES[*]})."
  echo "Run 'docker compose up -d' in the repo root and retry."
  exit 1
fi

echo "Using docker network: $NETWORK"

# If CLEAN=true (or --clean) is passed, drop & recreate DB and re-run init SQL
CLEAN=false
if [ "${1:-}" = "--clean" ] || [ "${CLEAN:-}" = "true" ] || [ "${CLEAN_ENV:-}" = "true" ]; then
  CLEAN=true
fi

# Load .env if present to get MySQL root password and DB name
if [ -f "$ROOT_DIR/.env" ]; then
  # shellcheck disable=SC1091
  set -o allexport
  # shellcheck source=/dev/null
  source "$ROOT_DIR/.env"
  set +o allexport
fi

if [ "$CLEAN" = "true" ]; then
  echo "Cleaning database ${MYSQL_DATABASE:-$DB_NAME} using root credentials..."
  if ! docker ps --format '{{.Names}}' | grep -q '^scut_mysql$'; then
    echo "MySQL container 'scut_mysql' not found. Make sure 'docker compose up -d' has been run." >&2
    exit 1
  fi
  SQLDB=${MYSQL_DATABASE:-$DB_NAME}
  echo "Dropping and recreating database ${SQLDB}..."
  # Use a simple, unambiguous SQL command to drop and create the DB
  docker exec -i scut_mysql mysql -uroot -p"${MYSQL_ROOT_PASSWORD:-}" -e "SET FOREIGN_KEY_CHECKS=0; DROP DATABASE IF EXISTS ${SQLDB}; CREATE DATABASE ${SQLDB} CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci; SET FOREIGN_KEY_CHECKS=1;"
  echo "Applying schema and init scripts into ${SQLDB}..."
  docker exec -i scut_mysql sh -c "exec mysql -uroot -p\"${MYSQL_ROOT_PASSWORD:-}\" ${SQLDB}" < "$ROOT_DIR/db/schema.sql"
  if [ -f "$ROOT_DIR/db/docker-init.sql" ]; then
    docker exec -i scut_mysql sh -c "exec mysql -uroot -p\"${MYSQL_ROOT_PASSWORD:-}\" ${SQLDB}" < "$ROOT_DIR/db/docker-init.sql"
  fi
  if [ -f "$ROOT_DIR/db/seed_products.sql" ]; then
    docker exec -i scut_mysql sh -c "exec mysql -uroot -p\"${MYSQL_ROOT_PASSWORD:-}\" ${SQLDB}" < "$ROOT_DIR/db/seed_products.sql"
  fi
fi

# Run tests inside a Maven container attached to the compose network

docker run --rm \
  -v "$BACKEND_DIR":/workspace -w /workspace \
  -e USE_EXISTING_DB=true \
  -e DB_HOST=db -e DB_PORT=3306 -e DB_NAME=scut_shop \
  -e DB_USER=scut_user -e DB_PASSWORD=123456 \
  --network "$NETWORK" \
  maven:3.9.0-eclipse-temurin-17 \
  mvn -DskipTests=false test -e
