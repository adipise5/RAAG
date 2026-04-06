# 🚀 RAAG Quick Start - 5 Minutes

Get RAAG running in just **5 minutes**.

## ⚡ Ultra-Quick Start

```bash
# 1. Navigate to project
cd raag-project

# 2. Create environment file
cp .env.example .env

# 3. Start all services
docker-compose up -d

# 4. Wait ~2 minutes, then open browser
# Frontend: http://localhost:3000
```

**Done!** Your RAAG instance is running. ✨

---

## 📋 Step-by-Step

### Step 1: Prerequisites Check
```bash
docker --version        # Should show Docker version 20+
docker-compose --version  # Should show 1.29+
```

**Don't have Docker?**
- [Install Docker Desktop](https://www.docker.com/products/docker-desktop)

### Step 2: Setup
```bash
# Clone/download the project
cd raag-project

# Copy environment template
cp .env.example .env

# (Optional) Add Gemini API key for AI features
# Edit .env and add: GEMINI_API_KEY=your_key_here
```

### Step 3: Start Services
```bash
# Start everything
docker-compose up -d

# Watch the startup (takes ~2-3 minutes)
docker-compose logs -f api-gateway
```

When you see:
```
🚀 API Gateway running on port 8000
```

You're ready! ✅

### Step 4: Access the Application

| Service | URL |
|---------|-----|
| **Web UI** | http://localhost:3000 |
| **API** | http://localhost:8000 |
| **RabbitMQ** | http://localhost:15672 (guest/guest) |

---

## 🎯 First Project in 2 Minutes

1. **Open** http://localhost:3000
2. **Click** "New Project"
3. **Fill in:**
   - Name: "My Test Project"
   - Description: "A project to analyze requirements"
   - Requirements (add 3):
     - "Users can login with email and password"
     - "System must handle 1000 concurrent users"
     - "All data must be encrypted in transit"
   - Architecture: "Microservices"
4. **Click** "Create Project"
5. **View** the analysis dashboard with quality scores, classifications, and recommendations

---

## 🛠️ Common Commands

```bash
# View logs
docker-compose logs -f

# Check if all services are running
docker-compose ps

# Stop services
docker-compose down

# Restart a service
docker-compose restart api-gateway

# Full reset (dangerous - deletes data)
docker-compose down -v && docker-compose up -d
```

---

## 🔧 Troubleshooting

### "Connection refused" error
```bash
# Services need time to start. Wait another minute and check:
curl http://localhost:8000/health
```

### "Port already in use"
```bash
# Find what's using port 8000
lsof -i :8000

# Edit docker-compose.yml to use different ports
# Change: "8000:8000" to "8001:8000"
```

### "Out of disk space"
```bash
# Docker images are large. Check available space:
df -h

# Need 10GB+ free space
```

---

## 🚀 Next Steps

1. **[Read Full README](./README.md)** - Complete documentation
2. **[Add Gemini API Key](./README.md#-adding-your-gemini-api-key)** - Enable AI features
3. **[Explore API Endpoints](./README.md#-api-endpoints)** - Test REST API
4. **[View Architecture](./README.md#-architecture-overview)** - Understand the system

---

## 📞 Support

**Services not starting?**
```bash
# View detailed logs
docker-compose logs llm-service
docker-compose logs api-gateway

# Rebuild from scratch
docker-compose build --no-cache
docker-compose up -d
```

**Need help?**
- Check [README.md](./README.md) troubleshooting section
- View service logs: `docker-compose logs -f <service_name>`
- Check if ports are available: `lsof -i :<port>`

---

## ✨ Features at a Glance

✅ **10 Microservices** - Production-grade polyglot architecture  
✅ **AI-Powered** - LLM integration ready (Gemini, local Ollama)  
✅ **Real-time Chat** - WebSocket-based chatbot  
✅ **PDF Export** - Professional report generation  
✅ **Audit Trail** - High-performance event logging  
✅ **Quality Analysis** - IEEE 830 requirement scoring  
✅ **Architecture Recommendations** - AI-driven suggestions  
✅ **Docker Compose** - One-command deployment  

---

## 🎓 Learning Outcomes

By running RAAG, you'll understand:
- ✅ Microservices architecture patterns
- ✅ Polyglot programming (Python, Node.js, Go, Java, Rust)
- ✅ Async messaging with RabbitMQ
- ✅ Multiple database types (MongoDB, PostgreSQL, Redis)
- ✅ Real-time WebSocket communication
- ✅ Container orchestration with Docker Compose
- ✅ API Gateway pattern
- ✅ Event-driven architecture

---

Enjoy! 🎉
