# RAAG v2.0 - Requirement Analysis & Architecture Generator

A **polyglot microservices architecture** demonstrating modern software engineering practices. RAAG analyzes project requirements, generates architecture recommendations, and provides quality insights through an AI-powered platform.

## 🏗️ Architecture Overview

For updated DFD and RTM artifacts, see [`docs/dfd-rtm.md`](docs/dfd-rtm.md).

**10 Microservices across 5 Programming Languages:**

| Service | Language | Framework | Port | Purpose |
|---------|----------|-----------|------|---------|
| API Gateway | Node.js | Express | 8000 | Central request router |
| User Service | Go | Gin | 8008 | User & project management |
| Ingestion | Python | FastAPI | 8001 | Requirement data ingestion |
| LLM Analysis | Python | LangChain | 8002 | AI-powered classification |
| Quality Validator | Go | Gin | 8004 | IEEE 830 quality scoring |
| Architecture Generator | Java | Spring Boot | 8003 | DFD & recommendation engine |
| Audit Trail | Rust | Actix-web | 8005 | High-throughput audit logging |
| Export Service | Python | WeasyPrint | 8006 | PDF report generation |
| Chatbot | Node.js | Socket.io | 8007 | Real-time project chat |
| Frontend | React | React | 3000 | Web UI |

**Infrastructure:**
- MongoDB - Document storage
- PostgreSQL - Structured data
- Redis - Caching & sessions
- RabbitMQ - Async messaging
- Docker Compose - Orchestration

## 🚀 Quick Start

### Prerequisites
- Docker & Docker Compose (v3.8+)
- Git
- 8GB RAM minimum

### Installation

1. **Clone & navigate to project:**
```bash
cd raag-project
```

2. **Create `.env` file in root directory:**
```bash
cat > .env << EOF
GEMINI_API_KEY=your-gemini-api-key-here
COMPOSE_PROJECT_NAME=raag
EOF
```

3. **Start all services:**
```bash
docker-compose up -d
```

This will:
- Build all 10 microservice images
- Start databases (MongoDB, PostgreSQL, Redis, RabbitMQ)
- Launch all services
- Start the React frontend

4. **Wait for services to be ready (~2-3 minutes):**
```bash
docker-compose logs -f api-gateway
```

Look for: `🚀 API Gateway running on port 8000`

5. **Access the application:**
- **Frontend:** http://localhost:3000
- **API Gateway:** http://localhost:8000/health
- **RabbitMQ Management:** http://localhost:15672 (guest/guest)

## 📋 Usage Flow

### 1. Create a Project
- Navigate to "New Project" tab
- Fill in:
  - Project Name
  - Project Description
  - Add Requirements (min 1)
  - Select Proposed Architecture
  - Choose Domain (optional)
- Click "Create Project"

### 2. View Analysis Dashboard
- **Quality Analysis:** IEEE 830 scores, vagueness detection
- **Classifications:** FR/NFR categorization
- **Architecture Recommendations:** Optimal style suggestions
- **Complexity Metrics:** Function point analysis

### 3. Chat with Project Context
- Real-time chat about your project
- Ask about specific requirements
- Get architecture explanations
- Query analysis results

### 4. Export Report
- Download professional PDF with:
  - Project overview
  - All requirements & classifications
  - Quality scores & analysis
  - Architecture recommendations
  - Compliance tracking

## 🔧 Development Setup

### Run Individual Services Locally

**Example: Run LLM Service locally while others use Docker**

```bash
# Terminal 1: Start core infrastructure only
docker-compose up -d mongodb rabbitmq redis

# Terminal 2: Run LLM service
cd services/llm-service
pip install -r requirements.txt
export MONGO_URL=mongodb://admin:password@localhost:27017
export RABBITMQ_URL=amqp://guest:guest@localhost:5672
python main.py
```

### Build Individual Services

```bash
# API Gateway
cd services/api-gateway
npm install
npm start

# User Service (Go)
cd services/user-service
go mod download
go run main.go

# Ingestion Service
cd services/ingestion-service
pip install -r requirements.txt
python main.py
```

## 🔌 Adding Your Gemini API Key

