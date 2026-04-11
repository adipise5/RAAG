const express = require('express');
const httpProxy = require('http-proxy');
const cors = require('cors');
const amqp = require('amqplib');
const redis = require('redis');
const { randomUUID } = require('crypto');

const app = express();
const PORT = process.env.PORT || 8000;

app.use(cors());
app.use(express.json());

// Attach a unique request ID to every request so the full call chain
// across services can be correlated in logs.
app.use((req, _res, next) => {
  req.requestId = req.headers['x-request-id'] || randomUUID();
  next();
});

let connection, channel, redisClient;

// Service routes
const services = {
  users: 'http://user-service:8008',
  ingestion: 'http://ingestion-service:8001',
  llm: 'http://llm-service:8002',
  quality: 'http://quality-service:8004',
  architecture: 'http://architecture-service:8003',
  audit: 'http://audit-service:8005',
  export: 'http://export-service:8006',
  chatbot: 'http://chatbot-service:8007'
};

const proxies = {};
Object.entries(services).forEach(([name, target]) => {
  // export: WeasyPrint can be slow (120 s); llm/chatbot: Gemini ~8 s + Ollama ~60 s (90 s); architecture: parallel LLM calls ~15 s max (60 s margin)
  const timeout = name === 'export' ? 120000 : (name === 'llm' || name === 'chatbot') ? 90000 : name === 'architecture' ? 60000 : 30000;
  proxies[name] = httpProxy.createProxyServer({ target, timeout, proxyTimeout: timeout });
});

// Forward JSON body explicitly for proxied requests after express.json() consumes the stream.
// Also forward the correlation ID to every upstream service.
Object.values(proxies).forEach((proxy) => {
  proxy.on('proxyReq', (proxyReq, req) => {
    // Always propagate the correlation ID
    proxyReq.setHeader('X-Request-ID', req.requestId);

    if (!['POST', 'PUT', 'PATCH', 'DELETE'].includes(req.method)) return;
    if (typeof req.body === 'undefined') return;

    const bodyData = JSON.stringify(req.body);
    proxyReq.setHeader('Content-Type', 'application/json');
    proxyReq.setHeader('Content-Length', Buffer.byteLength(bodyData));
    proxyReq.write(bodyData);
  });

  proxy.on('error', (err, req, res) => {
    console.error(`[${req.requestId}] Proxy error:`, err.message);
    if (!res.headersSent) {
      res.status(502).json({
        error: 'Bad Gateway',
        message: 'Upstream service is unavailable',
        requestId: req.requestId
      });
    }
  });
});

// Initialize RabbitMQ
async function initRabbitMQ() {
  try {
    connection = await amqp.connect(process.env.RABBITMQ_URL);
    channel = await connection.createChannel();
    await channel.assertExchange('audit', 'fanout', { durable: true });
    console.log('✓ RabbitMQ connected');
  } catch (err) {
    console.error('RabbitMQ error:', err);
    setTimeout(initRabbitMQ, 5000);
  }
}

// Initialize Redis
async function initRedis() {
  try {
    redisClient = redis.createClient({ url: process.env.REDIS_URL });
    redisClient.on('error', err => console.error('Redis error:', err));
    await redisClient.connect();
    console.log('✓ Redis connected');
  } catch (err) {
    console.error('Redis error:', err);
    setTimeout(initRedis, 5000);
  }
}

// Audit event emitter
async function emitAuditEvent(req, res, next) {
  const startTime = Date.now();

  // Echo the request ID back to the caller so they can reference it
  res.set('X-Request-ID', req.requestId);
  
  res.on('finish', async () => {
    try {
      const auditEvent = {
        requestId: req.requestId,
        timestamp: new Date().toISOString(),
        method: req.method,
        path: req.path,
        service: req.path.split('/')[1],
        status: res.statusCode,
        latency: Date.now() - startTime,
        ip: req.ip
      };

      console.log(`[${req.requestId}] ${req.method} ${req.path} -> ${res.statusCode} (${Date.now() - startTime}ms)`);
      
      if (channel) {
        await channel.publish('audit', '', Buffer.from(JSON.stringify(auditEvent)));
      }
    } catch (err) {
      console.error('Audit emit error:', err);
    }
  });
  
  next();
}

app.use(emitAuditEvent);

// Routes
app.post('/users/register', (req, res) => proxies.users.web(req, res));
app.post('/users/login', (req, res) => proxies.users.web(req, res));
app.get('/users/:id', (req, res) => proxies.users.web(req, res));

app.post('/projects', (req, res) => proxies.ingestion.web(req, res));
app.get('/projects/:id', (req, res) => proxies.ingestion.web(req, res));
app.post('/projects/:id/requirements', (req, res) => proxies.ingestion.web(req, res));

app.post('/analyze', (req, res) => proxies.llm.web(req, res));
app.get('/analysis/:id', (req, res) => proxies.llm.web(req, res));
app.post('/quality/enhanced', (req, res) => proxies.llm.web(req, res));
app.post('/gap-analysis', (req, res) => proxies.llm.web(req, res));
app.post('/traceability-matrix', (req, res) => proxies.llm.web(req, res));
app.post('/risk-assumptions', (req, res) => proxies.llm.web(req, res));
app.post('/complexity-estimation', (req, res) => proxies.llm.web(req, res));
app.post('/novelty-assessment', (req, res) => proxies.llm.web(req, res));
app.post('/rewrite-requirement', (req, res) => proxies.llm.web(req, res));

app.post('/quality-check', (req, res) => proxies.quality.web(req, res));
app.get('/quality/:id', (req, res) => proxies.quality.web(req, res));

app.post('/generate-architecture', (req, res) => proxies.architecture.web(req, res));
app.get('/architecture/:id', (req, res) => proxies.architecture.web(req, res));

app.get('/audit/stats', (req, res) => proxies.audit.web(req, res));
app.get('/audit/logs', (req, res) => proxies.audit.web(req, res));

app.post('/export/:id', (req, res) => proxies.export.web(req, res));
app.get('/export/:id', (req, res) => proxies.export.web(req, res));

app.post('/chat', (req, res) => proxies.chatbot.web(req, res));
app.get('/chat/:projectId', (req, res) => proxies.chatbot.web(req, res));

// Diagram generation: LLM generates PlantUML code, architecture service renders it to SVG
app.post('/generate-diagram', (req, res) => proxies.llm.web(req, res));
app.post('/render-diagram', (req, res) => proxies.architecture.web(req, res));

// Root route
app.get('/', (req, res) => {
  res.json({
    service: 'RAAG API Gateway',
    status: 'ok',
    health: '/health',
    frontend: 'http://localhost:3000'
  });
});

// Health check
app.get('/health', (req, res) => {
  res.json({ 
    status: 'ok',
    timestamp: new Date().toISOString(),
    services
  });
});

async function start() {
  await initRabbitMQ();
  await initRedis();
  
  app.listen(PORT, () => {
    console.log(`🚀 API Gateway running on port ${PORT}`);
  });
}

start().catch(err => {
  console.error('Fatal error:', err);
  process.exit(1);
});
