import React, { useState } from 'react';

const DEMO_PROJECT = {
  name: "MediTrack Patient Portal",
  description: "A web-based patient management portal for a mid-size private hospital. Patients can register, book appointments, access medical records, communicate with doctors via secure messaging, and receive automated prescription reminders. The hospital staff can manage schedules, update records, and generate compliance reports. The system must integrate with existing HL7 FHIR-compliant Electronic Health Record (EHR) systems and a third-party payment gateway for billing. High availability and HIPAA compliance are critical.",
  proposed_architecture: "Microservices",
  domain: "Healthcare",
  requirements: [
    { text: "The system shall allow patients to register using their email address and a password, verify their identity via a one-time code sent to their registered email within 2 minutes, and activate their account before they can access any portal features." },
    { text: "The system shall allow authenticated patients to book, reschedule, or cancel appointments with available doctors up to 30 days in advance, with confirmation sent via email and SMS within 60 seconds of the action." },
    { text: "The system shall enable patients to view their complete medical history, including diagnoses, prescriptions, lab results, and visit notes, sourced from the integrated EHR system, with page load times under 3 seconds for 95% of requests." },
    { text: "The system shall provide a secure messaging feature between patients and their assigned doctor, where messages are encrypted in transit using TLS 1.3 and at rest using AES-256, with delivery confirmation visible to the sender." },
    { text: "The system shall automatically send prescription refill reminders to patients via email 7 days before a prescription expires, based on data from the EHR integration." },
    { text: "The system shall allow hospital staff to update a patient's medical record, and all changes must be logged in an immutable audit trail that records the staff member's ID, timestamp, and the previous and new values of each modified field." },
    { text: "The system should be fast and easy to use so that doctors can quickly access patient information." },
    { text: "The system shall process online bill payments through a third-party payment gateway, support Visa, Mastercard, and insurance billing codes, and store no raw card data, complying with PCI-DSS Level 1 requirements." },
    { text: "The system shall enforce role-based access control (RBAC) with at least four roles: Patient, Doctor, Nurse, and Admin, where each role has a strictly defined permission set and unauthorized access attempts are logged and trigger an alert to the security team within 5 minutes." },
    { text: "The patient portal must be reliable and should not go down too much." },
    { text: "The system shall expose a REST API compatible with HL7 FHIR R4 for bidirectional synchronization of patient records with external EHR systems, with data validation against the FHIR schema before any write operation." },
    { text: "The system shall maintain 99.9% uptime measured monthly, support up to 5,000 concurrent users, and recover from any single-node failure within 60 seconds without data loss." },
    { text: "The system shall generate monthly HIPAA compliance reports listing all record accesses, exports, and modifications grouped by user role, exportable as PDF or CSV, within 30 seconds of the report being requested." },
    { text: "The system shall ensure that all patient data is encrypted at rest using AES-256 and that database backups are taken every 6 hours with a retention period of 7 years to comply with healthcare data retention regulations." },
    { text: "Doctors should be able to easily see their daily schedule and patient list." },
  ]
};

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

  const loadDemo = () => {
    setFormData(DEMO_PROJECT);
    setError('');
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
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px', paddingBottom: '16px', borderBottom: '1px solid #e2e8f0' }}>
        <h2 style={{ margin: 0, fontSize: '1.25rem', fontWeight: 700, color: '#1e293b' }}>Create New Project</h2>
        <button
          type="button"
          onClick={loadDemo}
          style={{
            background: 'linear-gradient(135deg, #eef2ff, #e0e7ff)',
            color: '#4f46e5',
            border: '1px solid rgba(79,70,229,0.2)',
            borderRadius: '8px',
            padding: '8px 16px',
            cursor: 'pointer',
            fontWeight: 600,
            fontSize: '0.82rem',
            transition: 'all 0.2s ease',
            letterSpacing: '0.2px'
          }}
        >
          Load Demo Project
        </button>
      </div>
      {error && (
        <div style={{
          color: '#dc2626',
          marginBottom: '16px',
          padding: '10px 14px',
          background: '#fef2f2',
          border: '1px solid rgba(220,38,38,0.15)',
          borderRadius: '8px',
          fontSize: '0.9rem',
          fontWeight: 500
        }}>
          {error}
        </div>
      )}

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
          <small style={{ color: '#94a3b8', fontSize: '0.78rem' }}>{formData.name.length}/100</small>
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
          <small style={{ color: '#94a3b8', fontSize: '0.78rem' }}>{formData.description.length}/2000</small>
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
