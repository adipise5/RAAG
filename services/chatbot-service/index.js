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
const GEMINI_API_KEY = process.env.GEMINI_API_KEY || '';

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
      // Mock response (in production, call Gemini API)
      const response = await generateChatResponse(message);
      
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

async function generateChatResponse(userMessage) {
  const messageLower = userMessage.toLowerCase();
  
  // Mock responses based on keywords
  const responses = {
    'requirement': 'Requirements are functional and non-functional specifications that define what the system should do.',
    'architecture': 'Architecture refers to the high-level structure of your system, including how components interact.',
    'quality': 'Quality scores are based on IEEE 830 standards, measuring specificity and completeness of requirements.',
    'vague': 'Vague requirements contain ambiguous terms like "fast", "easy", or "good". Try to be more specific.',
    'design': 'The system architecture helps you understand how different components work together.',
    'default': 'I can help you understand your requirements, architecture, and quality analysis. What would you like to know?'
  };

  for (const [key, response] of Object.entries(responses)) {
    if (messageLower.includes(key)) {
      return response;
    }
  }

  return responses.default;
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
    const response = await generateChatResponse(message);
    
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
    gemini_configured: !!GEMINI_API_KEY
  });
});

const PORT = process.env.PORT || 8007;

(async () => {
  await initConnections();
  server.listen(PORT, () => {
    console.log(`🚀 Chatbot Service running on port ${PORT}`);
  });
})();
