.PHONY: help start stop restart logs clean build health test

help:
	@echo "RAAG v2.0 - Requirement Analysis & Architecture Generator"
	@echo ""
	@echo "Available commands:"
	@echo "  make start       - Start all services"
	@echo "  make stop        - Stop all services"
	@echo "  make restart     - Restart all services"
	@echo "  make build       - Build all service images"
	@echo "  make logs        - View all service logs"
	@echo "  make health      - Check health of all services"
	@echo "  make clean       - Remove all containers and volumes"
	@echo "  make test        - Run basic API tests"
	@echo "  make frontend    - Open frontend in browser"
	@echo "  make docs        - Open documentation"

start:
	@echo "Starting RAAG services..."
	docker-compose up -d
	@echo "✓ Services started"
	@sleep 5
	@make health

stop:
	@echo "Stopping RAAG services..."
	docker-compose down
	@echo "✓ Services stopped"

restart: stop start

build:
	@echo "Building all services..."
	docker-compose build --no-cache
	@echo "✓ Build complete"

logs:
	docker-compose logs -f

logs-gateway:
	docker-compose logs -f api-gateway

logs-llm:
	docker-compose logs -f llm-service

logs-frontend:
	docker-compose logs -f frontend

logs-audit:
	docker-compose logs -f audit-service

health:
	@echo "Checking service health..."
	@curl -s http://localhost:8000/health | python3 -m json.tool || echo "API Gateway not ready"
	@echo ""
	@echo "Service Status:"
	@docker-compose ps

clean:
	@echo "Cleaning up all containers and volumes..."
	docker-compose down -v
	@echo "✓ Cleanup complete"

test:
	@echo "Testing API endpoints..."
	@echo "1. Health check..."
	@curl -s http://localhost:8000/health | python3 -m json.tool
	@echo "\n2. Creating test project..."
	@curl -s -X POST http://localhost:8000/projects \
		-H "Content-Type: application/json" \
		-d '{"name":"Test","description":"Test project","requirements":[{"text":"Test req"}],"proposed_architecture":"Microservices"}' | python3 -m json.tool

frontend:
	@open http://localhost:3000 || xdg-open http://localhost:3000 || echo "Open http://localhost:3000 in your browser"

docs:
	@open README.md || xdg-open README.md || cat README.md

services-status:
	@echo "Checking individual services..."
	@echo ""
	@echo "API Gateway (8000):"
	@curl -s http://localhost:8000/health | python3 -m json.tool || echo "❌ Not responding"
	@echo ""
	@echo "User Service (8008):"
	@curl -s http://localhost:8008/health | python3 -m json.tool || echo "❌ Not responding"
	@echo ""
	@echo "Ingestion Service (8001):"
	@curl -s http://localhost:8001/health | python3 -m json.tool || echo "❌ Not responding"
	@echo ""
	@echo "LLM Service (8002):"
	@curl -s http://localhost:8002/health | python3 -m json.tool || echo "❌ Not responding"
	@echo ""
	@echo "Quality Service (8004):"
	@curl -s http://localhost:8004/health | python3 -m json.tool || echo "❌ Not responding"
	@echo ""
	@echo "Architecture Service (8003):"
	@curl -s http://localhost:8003/health | python3 -m json.tool || echo "❌ Not responding"
	@echo ""
	@echo "Audit Service (8005):"
	@curl -s http://localhost:8005/health | python3 -m json.tool || echo "❌ Not responding"
	@echo ""
	@echo "Export Service (8006):"
	@curl -s http://localhost:8006/health | python3 -m json.tool || echo "❌ Not responding"
	@echo ""
	@echo "Chatbot Service (8007):"
	@curl -s http://localhost:8007/health | python3 -m json.tool || echo "❌ Not responding"

shell-gateway:
	docker-compose exec api-gateway sh

shell-mongodb:
	docker-compose exec mongodb mongo -u admin -p password --authenticationDatabase admin

shell-postgres:
	docker-compose exec postgres psql -U admin -d raag

shell-redis:
	docker-compose exec redis redis-cli

rabbitmq-ui:
	@open http://localhost:15672 || xdg-open http://localhost:15672 || echo "Open http://localhost:15672 in your browser"

env-setup:
	@if [ ! -f .env ]; then \
		echo "Creating .env file from .env.example..."; \
		cp .env.example .env; \
		echo "✓ .env created. Please edit it with your settings"; \
	else \
		echo ".env already exists"; \
	fi

install-deps:
	@echo "Installing system dependencies..."
	@which docker >/dev/null 2>&1 || echo "Please install Docker: https://docs.docker.com/get-docker/"
	@which docker-compose >/dev/null 2>&1 || echo "Please install Docker Compose: https://docs.docker.com/compose/install/"

.DEFAULT_GOAL := help
