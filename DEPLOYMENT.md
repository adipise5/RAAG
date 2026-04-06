# 🚀 RAAG Deployment Guide

Production-ready deployment instructions for RAAG v2.0.

## 📦 Deployment Options

### 1. Docker Compose (Single Server)
Best for: MVP, staging, small deployments (<1000 users)

```bash
# On production server
git clone <repo> raag
cd raag
cp .env.example .env
# Edit .env with production values
docker-compose -f docker-compose.yml up -d
```

### 2. Kubernetes (Scalable)
Best for: High-traffic, multi-region, enterprise

```bash
# Install kubectl and connect to cluster
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/secrets.yaml
kubectl apply -f k8s/statefulsets.yaml
kubectl apply -f k8s/deployments.yaml
kubectl apply -f k8s/services.yaml
kubectl apply -f k8s/ingress.yaml
```

### 3. Cloud Platforms

**AWS ECS:**
```bash
# Push images to ECR
aws ecr get-login-password | docker login --username AWS --password-stdin $ECR_REGISTRY
docker tag api-gateway:latest $ECR_REGISTRY/api-gateway:latest
docker push $ECR_REGISTRY/api-gateway:latest

# Update ECS task definitions and services
```

**Google Cloud Run:**
```bash
# Deploy services as Cloud Run services
gcloud run deploy api-gateway --image gcr.io/$PROJECT/api-gateway:latest
```

**Azure Container Instances:**
```bash
# Deploy using Azure Container Instances
az container create --resource-group raag --file docker-compose.yml
```

---

## 🔐 Security Checklist

- [ ] Generate strong random passwords for all databases
- [ ] Enable SSL/TLS for all external connections
- [ ] Use environment variables for all secrets (never hardcode)
- [ ] Implement OAuth2/JWT authentication
- [ ] Enable rate limiting on API Gateway
- [ ] Use WAF (Web Application Firewall)
- [ ] Enable database encryption
- [ ] Set up database backups (automated, tested recovery)
- [ ] Enable audit logging for all API calls
- [ ] Use private networks for inter-service communication
- [ ] Enable CORS with specific allowed origins
- [ ] Use secrets management (Vault, AWS Secrets Manager, Azure Key Vault)
- [ ] Implement input validation on all endpoints
- [ ] Enable database query logging
- [ ] Set up log aggregation and monitoring

---

## 📊 Monitoring Setup

### ELK Stack (Elasticsearch, Logstash, Kibana)
```yaml
services:
  elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch:8.0.0
    environment:
      - discovery.type=single-node
    
  logstash:
    image: docker.elastic.co/logstash/logstash:8.0.0
    
  kibana:
    image: docker.elastic.co/kibana/kibana:8.0.0
    ports:
      - "5601:5601"
```

### Prometheus + Grafana
```yaml
services:
  prometheus:
    image: prom/prometheus
    volumes:
      - ./prometheus.yml:/etc/prometheus/prometheus.yml
    ports:
      - "9090:9090"
  
  grafana:
    image: grafana/grafana
    ports:
      - "3001:3000"
```

### Application Performance Monitoring (APM)
```bash
# New Relic
export NEW_RELIC_APP_NAME="RAAG"
export NEW_RELIC_LICENSE_KEY="your-key"

# DataDog
export DD_SERVICE="raag"
export DD_VERSION="2.0.0"
```

---

## 🗄️ Database Backup Strategy

### MongoDB
```bash
# Backup
mongodump --uri "mongodb://admin:password@mongo:27017/raag" --archive=/backups/raag.archive

# Restore
mongorestore --archive=/backups/raag.archive
```

### PostgreSQL
```bash
# Backup
pg_dump -h postgres -U admin raag > /backups/raag.sql

# Restore
psql -h postgres -U admin raag < /backups/raag.sql

# Automated backup (cron)
0 2 * * * /usr/bin/pg_dump -h postgres -U admin raag | gzip > /backups/raag-$(date +\%Y\%m\%d).sql.gz
```

### Redis (RDB Snapshots)
```bash
# Automatic: Redis dumps RDB periodically
# Manual backup
docker-compose exec redis redis-cli BGSAVE

# Copy RDB file
docker cp raag_redis_1:/data/dump.rdb ./backups/dump.rdb
```

---

## 🚀 Auto-Scaling Configuration

### Horizontal Pod Autoscaling (Kubernetes)
```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: api-gateway-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: api-gateway
  minReplicas: 2
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
```

### Docker Swarm
```bash
# Create service with auto-scaling
docker service create \
  --replicas 3 \
  --reserve-cpu 0.5 \
  --limit-cpu 1 \
  api-gateway:latest
```

---

## 🔄 CI/CD Pipeline

### GitHub Actions
```yaml
name: RAAG CI/CD

on:
  push:
    branches: [main]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - name: Build images
        run: docker-compose build
      - name: Push to registry
        run: docker push ${{ secrets.REGISTRY }}/raag:latest
      - name: Deploy to production
        run: |
          kubectl set image deployment/raag-app \
            api-gateway=${{ secrets.REGISTRY }}/raag:latest
```

