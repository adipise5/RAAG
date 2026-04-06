import React, { useState } from 'react';

function ProjectForm({ onSubmit }) {
  const [formData, setFormData] = useState({
    name: '',
    description: '',
    proposed_architecture: 'Microservices',
    domain: 'General',
    requirements: [{ text: '' }]
  });

  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
  };

  const handleRequirementChange = (index, value) => {
    const newRequirements = [...formData.requirements];
    newRequirements[index].text = value;
    setFormData(prev => ({
      ...prev,
      requirements: newRequirements
    }));
  };

  const addRequirement = () => {
    setFormData(prev => ({
      ...prev,
      requirements: [...prev.requirements, { text: '' }]
    }));
  };

  const removeRequirement = (index) => {
    if (formData.requirements.length > 1) {
      setFormData(prev => ({
        ...prev,
        requirements: prev.requirements.filter((_, i) => i !== index)
      }));
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    // Validation
    if (!formData.name.trim()) {
      setError('Project name is required');
      return;
    }
    if (formData.name.length < 3) {
      setError('Project name must be at least 3 characters');
      return;
    }
    if (!formData.description.trim()) {
      setError('Project description is required');
      return;
    }
    if (formData.requirements.some(r => !r.text.trim())) {
      setError('All requirements must have text');
      return;
    }

    setIsLoading(true);
    try {
      await onSubmit({
        ...formData,
        requirements: formData.requirements.filter(r => r.text.trim())
      });
    } catch (err) {
      setError(err?.message || 'Failed to submit project');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="form-container">
      <h2>Create New Project</h2>
      {error && <div style={{ color: '#e74c3c', marginBottom: '16px' }}>{error}</div>}

      <form onSubmit={handleSubmit}>
        <div className="form-group">
          <label>Project Name *</label>
          <input
            type="text"
            name="name"
            value={formData.name}
            onChange={handleInputChange}
            placeholder="e.g., E-commerce Platform"
            required
          />
          <small style={{ color: '#666' }}>{formData.name.length}/100</small>
        </div>

        <div className="form-group">
          <label>Project Description *</label>
          <textarea
            name="description"
            value={formData.description}
            onChange={handleInputChange}
            placeholder="Describe your project in detail..."
            required
          />
          <small style={{ color: '#666' }}>{formData.description.length}/2000</small>
        </div>

        <div className="form-group">
          <label>Proposed Architecture *</label>
          <select
            name="proposed_architecture"
            value={formData.proposed_architecture}
            onChange={handleInputChange}
          >
            <option>Microservices</option>
            <option>Monolithic</option>
            <option>Serverless</option>
            <option>Event-Driven</option>
            <option>Layered (N-Tier)</option>
            <option>Service-Oriented Architecture (SOA)</option>
            <option>Hexagonal / Ports and Adapters</option>
            <option>CQRS + Event Sourcing</option>
            <option>Peer-to-Peer (P2P)</option>
          </select>
        </div>

        <div className="form-group">
          <label>Project Domain</label>
          <select
            name="domain"
            value={formData.domain}
            onChange={handleInputChange}
          >
            <option>General</option>
            <option>E-commerce</option>
            <option>Healthcare</option>
            <option>Finance</option>
            <option>Education</option>
            <option>Social Media</option>
            <option>IoT</option>
            <option>Other</option>
          </select>
        </div>

        <div className="form-group">
          <label>Requirements *</label>
          <div className="requirements-list">
            {formData.requirements.map((req, index) => (
              <div key={index} className="requirement-input">
                <input
                  type="text"
                  value={req.text}
                  onChange={(e) => handleRequirementChange(index, e.target.value)}
                  placeholder={`Requirement ${index + 1}...`}
                />
                {formData.requirements.length > 1 && (
                  <button
                    type="button"
                    onClick={() => removeRequirement(index)}
                  >
                    Delete
                  </button>
                )}
              </div>
            ))}
          </div>
          <button
            type="button"
            className="btn-secondary"
            onClick={addRequirement}
            style={{ marginTop: '10px' }}
          >
            + Add Requirement
          </button>
        </div>

        <button
          type="submit"
          className="btn-primary"
          disabled={isLoading}
        >
          {isLoading ? 'Creating Project...' : 'Create Project Analysis'}
        </button>
      </form>
    </div>
  );
}

export default ProjectForm;
