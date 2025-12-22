# Minimal Makefile: keep things simple and use docker compose up as the single command.
.PHONY: seed dev

# seed: import example product seeds into the running database
seed:
	@echo "Seeding database (scut_db) with example products..."
	@docker exec -i scut_db sh -c 'mysql -uroot -p"${MYSQL_ROOT_PASSWORD:-123456789}" scut_shop' < db/seed_products.sql || echo "Seeding failed; ensure scut_db is running"

# Dev: one-command local development (uses docker-compose.dev.yml to mount source and run dev servers)
dev:
	@echo "Starting development environment (backend mvn spring-boot:run, frontend npm run dev)"
	@docker compose -f docker-compose.dev.yml up --build

# Note: integration test scripts and registry setup were archived to /archive to keep the demo minimal.

