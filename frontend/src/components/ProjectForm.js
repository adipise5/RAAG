import React, { useState } from 'react';

const DEMO_PROJECT = {
  name: "MediTrack Patient Portal",
  description: "A web-based patient management portal for a mid-size private hospital. Patients can register, book appointments, access medical records, communicate with doctors via secure messaging, and receive automated prescription reminders. The hospital staff can manage schedules, update records, and generate compliance reports. The system must integrate with existing HL7 FHIR-compliant Electronic Health Record (EHR) systems and a third-party payment gateway for billing. High availability and HIPAA compliance are critical.",
  proposed_architecture: "Microservices",
  use_selected_architecture_for_report: true,
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

const DEMO_PROJECT_B = {
  name: "EduFlow Corporate LMS",
  description: "A web-based Learning Management System for mid-to-large enterprises to manage internal employee training programs. HR managers can build a course catalog, assign courses to teams, and track completion. Employees access video lessons, take quizzes, and earn verifiable certificates. The platform must integrate with the company's existing identity provider via SAML 2.0 SSO, support SCORM-packaged third-party content, and scale to handle thousands of simultaneous learners during company-wide training rollouts.",
  proposed_architecture: "Layered (N-Tier)",
  use_selected_architecture_for_report: true,
  domain: "Education",
  requirements: [
    { text: "The system shall authenticate employees using SAML 2.0 SSO integration with the company's identity provider (e.g., Okta, Azure AD), provisioning accounts automatically on first login without requiring manual HR setup." },
    { text: "HR managers shall be able to create courses composed of ordered modules (video, document, or quiz), set a mandatory completion deadline per assignment, and bulk-assign courses to one or more teams or departments." },
    { text: "The system shall stream video lessons using adaptive bitrate (ABR) delivery, automatically adjusting quality based on the learner's available bandwidth, and persist the playback position so learners can resume where they left off." },
    { text: "The system shall deliver auto-graded quizzes with a configurable pass-mark threshold (0–100%) per quiz, allow up to 3 retake attempts by default, and immediately display the learner's score and correct answers upon submission." },
    { text: "Upon successful course completion, the system shall automatically generate a tamper-evident PDF certificate containing the learner's name, course title, completion date, and a unique QR code that resolves to a publicly accessible verification URL." },
    { text: "The system shall support upload and playback of SCORM 1.2 and xAPI (Tin Can) content packages, tracking completion status and score data back into the learner's progress record." },
    { text: "The system should be intuitive and visually clean so that employees with no technical background can navigate and complete their assigned courses without needing any training." },
    { text: "The system shall provide managers with a real-time team progress dashboard showing per-employee completion status, time spent, quiz scores, and overdue assignments, with the ability to export reports as XLSX or PDF." },
    { text: "The system shall send automated email reminders to learners 7 days and 3 days before a course deadline, and notify their direct manager if the course remains incomplete 1 day after the deadline." },
    { text: "The system shall support a minimum of 2,000 concurrent video streaming sessions without buffering or degraded quiz functionality, validated under load testing conditions." },
    { text: "Admin users shall be able to manage the course catalog including creating categories, archiving outdated courses, setting global enrollment rules, and viewing platform-wide analytics." },
    { text: "The application should be fast and responsive on both desktop and mobile browsers." },
  ]
};

function ProjectForm({ onSubmit }) {
  const [formData, setFormData] = useState({
    name: '',
    description: '',
    proposed_architecture: 'Microservices',
    use_selected_architecture_for_report: true,
    domain: 'General',
    requirements: [{ text: '' }]
  });

  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  const handleInputChange = (e) => {
    const { name, value, type, checked } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: type === 'checkbox' ? checked : value
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

  const loadDemoB = () => {
    setFormData(DEMO_PROJECT_B);
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
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px', paddingBottom: '12px', borderBottom: '1px solid var(--c-border)' }}>
        <h2 style={{ margin: 0, fontSize: '1.2rem', fontWeight: 700, color: 'var(--c-text)' }}>Create New Project</h2>
        <div style={{ display: 'flex', gap: '8px' }}>
          <button
            type="button"
            onClick={loadDemo}
            title="Healthcare Patient Portal — Microservices"
            className="demo-btn"
          >
            Demo A
          </button>
          <button
            type="button"
            onClick={loadDemoB}
            title="Corporate LMS — Layered N-Tier"
            className="demo-btn"
          >
            Demo B
          </button>
        </div>
      </div>
      {error && (
        <div style={{
          color: '#b91c1c',
          marginBottom: '16px',
          padding: '8px 12px',
          background: '#f9fafb',
          border: '1px solid #e5e7eb',
          borderRadius: '4px',
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
          <small style={{ color: '#6b7280', fontSize: '0.78rem' }}>{formData.name.length}/100</small>
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
          <small style={{ color: '#6b7280', fontSize: '0.78rem' }}>{formData.description.length}/2000</small>
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

        <div className="form-group" style={{ marginTop: '-8px', marginBottom: '18px' }}>
          <label style={{ display: 'flex', alignItems: 'center', gap: '10px', cursor: 'pointer', fontWeight: 500, fontSize: '0.88rem' }}>
            <input
              type="checkbox"
              name="use_selected_architecture_for_report"
              checked={formData.use_selected_architecture_for_report}
              onChange={handleInputChange}
            />
            Use my selected architecture for report &amp; diagrams
          </label>
          <small style={{ color: 'var(--c-text-muted)', fontSize: '0.78rem', marginLeft: '28px', display: 'block' }}>
            Diagrams and report sections will follow your selection above. An alternate LLM recommendation is still shown.
          </small>
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
