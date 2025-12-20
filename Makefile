# Makefile helper targets for local development
.PHONY: test-integration test-integration-clean

test-integration:
	./scripts/run-integration-tests.sh

test-integration-clean:
	CLEAN=true ./scripts/run-integration-tests.sh
