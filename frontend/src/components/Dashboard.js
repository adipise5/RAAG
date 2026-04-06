import React, { useState, useEffect } from 'react';
import axios from 'axios';

function Dashboard({ projectId, projectData, apiUrl }) {
  const [analysis, setAnalysis] = useState(null);
  const [architecture, setArchitecture] = useState(null);
  const [quality, setQuality] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [exportLoading, setExportLoading] = useState(false);

  useEffect(() => {
    fetchAnalysisData();
  }, [projectId]);

  const fetchAnalysisData = async () => {
    try {
      setLoading(true);

      // Fetch analysis
      const analysisRes = await axios.get(`${apiUrl}/analysis/${projectId}`);
      setAnalysis(analysisRes.data);

      // Fetch architecture recommendations
      try {
        const archRes = await axios.post(`${apiUrl}/generate-architecture`, {
          projectId,
          proposedStyle: projectData?.proposed_architecture || 'Microservices',
          requirements: projectData?.requirements?.map(r => r.text) || [],
          projectDescription: projectData?.description || '',
          domain: projectData?.domain || 'General'
        });
        setArchitecture(archRes.data);
      } catch (err) {
        console.log('Architecture not yet generated');
      }

      setError('');
    } catch (err) {
      setError('Failed to load analysis data');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleExportPDF = async () => {
    try {
      setExportLoading(true);
      const response = await axios.get(`${apiUrl}/export/${projectId}`, {
        responseType: 'blob'
      });
      
      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `raag-report-${projectId}.pdf`);
      document.body.appendChild(link);
      link.click();
      link.parentNode.removeChild(link);
    } catch (err) {
      alert('Failed to export PDF');
    } finally {
      setExportLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="loading">
        <div className="spinner"></div>
        <p>Loading analysis...</p>
      </div>
    );
  }

  if (error) {
    return <div style={{ color: '#e74c3c' }}>{error}</div>;
  }

  return (
    <div className="dashboard">
      {/* Project Overview */}
      <div className="dashboard-section">
        <h2>Project Overview</h2>
        <div className="metric-grid">
          <div className="metric-card">
            <h3>Project Name</h3>
            <div className="value" style={{ fontSize: '1.1rem' }}>{projectData?.name}</div>
          </div>
          <div className="metric-card">
            <h3>Total Requirements</h3>
            <div className="value">{projectData?.requirements?.length || 0}</div>
          </div>
          <div className="metric-card">
            <h3>Proposed Architecture</h3>
            <div className="value" style={{ fontSize: '1.1rem' }}>{projectData?.proposed_architecture}</div>
          </div>
          <div className="metric-card">
            <h3>Domain</h3>
            <div className="value" style={{ fontSize: '1.1rem' }}>{projectData?.domain || 'General'}</div>
          </div>
        </div>
      </div>

      {/* Quality Analysis */}
      {analysis && (
        <div className="dashboard-section">
          <h2>Quality Analysis</h2>
          <div className="metric-grid">
            <div className="metric-card">
              <h3>Overall Quality Score</h3>
              <div className="value">{analysis.overall_quality}%</div>
            </div>
            <div className="metric-card">
              <h3>Classifications Processed</h3>
              <div className="value">{analysis.classifications?.length || 0}</div>
            </div>
            <div className="metric-card">
              <h3>Analysis Date</h3>
              <div className="value" style={{ fontSize: '0.9rem' }}>
                {new Date(analysis.analyzed_at).toLocaleDateString()}
              </div>
            </div>
          </div>

          <h3 style={{ marginTop: '24px', marginBottom: '16px' }}>Requirement Classifications</h3>
          <table className="classification-table">
            <thead>
              <tr>
                <th>Index</th>
                <th>Requirement</th>
                <th>Classification</th>
                <th>Confidence</th>
                <th>Quality Score</th>
                <th>Vague</th>
              </tr>
            </thead>
            <tbody>
              {analysis.classifications?.map((cls, idx) => (
                <tr key={idx}>
                  <td>{cls.requirement_index + 1}</td>
                  <td className="requirement-cell">
                    {cls.text || projectData?.requirements?.[cls.requirement_index]?.text || '-'}
                  </td>
                  <td>
                    <span className={cls.classification === 'FR' ? 'badge-fr' : 'badge-nfr'}>
                      {cls.classification}
                    </span>
                  </td>
                  <td>{(cls.confidence * 100).toFixed(0)}%</td>
                  <td>{cls.quality_score}%</td>
                  <td>{cls.is_vague ? '⚠️ Yes' : 'No'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* Architecture Recommendation */}
      {architecture && (
        <div className="dashboard-section">
          <h2>Architecture Recommendation</h2>
          <div className="metric-grid">
            <div className="metric-card">
              <h3>Recommended Style</h3>
              <div className="value" style={{ fontSize: '1.1rem' }}>{architecture.recommendedStyle}</div>
            </div>
            <div className="metric-card">
              <h3>Your Proposed</h3>
              <div className="value" style={{ fontSize: '1.1rem' }}>{architecture.proposedStyle}</div>
            </div>
            <div className="metric-card">
              <h3>Complexity</h3>
              <div className="value">{architecture.complexity?.toFixed(1)}</div>
            </div>
          </div>

          {architecture.justification && (
            <div style={{ marginTop: '16px', padding: '16px', background: '#f0f7ff', borderRadius: '4px' }}>
              <h4 style={{ marginBottom: '8px' }}>Justification</h4>
              <ul style={{ marginLeft: '20px' }}>
                {architecture.justification.map((item, idx) => (
                  <li key={idx} style={{ marginBottom: '8px' }}>{item}</li>
                ))}
              </ul>
            </div>
          )}
        </div>
      )}

      {/* Export Section */}
      <div className="dashboard-section">
        <h2>Export Report</h2>
        <p style={{ marginBottom: '16px', color: '#666' }}>
          Generate a comprehensive PDF report with all analysis data
        </p>
        <button
          className="btn-primary"
          onClick={handleExportPDF}
          disabled={exportLoading}
        >
          {exportLoading ? 'Generating PDF...' : '📄 Export as PDF'}
        </button>
      </div>
    </div>
  );
}

export default Dashboard;
