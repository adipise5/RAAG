#!/bin/bash

# RAAG Project Startup Script
# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}================================${NC}"
echo -e "${GREEN}🚀 RAAG v2.0 - Starting Services${NC}"
echo -e "${GREEN}================================${NC}\n"

# Check if .env exists
if [ ! -f .env ]; then
    echo -e "${YELLOW}⚠️  .env file not found${NC}"
    echo "Creating .env from .env.example..."
    cp .env.example .env
    echo -e "${GREEN}✓ .env created (no API keys required - uses local Ollama LLM)${NC}\n"
fi

# Check Docker
if ! command -v docker &> /dev/null; then
    echo -e "${RED}❌ Docker is not installed${NC}"
    exit 1
fi

echo -e "${GREEN}✓ Docker found${NC}"

# Check Docker Compose
if ! command -v docker-compose &> /dev/null; then
    echo -e "${RED}❌ Docker Compose is not installed${NC}"
    exit 1
fi

echo -e "${GREEN}✓ Docker Compose found${NC}\n"

# Pull latest images
echo -e "${YELLOW}📦 Pulling base images...${NC}"
docker pull mongo:7.0
docker pull postgres:15
docker pull redis:7-alpine
docker pull rabbitmq:3.12-management
docker pull node:18-alpine
docker pull python:3.11-slim
docker pull golang:1.21-alpine
docker pull eclipse-temurin:21-jre-alpine
docker pull rust:latest
echo -e "${GREEN}✓ Images pulled${NC}\n"

# Build and start services
echo -e "${YELLOW}🔨 Building and starting services...${NC}"
docker-compose up -d

# Wait for services to be ready
echo -e "${YELLOW}⏳ Waiting for services to start (this may take 2-3 minutes)...${NC}\n"

# Check API Gateway health
max_attempts=30
attempt=0

while [ $attempt -lt $max_attempts ]; do
    if curl -s http://localhost:8000/health > /dev/null 2>&1; then
        echo -e "${GREEN}✓ API Gateway is ready${NC}"
        break
    fi
    attempt=$((attempt + 1))
    echo "Checking... ($attempt/$max_attempts)"
    sleep 5
done

if [ $attempt -eq $max_attempts ]; then
    echo -e "${RED}❌ Services failed to start${NC}"
    echo "Check logs with: docker-compose logs"
    exit 1
fi

# Display service information
echo -e "\n${GREEN}================================${NC}"
echo -e "${GREEN}✅ All services are running!${NC}"
echo -e "${GREEN}================================${NC}\n"

echo -e "${YELLOW}📋 Service URLs:${NC}"
echo "  Frontend:           http://localhost:3000"
echo "  API Gateway:        http://localhost:8000"
echo "  RabbitMQ UI:        http://localhost:15672 (guest/guest)"
echo ""

echo -e "${YELLOW}📊 Service Ports:${NC}"
echo "  API Gateway:        8000"
echo "  User Service:       8008"
echo "  Ingestion:          8001"
echo "  LLM Analysis:       8002"
echo "  Architecture:       8003"
echo "  Quality Validator:  8004"
echo "  Audit Trail:        8005"
echo "  Export Service:     8006"
echo "  Chatbot:            8007"
echo ""

echo -e "${YELLOW}🔗 Useful Commands:${NC}"
echo "  View all logs:      docker-compose logs -f"
echo "  View service logs:  docker-compose logs -f <service_name>"
echo "  Stop services:      docker-compose down"
echo "  Full reset:         docker-compose down -v"
echo "  Rebuild services:   docker-compose up -d --build"
echo ""

echo -e "${GREEN}🎉 Ready to use! Open http://localhost:3000 in your browser${NC}\n"
