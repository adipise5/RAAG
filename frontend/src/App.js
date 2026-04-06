import React, { useState } from 'react';
import axios from 'axios';
import './App.css';
import ProjectForm from './components/ProjectForm';
import Dashboard from './components/Dashboard';
import Chatbot from './components/Chatbot';

function App() {
  const [currentPage, setCurrentPage] = useState('form'); // form, dashboard, chatbot
  const [projectData, setProjectData] = useState(null);
  const [projectId, setProjectId] = useState(null);

  const API_URL = process.env.REACT_APP_API_URL || 'http://localhost:8000';

  const handleProjectSubmit = async (formData) => {
    try {
      const response = await axios.post(`${API_URL}/projects`, formData);
      setProjectId(response.data.project_id);
      setProjectData(formData);
      
      // Fetch analysis
      await axios.post(`${API_URL}/analyze`, {
        project_id: response.data.project_id,
        requirements: formData.requirements.map(r => r.text)
      });
      
      setCurrentPage('dashboard');
    } catch (error) {
      console.error('Error submitting project:', error);
      const message =
        error?.response?.data?.detail ||
        error?.response?.data?.message ||
        'Error creating project. Please try again.';
      throw new Error(message);
    }
  };

  const handleNavClick = (page) => {
    setCurrentPage(page);
  };

  return (
    <div className="app">
      <header className="app-header">
        <div className="header-content">
          <h1>🏗️ RAAG</h1>
          <p>Requirement Analysis & Architecture Generator v2.0</p>
        </div>
      </header>

      <nav className="app-nav">
        <button 
          className={currentPage === 'form' ? 'active' : ''} 
          onClick={() => handleNavClick('form')}
        >
          New Project
        </button>
        {projectId && (
          <>
            <button 
              className={currentPage === 'dashboard' ? 'active' : ''} 
              onClick={() => handleNavClick('dashboard')}
            >
              Analysis Dashboard
            </button>
            <button 
              className={currentPage === 'chatbot' ? 'active' : ''} 
              onClick={() => handleNavClick('chatbot')}
            >
              Project Chatbot
            </button>
          </>
        )}
      </nav>

      <main className="app-main">
        {currentPage === 'form' && (
          <ProjectForm onSubmit={handleProjectSubmit} />
        )}
        {currentPage === 'dashboard' && projectId && (
          <Dashboard projectId={projectId} projectData={projectData} apiUrl={API_URL} />
        )}
        {currentPage === 'chatbot' && projectId && (
          <Chatbot projectId={projectId} apiUrl={API_URL} />
        )}
      </main>

      <footer className="app-footer">
        <p>RAAG v2.0 - A polyglot microservices architecture demonstration</p>
      </footer>
    </div>
  );
}

export default App;