### GitLab CI
```yaml
stages:
  - build
  - test
  - deploy

build:
  stage: build
  script:
    - docker-compose build
    - docker push registry.gitlab.com/raag/services:latest

test:
  stage: test
  script:
    - docker-compose up -d
    - npm run test

deploy:
  stage: deploy
  script:
    - kubectl apply -f k8s/
```

---

## 📈 Performance Optimization

### API Gateway Optimization
```javascript
// Enable compression
app.use(compression());

// Implement caching headers
app.use((req, res, next) => {
  res.set('Cache-Control', 'public, max-age=300');
  next();
});

// Use HTTP/2
// Configure in nginx/reverse proxy
```

### Database Optimization
```sql
-- Create indexes
CREATE INDEX idx_projects_user_id ON projects(user_id);
CREATE INDEX idx_requirements_project_id ON requirements(project_id);
CREATE INDEX idx_audit_timestamp ON audit_logs(timestamp);

-- Enable query caching
SET SESSION query_cache_type = ON;
```

### Redis Caching Strategy
```javascript
// Cache analysis results
const cacheKey = `analysis:${projectId}`;
const cached = await redis.get(cacheKey);
if (cached) return JSON.parse(cached);

// Store with TTL
await redis.setex(cacheKey, 3600, JSON.stringify(result));
```

---

## 🌍 Multi-Region Deployment

### Active-Active Setup (AWS)
```bash
# Region 1 (us-east-1)
aws ecs create-service --region us-east-1 --cluster raag-prod --service-name raag-api

# Region 2 (eu-west-1)
aws ecs create-service --region eu-west-1 --cluster raag-prod --service-name raag-api

# CloudFront distribution for routing
aws cloudfront create-distribution \
  --origins DomainName=api-us-east-1.raag.com,DomainName=api-eu-west-1.raag.com
```

### Database Replication
```bash
# MongoDB replication set
rs.initiate({
  _id: "rs0",
  members: [
    { _id: 0, host: "mongo-1:27017" },
    { _id: 1, host: "mongo-2:27017" },
    { _id: 2, host: "mongo-3:27017" }
  ]
})

# PostgreSQL streaming replication
# Primary: postgresql.conf
# wal_level = replica
# max_wal_senders = 3
```

---

## 💾 Disaster Recovery Plan

### RTO (Recovery Time Objective): 1 hour
### RPO (Recovery Point Objective): 15 minutes

```bash
#!/bin/bash
# Automated backup script

BACKUP_DIR="/backups"
DATE=$(date +%Y%m%d_%H%M%S)

# MongoDB backup
mongodump --archive=$BACKUP_DIR/mongo_$DATE.archive

# PostgreSQL backup
pg_dump -h postgres -U admin raag | gzip > $BACKUP_DIR/postgres_$DATE.sql.gz

# Upload to S3
aws s3 sync $BACKUP_DIR s3://raag-backups/

# Clean old backups (keep 30 days)
find $BACKUP_DIR -mtime +30 -delete
```

### Recovery Procedure
```bash
# 1. Restore databases
mongorestore --archive=/backups/mongo_latest.archive
psql -h postgres -U admin raag < /backups/postgres_latest.sql

# 2. Restart services
docker-compose down
docker-compose up -d

# 3. Verify
curl http://localhost:8000/health
```

---

## 📋 Pre-Launch Checklist

- [ ] Database backups tested and verified
- [ ] SSL/TLS certificates installed
- [ ] Firewall rules configured
- [ ] Environment variables set
- [ ] Monitoring dashboards created
- [ ] Alert thresholds configured
- [ ] Load testing completed (target: 1000 RPS)
- [ ] Security audit completed
- [ ] Disaster recovery plan documented
- [ ] Team trained on deployment
- [ ] Runbooks created for common issues
- [ ] Status page set up
- [ ] Incident response plan defined
- [ ] License checks completed

---

## 🚨 Incident Response

### Service Down
```bash
# 1. Check logs
docker-compose logs -f <service>

# 2. Restart service
docker-compose restart <service>

# 3. If persists, rebuild
docker-compose build --no-cache <service>
docker-compose up -d <service>

# 4. Check health
curl http://localhost:8000/health
```

### Database Connection Issues
```bash
# 1. Verify database is running
docker-compose ps

# 2. Check logs
docker-compose logs mongodb | tail -50

# 3. Test connection
docker-compose exec api-gateway curl mongodb:27017
```

---

## 📞 Support & Escalation

1. **Level 1:** Check logs and health endpoints
2. **Level 2:** Restart affected service
3. **Level 3:** Database integrity checks
4. **Level 4:** Full system rebuild from backups
5. **Level 5:** Involve infrastructure team

Contact: devops@raag-project.com

---

## 🔗 Additional Resources

- [Docker Compose Documentation](https://docs.docker.com/compose/)
- [Kubernetes Deployment Guide](https://kubernetes.io/docs/concepts/workloads/controllers/deployment/)
- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
- [12 Factor App Methodology](https://12factor.net/)

---

**Last Updated:** April 2026  
**Version:** 2.0.0
