import React, { useState, useEffect, useRef } from 'react';
import axios from 'axios';
import io from 'socket.io-client';

function Chatbot({ projectId, apiUrl }) {
  const [messages, setMessages] = useState([]);
  const [inputValue, setInputValue] = useState('');
  const [socket, setSocket] = useState(null);
  const [isConnected, setIsConnected] = useState(false);
  const [loading, setLoading] = useState(true);
  const [sending, setSending] = useState(false);
  const messagesEndRef = useRef(null);

  useEffect(() => {
    // Chatbot-service is exposed on :8007; strip the gateway port and swap in chatbot port.
    // apiUrl = 'http://localhost:8000' → socketUrl = 'http://localhost:8007'
    const socketUrl = apiUrl.replace(':8000', ':8007');
    const newSocket = io(socketUrl, { timeout: 5000 });

    // Give the socket 6 s to connect before falling back to REST-only mode
    const connectTimeout = setTimeout(() => {
      if (!newSocket.connected) {
        setLoading(false);
        setIsConnected(false);
      }
    }, 6000);

    newSocket.on('connect', () => {
      clearTimeout(connectTimeout);
      setIsConnected(true);
      setLoading(false);
      newSocket.emit('join-project', projectId);
    });

    newSocket.on('connect_error', () => {
      clearTimeout(connectTimeout);
      setLoading(false);
      setIsConnected(false);
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
      clearTimeout(connectTimeout);
      newSocket.close();
    };
  }, [projectId, apiUrl]);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  const handleSendMessage = async (e) => {
    e.preventDefault();
    if (!inputValue.trim() || sending) return;

    const userMessage = inputValue;
    setInputValue('');
    setSending(true);

    if (socket && isConnected) {
      socket.emit('message', {
        projectId,
        message: userMessage,
        userId: 'user'
      });
      setSending(false);
    } else {
      // REST fallback when socket is unavailable
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
        setMessages(prev => [...prev, {
          timestamp: new Date().toISOString(),
          userMessage,
          botResponse: 'Failed to send message. Please try again.'
        }]);
      } finally {
        setSending(false);
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
      <div style={{ padding: '14px 20px', borderBottom: '1px solid #e2e8f0', display: 'flex', alignItems: 'center', justifyContent: 'space-between', background: '#fff' }}>
        <span style={{ fontWeight: 600, fontSize: '0.95rem', color: '#1e293b' }}>Project Assistant</span>
        <span style={{ fontSize: '0.78rem', color: isConnected ? '#10b981' : '#64748b', fontWeight: 500, display: 'flex', alignItems: 'center', gap: '5px' }}>
          <span style={{ width: 7, height: 7, borderRadius: '50%', background: isConnected ? '#10b981' : '#94a3b8', display: 'inline-block' }} />
          {isConnected ? 'Real-time' : 'REST mode'}
        </span>
      </div>

      <div className="chat-messages">
        {messages.length === 0 && (
          <div style={{ textAlign: 'center', color: '#94a3b8', paddingTop: '60px' }}>
            <p style={{ fontSize: '1rem', fontWeight: 500, marginBottom: '6px' }}>No messages yet</p>
            <p style={{ fontSize: '0.85rem' }}>
              Ask about your requirements, architecture, or quality analysis.
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
          disabled={sending}
        />
        <button type="submit" disabled={sending || !inputValue.trim()}>
          {sending ? 'Sending…' : 'Send'}
        </button>
      </form>
    </div>
  );
}

export default Chatbot;
