import React, { useState, useEffect, useRef } from 'react';
import axios from 'axios';
import io from 'socket.io-client';

function Chatbot({ projectId, apiUrl }) {
  const [messages, setMessages] = useState([]);
  const [inputValue, setInputValue] = useState('');
  const [socket, setSocket] = useState(null);
  const [isConnected, setIsConnected] = useState(false);
  const [loading, setLoading] = useState(true);
  const messagesEndRef = useRef(null);

  useEffect(() => {
    // Initialize Socket.io connection
    const socketUrl = apiUrl.replace('/api', '').replace(':8000', ':8007');
    const newSocket = io(socketUrl);

    newSocket.on('connect', () => {
      setIsConnected(true);
      setLoading(false);
      newSocket.emit('join-project', projectId);
    });

    newSocket.on('chat-history', (history) => {
      setMessages(history);
    });

    newSocket.on('message-received', (data) => {
      setMessages(prev => [...prev, {
        timestamp: data.timestamp,
        userMessage: data.userMessage,
        botResponse: data.botResponse
      }]);
    });

    newSocket.on('disconnect', () => {
      setIsConnected(false);
    });

    setSocket(newSocket);

    return () => {
      newSocket.close();
    };
  }, [projectId, apiUrl]);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  const handleSendMessage = async (e) => {
    e.preventDefault();
    if (!inputValue.trim()) return;

    const userMessage = inputValue;
    setInputValue('');

    if (socket && isConnected) {
      socket.emit('message', {
        projectId,
        message: userMessage,
        userId: 'user'
      });
    } else {
      // Fallback to REST API
      try {
        const response = await axios.post(`${apiUrl}/chat`, {
          projectId,
          message: userMessage,
          userId: 'user'
        });
        
        setMessages(prev => [...prev, {
          timestamp: response.data.timestamp,
          userMessage: response.data.userMessage,
          botResponse: response.data.botResponse
        }]);
      } catch (err) {
        console.error('Error sending message:', err);
      }
    }
  };

  if (loading) {
    return (
      <div className="loading">
        <div className="spinner"></div>
        <p>Connecting chatbot...</p>
      </div>
    );
  }

  return (
    <div className="chatbot-container">
      <div className="chat-messages">
        {messages.length === 0 && (
          <div style={{ textAlign: 'center', color: '#999', paddingTop: '40px' }}>
            <p>No messages yet. Start by asking about your project!</p>
            <p style={{ fontSize: '0.9rem', marginTop: '10px' }}>
              {isConnected ? '✓ Connected' : '⚠ Connecting...'}
            </p>
          </div>
        )}
        
        {messages.map((msg, idx) => (
          <div key={idx}>
            <div className="message user">
              <div className="message-content">
                {msg.userMessage}
              </div>
            </div>
            <div className="message bot">
              <div className="message-content">
                {msg.botResponse}
              </div>
            </div>
          </div>
        ))}
        
        <div ref={messagesEndRef} />
      </div>

      <form className="chat-input" onSubmit={handleSendMessage}>
        <input
          type="text"
          value={inputValue}
          onChange={(e) => setInputValue(e.target.value)}
          placeholder="Ask about your requirements, architecture, or quality analysis..."
          disabled={!isConnected}
        />
        <button type="submit" disabled={!isConnected || !inputValue.trim()}>
          Send
        </button>
      </form>
    </div>
  );
}

export default Chatbot;