Once you have a [Google Gemini API Key](https://aistudio.google.com/app/apikey):

1. **Update `.env` file:**
```bash
GEMINI_API_KEY=sk_live_your_actual_key_here
```

2. **Restart services:**
```bash
docker-compose down
docker-compose up -d
```

3. **LLM Service will now use real Gemini models** for:
   - Requirement classification
   - Architecture recommendations
   - Gap analysis
   - Quality assessment

## 📊 API Endpoints

### Project Management
```bash
# Create project
POST /projects
{
  "name": "My Project",
  "description": "...",
  "requirements": [{"text": "..."}, ...],
  "proposed_architecture": "Microservices",
  "domain": "E-commerce"
}

# Get project
GET /projects/{project_id}

# Analyze requirements
POST /analyze
{
  "project_id": "...",
  "requirements": ["req1", "req2", ...]
}

# Get analysis results
GET /analysis/{project_id}
```

### Export & Reporting
```bash
# Export to PDF
GET /export/{project_id}

# Preview HTML
GET /export/{project_id}?format=html
```

### Audit & Analytics
```bash
# Get API statistics
GET /audit/stats

# Get audit logs
GET /audit/logs?limit=100&offset=0
```

### Chat
```bash
# Send message (REST)
POST /chat
{
  "projectId": "...",
  "message": "...",
  "userId": "..."
}

# Get chat history
GET /chat/{projectId}
```

## 🧪 Testing

### Health Check All Services
```bash
curl http://localhost:8000/health
```

Response shows all available services.

### Test API Gateway
```bash
curl -X POST http://localhost:8000/projects \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test Project",
    "description": "A test project",
    "requirements": [{"text": "User can login"}],
    "proposed_architecture": "Microservices"
  }'
```

### View Service Logs
```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f llm-service

# Follow only errors
docker-compose logs -f api-gateway 2>&1 | grep error
```

## 🏛️ Architecture Patterns Demonstrated

1. **API Gateway** - Central routing & rate limiting
2. **Polyglot Persistence** - MongoDB, PostgreSQL, Redis
3. **Event-Driven Architecture** - RabbitMQ async messaging
4. **CQRS** - Separate read/write paths
5. **Circuit Breaker** - API Gateway resilience
6. **Saga Pattern** - Multi-step async workflows
7. **Audit Logging** - Immutable event trail
8. **Service Mesh Concepts** - OpenAPI contracts
9. **Database per Service** - Data isolation
10. **Cache-Aside Pattern** - Redis caching

## 📁 Project Structure

```
raag-project/
├── docker-compose.yml          # Orchestration config
├── services/
│   ├── api-gateway/           # Node.js/Express
│   ├── user-service/          # Go/Gin
│   ├── ingestion-service/     # Python/FastAPI
│   ├── llm-service/           # Python/LangChain
│   ├── quality-service/       # Go/Gin
│   ├── architecture-service/  # Java/Spring Boot
│   ├── audit-service/         # Rust/Actix
│   ├── export-service/        # Python/WeasyPrint
│   └── chatbot-service/       # Node.js/Socket.io
├── frontend/
│   ├── public/
│   ├── src/
│   │   ├── components/        # React components
│   │   ├── App.js
│   │   ├── App.css
│   │   └── index.js
│   └── package.json
└── README.md
```

## 🔒 Security Notes

**Development Only:**
- RabbitMQ runs with default guest:guest credentials
- MongoDB/PostgreSQL use basic credentials
- No SSL/TLS in compose setup
- API Gateway has no authentication

**For Production:**
1. Generate strong passwords
2. Enable SSL/TLS
3. Implement OAuth2/JWT auth
4. Use secrets management (Vault, K8s Secrets)
5. Enable database encryption
6. Implement rate limiting per user
7. Add API key authentication

## 🐛 Troubleshooting

### Services won't start
```bash
# Check logs
docker-compose logs <service_name>

# Rebuild images
docker-compose build --no-cache

# Full reset
docker-compose down -v
docker-compose up -d
```

### Port conflicts
If ports are already in use:
```yaml
# Edit docker-compose.yml
ports:
  - "8001:8001"  # Change first number to unused port
```

### Database connection errors
```bash
# Restart databases
docker-compose restart mongodb postgres redis

# Check network
docker network ls
docker inspect raag_raag-network
```

### Frontend not loading
```bash
# Check frontend logs
docker-compose logs frontend

# Verify API_URL
echo $REACT_APP_API_URL
```

## 📈 Performance Tuning

**For load testing:**
```bash
# Install k6
brew install k6

# Run basic load test
k6 run - << EOF
import http from 'k6/http';
import { check } from 'k6';

export default function() {
  let res = http.get('http://localhost:8000/health');
  check(res, {
    'status is 200': (r) => r.status === 200,
  });
}
EOF
```

## 🎓 Learning Resources

- **Microservices:** [Pattern Language](https://microservices.io/)
- **Database Patterns:** [CQRS & Event Sourcing](https://martinfowler.com/bliki/CQRS.html)
- **Polyglot Architecture:** [Choosing the Right Tool](https://www.thoughtworks.com/en-us/radar/platforms)
- **Go Microservices:** [Go Design Patterns](https://github.com/senatorwang/go-design-patterns)
- **Python FastAPI:** [Official Docs](https://fastapi.tiangolo.com/)
- **Rust Safety:** [The Rust Book](https://doc.rust-lang.org/book/)

## 📄 License

MIT License - Feel free to use this project for learning and development.

## 👥 Authors

Prishiv Singh Jamwal | Aditya Ashok Pise | Ansh  
Software Architecture Course Project — February-April 2026

---

## ⭐ Quick Reference

| Task | Command |
|------|---------|
| Start | `docker-compose up -d` |
| Stop | `docker-compose down` |
| Logs | `docker-compose logs -f <service>` |
| Rebuild | `docker-compose up -d --build` |
| Clean reset | `docker-compose down -v && docker-compose up -d` |
| Check health | `curl http://localhost:8000/health` |
| Access frontend | http://localhost:3000 |
| RabbitMQ UI | http://localhost:15672 |

Enjoy building with RAAG! 🚀
