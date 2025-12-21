# Makefile helper targets for local development
.PHONY: test-integration test-integration-clean

test-integration:
	./scripts/run-integration-tests.sh

test-integration-clean:
	CLEAN=true ./scripts/run-integration-tests.sh

# E2E targets were removed in the simplified demo branch.
# To run full E2E, re-enable Playwright and the test targets in a non-demo branch.

# Registry/advanced build targets removed for simplified demo.
# Use the default docker compose or Make dev for local development.


# Dev: one-command local development (uses docker-compose.dev.yml to mount source and run dev servers)
.PHONY: dev
dev:
	@echo "Starting development environment (backend mvn spring-boot:run, frontend npm run dev)"
	@docker compose -f docker-compose.dev.yml up --build
