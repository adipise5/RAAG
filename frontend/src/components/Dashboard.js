import React, { useState, useEffect, useCallback } from 'react';
import axios from 'axios';
import mermaid from 'mermaid';

function Dashboard({ projectId, projectData, apiUrl }) {
  const [analysis, setAnalysis] = useState(null);
  const [architecture, setArchitecture] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [exportLoading, setExportLoading] = useState(false);

  // Diagram generation state
  const [diagramPrompt, setDiagramPrompt] = useState('');
  const [diagramType, setDiagramType] = useState('component');
  const [diagramLoading, setDiagramLoading] = useState(false);
  const [generatedSvg, setGeneratedSvg] = useState('');
  const [dfdLevel0Svg, setDfdLevel0Svg] = useState('');
  const [dfdLevel1Svg, setDfdLevel1Svg] = useState('');
  const [dfdRenderError, setDfdRenderError] = useState('');

  const fetchAnalysisData = useCallback(async () => {
    try {
      setLoading(true);

      // Fetch analysis
      const analysisRes = await axios.get(`${apiUrl}/analysis/${projectId}`);
      setAnalysis(analysisRes.data);

      // Fetch architecture recommendations
      try {
        const archRes = await axios.post(`${apiUrl}/generate-architecture`, {
          projectId,
          projectName: projectData?.name || '',
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
  }, [apiUrl, projectData, projectId]);

  useEffect(() => {
    fetchAnalysisData();
  }, [fetchAnalysisData]);

  useEffect(() => {
    let cancelled = false;

    const renderDfdMermaid = async () => {
      const level0Code = architecture?.dfd?.level_0?.mermaid;
      const level1Code = architecture?.dfd?.level_1?.mermaid;

      if (!level0Code && !level1Code) {
        setDfdLevel0Svg('');
        setDfdLevel1Svg('');
        setDfdRenderError('');
        return;
      }

      try {
        mermaid.initialize({
          startOnLoad: false,
          securityLevel: 'loose',
          theme: 'default'
        });

        let nextL0 = '';
        let nextL1 = '';

        if (level0Code) {
          const { svg } = await mermaid.render(`dfd-l0-${Date.now()}`, level0Code);
          nextL0 = svg;
        }

        if (level1Code) {
          const { svg } = await mermaid.render(`dfd-l1-${Date.now()}`, level1Code);
          nextL1 = svg;
        }

        if (!cancelled) {
          setDfdLevel0Svg(nextL0);
          setDfdLevel1Svg(nextL1);
          setDfdRenderError('');
        }
      } catch (err) {
        if (!cancelled) {
          setDfdRenderError('Could not render Mermaid DFD.');
          setDfdLevel0Svg('');
          setDfdLevel1Svg('');
        }
      }
    };

    renderDfdMermaid();

    return () => {
      cancelled = true;
    };
  }, [architecture?.dfd?.level_0?.mermaid, architecture?.dfd?.level_1?.mermaid]);

  const handleExportPDF = async () => {
    try {
      setExportLoading(true);
      const response = await axios.post(`${apiUrl}/export/${projectId}`, {}, {
        responseType: 'blob',
        timeout: 90000
      });
      
      const url = window.URL.createObjectURL(new Blob([response.data], { type: 'application/pdf' }));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `raag-report-${projectId}.pdf`);
      document.body.appendChild(link);
      link.click();
      link.parentNode.removeChild(link);
      window.URL.revokeObjectURL(url);
    } catch (err) {
      const msg = err.code === 'ECONNABORTED'
        ? 'PDF generation timed out. The report may be large — try again.'
        : `Failed to export PDF: ${err.response?.status || 'network error'}`;
      alert(msg);
    } finally {
      setExportLoading(false);
    }
  };

  const handleGenerateDiagram = async () => {
    if (!diagramPrompt.trim()) return;
    try {
      setDiagramLoading(true);
      setGeneratedSvg('');
      const codeRes = await axios.post(`${apiUrl}/generate-diagram`, {
        prompt: diagramPrompt,
        diagram_type: diagramType
      });
      const svgRes = await axios.post(`${apiUrl}/render-diagram`, {
        code: codeRes.data.plantuml_code
      });
      setGeneratedSvg(svgRes.data.svg || '');
    } catch (err) {
      alert('Failed to generate diagram');
    } finally {
      setDiagramLoading(false);
    }
  };

  const rtmRows = architecture?.traceabilityMatrix?.matrix || [];
  const canonicalComponents = [
    'API Gateway',
    'Ingestion + LLM Analysis',
    'Quality + Architecture Services',
    'Frontend Dashboard & Chat UI',
    'Export Service',
    'Audit Service'
  ];
  const rtmComponents = canonicalComponents.filter((comp) =>
    rtmRows.some((row) => (row.components || []).includes(comp))
  );
  const visibleRtmComponents = rtmComponents.length > 0 ? rtmComponents : canonicalComponents;

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
            <div className="value" style={{ fontSize: '1rem' }}>{projectData?.name}</div>
          </div>
          <div className="metric-card">
            <h3>Total Requirements</h3>
            <div className="value">{projectData?.requirements?.length || 0}</div>
          </div>
          <div className="metric-card">
            <h3>Proposed Architecture</h3>
            <div className="value" style={{ fontSize: '1rem' }}>{projectData?.proposed_architecture}</div>
          </div>
          <div className="metric-card">
            <h3>Domain</h3>
            <div className="value" style={{ fontSize: '1rem' }}>{projectData?.domain || 'General'}</div>
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
              <h3>Ambiguous Requirements</h3>
              <div className="value" style={{ color: analysis.quality_summary?.ambiguous_count > 0 ? '#f59e0b' : '#22c55e' }}>
                {analysis.quality_summary?.ambiguous_count || 0}
              </div>
            </div>
            <div className="metric-card">
              <h3>High Risk (Score &lt; 60)</h3>
              <div className="value" style={{ color: analysis.quality_summary?.high_risk > 0 ? '#ef4444' : '#22c55e' }}>
                {analysis.quality_summary?.high_risk || 0}
              </div>
            </div>
          </div>

          <h3 style={{ marginTop: '24px', marginBottom: '14px', fontSize: '1rem', fontWeight: 600, color: '#1e293b' }}>Requirement Classifications</h3>
          <table className="classification-table">
            <thead>
              <tr>
                <th>#</th>
                <th>Requirement</th>
                <th>Type</th>
                <th>Priority</th>
                <th>Confidence</th>
                <th>Quality</th>
                <th>Vague</th>
              </tr>
            </thead>
            <tbody>
              {analysis.classifications?.map((cls, idx) => (
                <tr key={idx}>
                  <td style={{ fontWeight: 600, color: '#64748b' }}>{cls.requirement_index + 1}</td>
                  <td className="requirement-cell">
                    {cls.text || projectData?.requirements?.[cls.requirement_index]?.text || '-'}
                    {cls.subcategories?.length > 0 && (
                      <div style={{ marginTop: '4px' }}>
                        {cls.subcategories.map((sub, i) => (
                          <span key={i} style={{ display: 'inline-block', fontSize: '0.7rem', padding: '2px 8px', borderRadius: '10px', background: '#f1f5f9', color: '#64748b', marginRight: '4px', marginTop: '2px' }}>{sub}</span>
                        ))}
                      </div>
                    )}
                  </td>
                  <td>
                    <span className={cls.classification === 'FR' ? 'badge-fr' : cls.classification === 'NFR' ? 'badge-nfr' : 'badge-fr'}>
                      {cls.classification}
                    </span>
                  </td>
                  <td>
                    <span style={{
                      fontWeight: 600,
                      fontSize: '0.8rem',
                      color: cls.priority === 'High' ? '#ef4444' : cls.priority === 'Medium' ? '#f59e0b' : '#22c55e'
                    }}>
                      {cls.priority || 'Medium'}
                    </span>
                  </td>
                  <td style={{ fontWeight: 500 }}>{(cls.confidence * 100).toFixed(0)}%</td>
                  <td style={{ fontWeight: 500 }}>{cls.quality_score}%</td>
                  <td>{cls.is_vague ? <span style={{ color: '#f59e0b', fontWeight: 600 }}>Yes</span> : <span style={{ color: '#94a3b8' }}>No</span>}</td>
                </tr>
              ))}
            </tbody>
          </table>

          {/* SMART Rewrites for vague/low-quality requirements */}
          {analysis.classifications?.some(cls => cls.is_vague || cls.quality_score < 70) && (
            <div style={{ marginTop: '24px' }}>
              <h3 style={{ marginBottom: '14px', fontSize: '1rem', fontWeight: 600, color: '#1e293b' }}>Suggested SMART Rewrites</h3>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                {analysis.classifications.filter(cls => cls.is_vague || cls.quality_score < 70).map((cls, idx) => (
                  <div key={idx} style={{ padding: '14px 18px', background: '#fffbeb', borderRadius: '10px', border: '1px solid #fde68a' }}>
                    <div style={{ fontSize: '0.8rem', color: '#92400e', fontWeight: 600, marginBottom: '6px' }}>
                      Requirement #{cls.requirement_index + 1} — Quality: {cls.quality_score}%
                      {cls.missing_elements?.length > 0 && <span style={{ fontWeight: 400 }}> (Missing: {cls.missing_elements.join(', ')})</span>}
                    </div>
                    <div style={{ fontSize: '0.85rem', color: '#78350f' }}>
                      <strong>Original:</strong> {cls.text}
                    </div>
                    <div style={{ fontSize: '0.85rem', color: '#065f46', marginTop: '6px' }}>
                      <strong>Rewrite:</strong> {cls.rewritten_requirement || cls.improved_requirement}
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Extracted Entities, Processes, Data Stores */}
          {analysis.classifications?.some(cls => cls.entities?.length || cls.processes?.length || cls.data_stores?.length) && (
            <div style={{ marginTop: '24px' }}>
              <h3 style={{ marginBottom: '14px', fontSize: '1rem', fontWeight: 600, color: '#1e293b' }}>Extracted Elements</h3>
              <div className="metric-grid" style={{ gridTemplateColumns: 'repeat(3, 1fr)' }}>
                <div style={{ padding: '16px 18px', background: '#f8fafc', borderRadius: '10px', border: '1px solid #e2e8f0' }}>
                  <h4 style={{ fontSize: '0.85rem', fontWeight: 600, color: '#1e293b', marginBottom: '10px' }}>Entities</h4>
                  <div style={{ display: 'flex', flexWrap: 'wrap', gap: '6px' }}>
                    {[...new Set(analysis.classifications.flatMap(c => c.entities || []))].map((e, i) => (
                      <span key={i} style={{ fontSize: '0.78rem', padding: '3px 10px', borderRadius: '12px', background: '#dbeafe', color: '#1e40af' }}>{e}</span>
                    ))}
                  </div>
                </div>
                <div style={{ padding: '16px 18px', background: '#f8fafc', borderRadius: '10px', border: '1px solid #e2e8f0' }}>
                  <h4 style={{ fontSize: '0.85rem', fontWeight: 600, color: '#1e293b', marginBottom: '10px' }}>Processes</h4>
                  <div style={{ display: 'flex', flexWrap: 'wrap', gap: '6px' }}>
                    {[...new Set(analysis.classifications.flatMap(c => c.processes || []))].map((p, i) => (
                      <span key={i} style={{ fontSize: '0.78rem', padding: '3px 10px', borderRadius: '12px', background: '#dcfce7', color: '#166534' }}>{p}</span>
                    ))}
                  </div>
                </div>
                <div style={{ padding: '16px 18px', background: '#f8fafc', borderRadius: '10px', border: '1px solid #e2e8f0' }}>
                  <h4 style={{ fontSize: '0.85rem', fontWeight: 600, color: '#1e293b', marginBottom: '10px' }}>Data Stores</h4>
                  <div style={{ display: 'flex', flexWrap: 'wrap', gap: '6px' }}>
                    {[...new Set(analysis.classifications.flatMap(c => c.data_stores || []))].map((d, i) => (
                      <span key={i} style={{ fontSize: '0.78rem', padding: '3px 10px', borderRadius: '12px', background: '#fef3c7', color: '#92400e' }}>{d}</span>
                    ))}
                  </div>
                </div>
              </div>
            </div>
          )}
        </div>
      )}

      {/* Architecture Recommendation */}
      {architecture && (
        <div className="dashboard-section">
          <h2>Architecture Recommendation</h2>
          <div className="metric-grid">
            <div className="metric-card">
              <h3>Recommended Style</h3>
              <div className="value" style={{ fontSize: '1rem' }}>{architecture.recommendedStyle}</div>
            </div>
            <div className="metric-card">
              <h3>Your Proposed</h3>
              <div className="value" style={{ fontSize: '1rem' }}>{architecture.proposedStyle}</div>
            </div>
            <div className="metric-card">
              <h3>Complexity</h3>
              <div className="value">{architecture.complexity?.toFixed(1)}</div>
            </div>
          </div>

          {architecture.justification && (
            <div style={{ marginTop: '18px', padding: '18px 20px', background: '#f8fafc', borderRadius: '10px', border: '1px solid #e2e8f0' }}>
              <h4 style={{ marginBottom: '10px', fontSize: '0.95rem', fontWeight: 600, color: '#1e293b' }}>Justification</h4>
              <ul style={{ marginLeft: '18px', color: '#475569', lineHeight: 1.7 }}>
                {architecture.justification.map((item, idx) => (
                  <li key={idx} style={{ marginBottom: '6px', fontSize: '0.9rem' }}>{item}</li>
                ))}
              </ul>
            </div>
          )}

          {architecture.comparison && architecture.comparison.length > 0 && architecture.comparison[0] !== 'Proposed architecture matches recommendation.' && (
            <div style={{ marginTop: '18px', padding: '18px 20px', background: '#fefce8', borderRadius: '10px', border: '1px solid #fde68a' }}>
              <h4 style={{ marginBottom: '10px', fontSize: '0.95rem', fontWeight: 600, color: '#92400e' }}>
                Why {architecture.recommendedStyle} over {architecture.proposedStyle}?
              </h4>
              <ul style={{ marginLeft: '18px', color: '#78350f', lineHeight: 1.7 }}>
                {architecture.comparison.map((item, idx) => (
                  <li key={idx} style={{ marginBottom: '6px', fontSize: '0.9rem' }}>{item}</li>
                ))}
              </ul>
            </div>
          )}

          {/* Complexity Estimation */}
          {architecture.complexityEstimation && (
            <div style={{ marginTop: '24px' }}>
              <h3 style={{ marginBottom: '14px', fontSize: '1rem', fontWeight: 600, color: '#1e293b' }}>Complexity Estimation</h3>
              <div className="metric-grid">
                <div className="metric-card">
                  <h3>Function Points</h3>
                  <div className="value">{architecture.complexityEstimation.function_points}</div>
                </div>
                <div className="metric-card">
                  <h3>Story Points</h3>
                  <div className="value">{architecture.complexityEstimation.story_points}</div>
                </div>
                <div className="metric-card">
                  <h3>Effort Estimate</h3>
                  <div className="value">{architecture.complexityEstimation.effort_estimate_weeks} wks</div>
                </div>
              </div>
              {architecture.complexityEstimation.top_complex_requirements?.length > 0 && (
                <div style={{ marginTop: '12px', padding: '14px 18px', background: '#f8fafc', borderRadius: '10px', border: '1px solid #e2e8f0' }}>
                  <h4 style={{ fontSize: '0.85rem', fontWeight: 600, color: '#64748b', marginBottom: '8px' }}>Most Complex Requirements</h4>
                  <ol style={{ marginLeft: '18px', color: '#475569', lineHeight: 1.7 }}>
                    {architecture.complexityEstimation.top_complex_requirements.map((req, idx) => (
                      <li key={idx} style={{ fontSize: '0.85rem', marginBottom: '4px' }}>{req}</li>
                    ))}
                  </ol>
                </div>
              )}
            </div>
          )}

          {/* Novelty Assessment */}
          {architecture.noveltyAssessment && (
            <div style={{ marginTop: '24px' }}>
              <h3 style={{ marginBottom: '14px', fontSize: '1rem', fontWeight: 600, color: '#1e293b' }}>Novelty Assessment</h3>
              <div className="metric-grid">
                <div className="metric-card">
                  <h3>Novelty Score</h3>
                  <div className="value">{architecture.noveltyAssessment.score}</div>
                </div>
                <div className="metric-card">
                  <h3>Category</h3>
                  <div className="value" style={{ fontSize: '0.95rem' }}>{architecture.noveltyAssessment.category}</div>
                </div>
              </div>
              {architecture.noveltyAssessment.breakdown && (
                <div style={{ marginTop: '12px', padding: '14px 18px', background: '#f8fafc', borderRadius: '10px', border: '1px solid #e2e8f0' }}>
                  <h4 style={{ fontSize: '0.85rem', fontWeight: 600, color: '#64748b', marginBottom: '12px' }}>Breakdown</h4>
                  {['technical', 'domain', 'approach'].map(dim => (
                    <div key={dim} style={{ marginBottom: '8px' }}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '3px' }}>
                        <span style={{ fontSize: '0.8rem', fontWeight: 500, color: '#475569', textTransform: 'capitalize' }}>{dim}</span>
                        <span style={{ fontSize: '0.8rem', fontWeight: 600, color: '#1e293b' }}>{architecture.noveltyAssessment.breakdown[dim]}%</span>
                      </div>
                      <div style={{ height: '6px', background: '#e2e8f0', borderRadius: '3px', overflow: 'hidden' }}>
                        <div style={{
                          height: '100%',
                          width: `${architecture.noveltyAssessment.breakdown[dim]}%`,
                          background: architecture.noveltyAssessment.breakdown[dim] >= 70 ? '#4f46e5' : architecture.noveltyAssessment.breakdown[dim] >= 50 ? '#f59e0b' : '#94a3b8',
                          borderRadius: '3px',
                          transition: 'width 0.5s ease'
                        }} />
                      </div>
                    </div>
                  ))}
                  {architecture.noveltyAssessment.reasoning && (
                    <p style={{ fontSize: '0.83rem', color: '#64748b', marginTop: '10px', lineHeight: 1.5 }}>{architecture.noveltyAssessment.reasoning}</p>
                  )}
                </div>
              )}
            </div>
          )}

          {/* Gap Analysis */}
          {architecture.gapAnalysis && architecture.gapAnalysis.length > 0 && (
            <div style={{ marginTop: '24px' }}>
              <h3 style={{ marginBottom: '14px', fontSize: '1rem', fontWeight: 600, color: '#1e293b' }}>Gap Analysis</h3>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
                {architecture.gapAnalysis.map((gap, idx) => (
                  <div key={idx} style={{
                    padding: '14px 18px', borderRadius: '10px',
                    background: gap.severity === 'Critical' ? '#fef2f2' : gap.severity === 'High' ? '#fffbeb' : '#f8fafc',
                    border: `1px solid ${gap.severity === 'Critical' ? '#fecaca' : gap.severity === 'High' ? '#fde68a' : '#e2e8f0'}`
                  }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '6px' }}>
                      <span style={{ fontWeight: 600, fontSize: '0.88rem', color: '#1e293b' }}>{gap.gap}</span>
                      <span style={{
                        fontSize: '0.72rem', fontWeight: 600, padding: '2px 10px', borderRadius: '10px',
                        background: gap.severity === 'Critical' ? '#ef4444' : gap.severity === 'High' ? '#f59e0b' : '#94a3b8',
                        color: '#fff'
                      }}>{gap.severity}</span>
                    </div>
                    <p style={{ fontSize: '0.83rem', color: '#475569', margin: 0, lineHeight: 1.5 }}>{gap.suggestion}</p>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Risk & Assumptions */}
          {architecture.riskAndAssumptions && architecture.riskAndAssumptions.length > 0 && (
            <div style={{ marginTop: '24px' }}>
              <h3 style={{ marginBottom: '14px', fontSize: '1rem', fontWeight: 600, color: '#1e293b' }}>Risks & Assumptions</h3>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
                {architecture.riskAndAssumptions.map((item, idx) => (
                  <div key={idx} style={{
                    padding: '14px 18px', borderRadius: '10px',
                    background: item.type === 'Risk' ? '#fef2f2' : '#f0f9ff',
                    border: `1px solid ${item.type === 'Risk' ? '#fecaca' : '#bae6fd'}`
                  }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '6px' }}>
                      <span style={{ fontWeight: 600, fontSize: '0.85rem', color: '#1e293b' }}>
                        <span style={{
                          display: 'inline-block', fontSize: '0.7rem', fontWeight: 600, padding: '2px 8px', borderRadius: '8px', marginRight: '8px',
                          background: item.type === 'Risk' ? '#fee2e2' : '#e0f2fe',
                          color: item.type === 'Risk' ? '#dc2626' : '#0284c7'
                        }}>{item.type}</span>
                        {item.description}
                      </span>
                      <span style={{
                        fontSize: '0.7rem', fontWeight: 600, padding: '2px 8px', borderRadius: '8px',
                        background: item.severity === 'Critical' ? '#ef4444' : item.severity === 'High' ? '#f59e0b' : '#94a3b8',
                        color: '#fff', whiteSpace: 'nowrap', marginLeft: '8px'
                      }}>{item.severity}</span>
                    </div>
                    <p style={{ fontSize: '0.83rem', color: '#475569', margin: 0, lineHeight: 1.5 }}>
                      <strong>Mitigation:</strong> {item.mitigation}
                    </p>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Traceability Matrix */}
          {architecture.traceabilityMatrix && architecture.traceabilityMatrix.matrix?.length > 0 && (
            <div style={{ marginTop: '24px' }}>
              <h3 style={{ marginBottom: '14px', fontSize: '1rem', fontWeight: 600, color: '#1e293b' }}>Traceability Matrix</h3>
              <div style={{ overflowX: 'auto', border: '1px solid #e5e7eb', borderRadius: '8px', padding: '0', background: '#fff' }}>
                <table className="classification-table" style={{ marginTop: 0, border: 'none' }}>
                  <thead>
                    <tr>
                      <th style={{ minWidth: '90px' }}>Requirement</th>
                      {visibleRtmComponents.map((comp, idx) => (
                        <th key={`rtm-comp-head-${idx}`} style={{ minWidth: '180px' }}>{comp}</th>
                      ))}
                    </tr>
                  </thead>
                  <tbody>
                    {rtmRows.map((row, rIdx) => (
                      <tr key={`rtm-row-${rIdx}`}>
                        <td style={{ fontWeight: 600 }}>R{rIdx + 1}</td>
                        {visibleRtmComponents.map((comp, cIdx) => (
                          <td key={`rtm-cell-${rIdx}-${cIdx}`} style={{ textAlign: 'center', fontWeight: 700 }}>
                            {(row.components || []).includes(comp) ? 'X' : ''}
                          </td>
                        ))}
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>

              <div style={{ marginTop: '10px', padding: '10px 12px', background: '#f8fafc', border: '1px solid #e5e7eb', borderRadius: '8px' }}>
                <div style={{ fontSize: '0.8rem', fontWeight: 600, color: '#374151', marginBottom: '6px' }}>Legend</div>
                <div style={{ fontSize: '0.78rem', color: '#4b5563', marginBottom: '6px' }}>
                  {rtmRows.map((row, idx) => (
                    <div key={`req-legend-${idx}`}>R{idx + 1}: {row.requirement}</div>
                  ))}
                </div>
                <div style={{ fontSize: '0.78rem', color: '#4b5563' }}>
                  {visibleRtmComponents.map((comp, idx) => (
                    <div key={`comp-legend-${idx}`}>{comp}</div>
                  ))}
                </div>
              </div>

              {architecture.traceabilityMatrix.untraced_requirements?.length > 0 && (
                <div style={{ marginTop: '10px', padding: '10px 14px', background: '#fef2f2', borderRadius: '8px', border: '1px solid #fecaca' }}>
                  <span style={{ fontSize: '0.8rem', fontWeight: 600, color: '#dc2626' }}>
                    {architecture.traceabilityMatrix.untraced_requirements.length} untraced requirement(s)
                  </span>
                </div>
              )}
              {architecture.traceabilityMatrix.high_density_components?.length > 0 && (
                <div style={{ marginTop: '10px', padding: '10px 14px', background: '#fffbeb', borderRadius: '8px', border: '1px solid #fde68a' }}>
                  <span style={{ fontSize: '0.8rem', fontWeight: 600, color: '#92400e' }}>High-density components: </span>
                  {architecture.traceabilityMatrix.high_density_components.map((hd, i) => (
                    <span key={i} style={{ fontSize: '0.8rem', color: '#78350f' }}>
                      {hd.component} ({hd.requirement_count} reqs){i < architecture.traceabilityMatrix.high_density_components.length - 1 ? ', ' : ''}
                    </span>
                  ))}
                </div>
              )}
            </div>
          )}

          {/* DFD Diagrams */}
          {architecture.dfd && (
            <div style={{ marginTop: '24px' }}>
              <h3 style={{ marginBottom: '16px', fontSize: '1rem', fontWeight: 600, color: '#1e293b' }}>Data Flow Diagrams</h3>
              {dfdRenderError && (
                <div style={{ marginBottom: '14px', padding: '10px 12px', borderRadius: '8px', background: '#fef2f2', border: '1px solid #fecaca', color: '#b91c1c', fontSize: '0.82rem' }}>
                  {dfdRenderError}
                </div>
              )}
              {(dfdLevel0Svg || architecture.dfd.level_0?.svg) && (
                <div style={{ marginBottom: '24px' }}>
                  <h4 style={{ marginBottom: '10px', color: '#64748b', fontSize: '0.88rem', fontWeight: 600 }}>Level 0 — System Context</h4>
                  <div
                    style={{ overflowX: 'auto', border: '1px solid #e2e8f0', borderRadius: '10px', padding: '16px', background: '#fff' }}
                    dangerouslySetInnerHTML={{ __html: dfdLevel0Svg || architecture.dfd.level_0?.svg }}
                  />
                </div>
              )}
              {(dfdLevel1Svg || architecture.dfd.level_1?.svg) && (
                <div>
                  <h4 style={{ marginBottom: '10px', color: '#64748b', fontSize: '0.88rem', fontWeight: 600 }}>Level 1 — Functional Decomposition</h4>
                  <div
                    style={{ overflowX: 'auto', border: '1px solid #e2e8f0', borderRadius: '10px', padding: '16px', background: '#fff' }}
                    dangerouslySetInnerHTML={{ __html: dfdLevel1Svg || architecture.dfd.level_1?.svg }}
                  />
                </div>
              )}
            </div>
          )}

          {/* Additional Diagrams */}
          {architecture.additionalDiagrams && architecture.additionalDiagrams.length > 0 && (
            <div style={{ marginTop: '24px' }}>
              <h3 style={{ marginBottom: '16px', fontSize: '1rem', fontWeight: 600, color: '#1e293b' }}>Architecture Diagrams</h3>
              {architecture.additionalDiagrams.map((diagram, idx) => (
                <div key={idx} style={{ marginBottom: '24px' }}>
                  <h4 style={{ marginBottom: '10px', color: '#64748b', fontSize: '0.88rem', fontWeight: 600 }}>{diagram.title}</h4>
                  <div
                    style={{ overflowX: 'auto', border: '1px solid #e2e8f0', borderRadius: '10px', padding: '16px', background: '#fff' }}
                    dangerouslySetInnerHTML={{ __html: diagram.svg }}
                  />
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {/* Generate Diagram */}
      <div className="dashboard-section">
        <h2>Generate Diagram</h2>
        <p style={{ marginBottom: '16px', color: '#64748b', fontSize: '0.9rem' }}>
          Describe the diagram you want and the AI will generate it using PlantUML.
        </p>
        <div style={{ display: 'flex', gap: '10px', marginBottom: '16px', flexWrap: 'wrap' }}>
          <input
            type="text"
            value={diagramPrompt}
            onChange={e => setDiagramPrompt(e.target.value)}
            onKeyDown={e => e.key === 'Enter' && handleGenerateDiagram()}
            placeholder="e.g. Show how users authenticate and access the API"
            style={{ flex: '1', minWidth: '200px', padding: '10px 14px', border: '1.5px solid #e2e8f0', borderRadius: '8px', fontSize: '0.9rem', transition: 'border-color 0.2s, box-shadow 0.2s', outline: 'none' }}
          />
          <select
            value={diagramType}
            onChange={e => setDiagramType(e.target.value)}
            style={{ padding: '10px 14px', border: '1.5px solid #e2e8f0', borderRadius: '8px', fontSize: '0.9rem', color: '#1e293b', background: '#fff' }}
          >
            <option value="component">Component</option>
            <option value="sequence">Sequence</option>
            <option value="dfd">Data Flow</option>
            <option value="usecase">Use Case</option>
            <option value="er">ER Diagram</option>
          </select>
          <button
            className="btn-primary"
            onClick={handleGenerateDiagram}
            disabled={diagramLoading || !diagramPrompt.trim()}
          >
            {diagramLoading ? 'Generating...' : 'Generate Diagram'}
          </button>
        </div>
        {generatedSvg && (
          <div style={{ overflowX: 'auto', border: '1px solid #e2e8f0', borderRadius: '10px', padding: '20px', background: '#fff' }}>
            <div dangerouslySetInnerHTML={{ __html: generatedSvg }} />
          </div>
        )}
      </div>

      {/* Export Section */}
      <div className="dashboard-section">
        <h2>Export Report</h2>
        <p style={{ marginBottom: '16px', color: '#64748b', fontSize: '0.9rem' }}>
          Generate a comprehensive PDF report with all analysis data.
        </p>
        <button
          className="btn-primary"
          onClick={handleExportPDF}
          disabled={exportLoading}
        >
          {exportLoading ? 'Generating PDF...' : 'Export as PDF'}
        </button>
      </div>
    </div>
  );
}

export default Dashboard;
