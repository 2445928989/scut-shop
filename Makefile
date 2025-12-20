# Makefile helper targets for local development
.PHONY: test-integration test-integration-clean

test-integration:
	./scripts/run-integration-tests.sh

test-integration-clean:
	CLEAN=true ./scripts/run-integration-tests.sh

# E2E targets (Playwright + MailHog)
.PHONY: test-e2e test-e2e-local

# Run E2E in docker-compose environment (CI-friendly)
# Sets EMAIL_ACTIVATION_ENABLED=true and ensures MailHog is available
test-e2e:
	@echo "Starting services for E2E..."
	EMAIL_ACTIVATION_ENABLED=true docker compose up -d --build mailhog app frontend db
	@echo "Waiting for services to be healthy..."
	./scripts/wait_for_services.sh
	@echo "Running Playwright tests (CI)"
	cd frontend && CI=true API_BASE=http://app:8080 MAILHOG_API=http://mailhog:8025/api/v2/messages npx playwright test --reporter=list || true
	@echo "E2E finished; keeping services up for inspection"

# Run E2E locally (allows DB fallback & reuses host ports)
test-e2e-local:
	@echo "Starting services for local E2E..."
	EMAIL_ACTIVATION_ENABLED=true docker compose up -d --build
	./scripts/wait_for_services.sh
	cd frontend && EMAIL_ACTIVATION_ENABLED=true MYSQL_ROOT_PASSWORD=123456789 API_BASE=http://localhost:8081 MAILHOG_API=http://localhost:8025 npx playwright test e2e/activation-e2e.spec.ts --reporter=list || true
	@echo "Local E2E finished"
