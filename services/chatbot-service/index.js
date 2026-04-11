const express = require('express');
const http = require('http');
const socketIo = require('socket.io');
const { MongoClient } = require('mongodb');
const redis = require('redis');
const cors = require('cors');

const app = express();
const server = http.createServer(app);
const io = socketIo(server, {
  cors: { origin: '*' }
});

app.use(cors());
app.use(express.json());

const MONGO_URL = process.env.MONGO_URL || 'mongodb://localhost:27017';
const REDIS_URL = process.env.REDIS_URL || 'redis://localhost:6379';
const LLM_SERVICE_URL = process.env.LLM_SERVICE_URL || 'http://llm-service:8002';

let mongoClient, chatCollection, redisClient;

async function initConnections() {
  try {
    // MongoDB
    mongoClient = new MongoClient(MONGO_URL);
    await mongoClient.connect();
    const db = mongoClient.db('raag_chatbot');
    chatCollection = db.collection('conversations');
    console.log('✓ MongoDB connected');

    // Redis
    redisClient = redis.createClient({ url: REDIS_URL });
    redisClient.on('error', err => console.error('Redis error:', err));
    await redisClient.connect();
    console.log('✓ Redis connected');
  } catch (err) {
    console.error('Connection error:', err);
    setTimeout(initConnections, 5000);
  }
}

// Socket.io connection handling
io.on('connection', (socket) => {
  console.log(`User connected: ${socket.id}`);

  socket.on('join-project', async (projectId) => {
    socket.join(`project-${projectId}`);
    
    try {
      // Load chat history from Redis
      const history = await redisClient.get(`chat-history:${projectId}`);
      if (history) {
        socket.emit('chat-history', JSON.parse(history));
      }
    } catch (err) {
      console.error('Error loading chat history:', err);
    }
  });

  socket.on('message', async (data) => {
    const { projectId, message, userId } = data;
    
    try {
      // Call LLM service for an intelligent response with project context
      const response = await generateChatResponse(message, projectId);
      
      const chatMessage = {
        timestamp: new Date().toISOString(),
        userId,
        userMessage: message,
        botResponse: response,
        projectId
      };

      // Store in MongoDB
      await chatCollection.insertOne(chatMessage);

      // Cache in Redis
      const key = `chat-history:${projectId}`;
      const history = await redisClient.get(key);
      const messages = history ? JSON.parse(history) : [];
      messages.push(chatMessage);
      await redisClient.setEx(key, 86400, JSON.stringify(messages)); // 24 hour TTL

      // Emit to all users in this project
      io.to(`project-${projectId}`).emit('message-received', {
        timestamp: chatMessage.timestamp,
        userMessage: message,
        botResponse: response
      });
    } catch (err) {
      console.error('Error processing message:', err);
      socket.emit('error', { message: 'Failed to process message' });
    }
  });

  socket.on('disconnect', () => {
    console.log(`User disconnected: ${socket.id}`);
  });
});

async function generateChatResponse(userMessage, projectId) {
  try {
    const body = JSON.stringify({ question: userMessage, project_id: projectId || null });
    const res = await fetch(`${LLM_SERVICE_URL}/chat`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body,
      signal: AbortSignal.timeout(75000)
    });
    if (!res.ok) throw new Error(`LLM service returned ${res.status}`);
    const data = await res.json();
    return data.response || 'Sorry, I could not generate a response.';
  } catch (err) {
    console.error('LLM call failed, using fallback:', err.message);
    return (
      'I can help you understand your project requirements and architecture. ' +
      'Ask me about specific requirements, quality issues, or architectural decisions.'
    );
  }
}

// REST API endpoints
app.get('/chat/:projectId', async (req, res) => {
  try {
    const { projectId } = req.params;
    const messages = await chatCollection
      .find({ projectId })
      .sort({ timestamp: -1 })
      .limit(50)
      .toArray();
    
    res.json(messages.reverse());
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.post('/chat', async (req, res) => {
  try {
    const { projectId, message, userId } = req.body;
    const response = await generateChatResponse(message, projectId);
    
    const chatMessage = {
      timestamp: new Date().toISOString(),
      userId,
      userMessage: message,
      botResponse: response,
      projectId
    };

    await chatCollection.insertOne(chatMessage);
    res.json(chatMessage);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.get('/health', (req, res) => {
  res.json({ 
    status: 'ok', 
    service: 'chatbot',
    llm_service: LLM_SERVICE_URL
  });
});

const PORT = process.env.PORT || 8007;

(async () => {
  await initConnections();
  server.listen(PORT, () => {
    console.log(`🚀 Chatbot Service running on port ${PORT}`);
  });
})();
