import os
import re
import json
import math
import asyncio
import logging
from datetime import datetime
from typing import Optional, List, Dict, Any, Tuple

from fastapi import FastAPI, HTTPException, Body
from pydantic import BaseModel, Field
from pymongo import MongoClient

app = FastAPI()

# ==============================
# CONFIG
# ==============================
MONGO_URL = os.getenv("MONGO_URL", "mongodb://localhost:27017")
MAX_LLM_RETRIES = int(os.getenv("LLM_MAX_RETRIES", "2"))
OLLAMA_URL = os.getenv("OLLAMA_URL", "http://ollama:11434")
OLLAMA_MODEL = os.getenv("OLLAMA_MODEL", "qwen2.5:7b")
OLLAMA_TIMEOUT_SECONDS = int(os.getenv("OLLAMA_TIMEOUT_SECONDS", "60"))

mongo_client = MongoClient(MONGO_URL)
db = mongo_client["raag_projects"]
analysis_collection = db["analysis"]
projects_collection = db["projects"]

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("raag-llm")

# ==============================
# MODELS
# ==============================
class AnalysisRequest(BaseModel):
    project_id: str
    requirements: List[str]


class EnhancedQualityRequest(BaseModel):
    project_id: Optional[str] = None
    requirements: List[str] = Field(default_factory=list)


class GapAnalysisRequest(BaseModel):
    requirements: List[str] = Field(default_factory=list)


class TraceabilityRequest(BaseModel):
    requirements: List[str] = Field(default_factory=list)
    architecture_components: List[str] = Field(default_factory=list)


class RiskAssumptionRequest(BaseModel):
    project_description: Optional[str] = ""
    requirements: List[str] = Field(default_factory=list)


class ComplexityRequest(BaseModel):
    requirements: List[str] = Field(default_factory=list)


class NoveltyRequest(BaseModel):
    project_description: Optional[str] = ""
    domain: Optional[str] = "General"
    requirements: List[str] = Field(default_factory=list)


class RewriteRequest(BaseModel):
    requirement_text: str


class PromptRequest(BaseModel):
    prompt: str


# ==============================
# CONSTANTS & HEURISTICS
# ==============================
VAGUE_TERMS = [
    "fast", "quick", "easy", "simple", "user-friendly", "robust", "seamless",
    "efficient", "optimize", "as soon as possible", "etc", "and so on", "better"
]

CATEGORY_KEYWORDS = {
    "Security": ["security", "secure", "auth", "authentication", "authorization", "jwt", "token", "encrypt", "encryption", "owasp", "rbac", "privacy"],
    "Performance": ["performance", "latency", "throughput", "response time", "concurrent", "load", "scale", "scalability", "rps"],
    "Reliability": ["reliability", "availability", "uptime", "failover", "recovery", "backup", "fault", "resilience", "durability"],
    "Usability": ["usability", "ux", "ui", "accessible", "accessibility", "onboarding", "learnability", "user experience"],
    "Compliance": ["compliance", "regulatory", "gdpr", "hipaa", "pci", "soc2", "iso", "audit trail", "data retention"]
}

CATEGORY_SEVERITY = {
    "Security": "Critical",
    "Performance": "High",
    "Reliability": "High",
    "Usability": "Medium",
    "Compliance": "Critical"
}

CATEGORY_SUGGESTION = {
    "Security": "System shall enforce JWT-based authentication, role-based authorization, and encryption for data in transit and at rest.",
    "Performance": "System shall respond within 200ms for 95% of requests under 1,000 concurrent users.",
    "Reliability": "System shall maintain 99.9% uptime with automated failover and recovery within 5 minutes.",
    "Usability": "System shall provide task completion for first-time users within 3 minutes and meet WCAG 2.1 AA accessibility standards.",
    "Compliance": "System shall comply with applicable regulations (e.g., GDPR/PCI/HIPAA) and maintain auditable access and retention logs."
}

RISK_SEVERITY = {"low": "Low", "medium": "Medium", "high": "High", "critical": "Critical"}

_ollama_ready = False  # set to True after model is confirmed available
_ollama_semaphore: Optional[asyncio.Semaphore] = None  # limits concurrent Ollama calls


# ==============================
# JSON / TEXT UTILITIES
# ==============================
def normalize_requirements(requirements: List[str]) -> List[str]:
    return [r.strip() for r in requirements if isinstance(r, str) and r.strip()]


def extract_json_content(raw: str) -> Any:
    text = (raw or "").strip()
    if not text:
        raise ValueError("Empty LLM response")

    if text.startswith("```"):
        blocks = re.findall(r"```(?:json)?\s*([\s\S]*?)```", text, flags=re.IGNORECASE)
        if blocks:
            text = blocks[0].strip()

    try:
        return json.loads(text)
    except json.JSONDecodeError:
        pass

    match = re.search(r"(\{[\s\S]*\}|\[[\s\S]*\])", text)
    if match:
        candidate = match.group(1).strip()
        return json.loads(candidate)

    raise ValueError("No valid JSON found in LLM response")


def unwrap_result(payload: Any) -> Any:
    if isinstance(payload, dict) and "result" in payload:
        return payload["result"]
    return payload


def clamp_int(value: Any, low: int, high: int, default: int) -> int:
    try:
        val = int(round(float(value)))
        return max(low, min(high, val))
    except Exception:
        return default


def normalize_bool(value: Any, default: bool = False) -> bool:
    if isinstance(value, bool):
        return value
    if isinstance(value, str):
        return value.strip().lower() in {"true", "yes", "1"}
    if isinstance(value, (int, float)):
        return value != 0
    return default


def normalize_string_list(value: Any) -> List[str]:
    if isinstance(value, list):
        out = []
        for item in value:
            s = str(item).strip()
            if s:
                out.append(s)
        return out
    if isinstance(value, str) and value.strip():
        return [value.strip()]
    return []


def normalize_severity(value: Any, default: str = "Medium") -> str:
    if not isinstance(value, str):
        return default
    key = value.strip().lower()
    return RISK_SEVERITY.get(key, value.strip().title())


def sanitize_rewritten_requirement(text: str) -> str:
    cleaned = (text or "").strip()
    if not cleaned:
        return cleaned

    # Remove duplicated modal fragments sometimes produced by LLMs
    # e.g. "The system shall User shall ..."
    cleaned = re.sub(
        r"\bshall\s+(?:the\s+system|system|user|users|admin|administrator|customer|client|operator)\s+shall\b",
        "shall",
        cleaned,
        flags=re.IGNORECASE
    )

    cleaned = re.sub(r"\s+", " ", cleaned).strip()
    if cleaned and not cleaned.endswith("."):
        cleaned += "."
    return cleaned


def has_metric(text: str) -> bool:
    lower = text.lower()
    metric_patterns = [
        r"\b\d+\s*(ms|millisecond|milliseconds|s|sec|seconds|minutes|hours|days)\b",
        r"\b\d+\s*%",
        r"\b\d+[\d,]*\s*(users|requests|rps|req/s|transactions|tps|mb|gb)\b",
        r"\b(p95|p99|sla|slo|uptime)\b",
        r"\b(within|under|less than|greater than|at least|no more than)\b"
    ]
    return any(re.search(pattern, lower) for pattern in metric_patterns)


def detect_vague_terms(text: str) -> List[str]:
    lower = text.lower()
    found = []
    for term in VAGUE_TERMS:
        if re.search(rf"\b{re.escape(term)}\b", lower):
            found.append(term)
    return found


def extract_actor(text: str) -> Optional[str]:
    actor_patterns = [
        r"\b(the\s+system|system|user|users|admin|administrator|customer|client|operator|application|service|platform)\b",
        r"^\s*([A-Z][a-zA-Z0-9\s]{2,30})\s+(shall|must|should|can|will)"
    ]
    for pattern in actor_patterns:
        match = re.search(pattern, text, flags=re.IGNORECASE)
        if match:
            actor = match.group(1).strip()
            if actor.lower() in {"system", "the system"}:
                return "The system"
            return actor[0].upper() + actor[1:]
    return None


def extract_action_phrase(text: str) -> Optional[str]:
    match = re.search(r"\b(shall|must|should|can|will)\s+([^.,;]+)", text, flags=re.IGNORECASE)
    if match:
        action = match.group(2).strip()
        action = re.sub(r"\s+", " ", action)
        if action:
            return action

    # fallback: find likely verb chunk
    alt = re.search(r"\b(create|update|delete|process|validate|send|receive|store|retrieve|generate|analyze|classify|encrypt|authenticate)\b([^.,;]*)", text, flags=re.IGNORECASE)
    if alt:
        return f"{alt.group(1).lower()}{alt.group(2)}".strip()
    return None


def extract_condition_phrase(text: str) -> Optional[str]:
    match = re.search(r"\b(if|when|while|under|within|before|after|during|once|upon)\b([^.,;]*)", text, flags=re.IGNORECASE)
    if match:
        condition = f"{match.group(1).lower()}{match.group(2)}".strip()
        return re.sub(r"\s+", " ", condition)

    if has_metric(text):
        return "under the stated measurable constraints"

    return None


def detect_missing_elements(text: str) -> List[str]:
    missing = []
    if not extract_actor(text):
        missing.append("Actor")
    if not extract_action_phrase(text):
        missing.append("Action")
    if not extract_condition_phrase(text):
        missing.append("Condition")
    return missing


def compute_ieee830_score(text: str, vague_terms: List[str], missing_elements: List[str]) -> int:
    score = 100

    if vague_terms:
        score -= min(30, 8 * len(vague_terms))

    score -= 15 * len(missing_elements)

    lower = text.lower()
    if not re.search(r"\b(shall|must)\b", lower):
        score -= 10

    if not has_metric(text):
        score -= 10

    if len(text.split()) < 7:
        score -= 10

    if "?" in text:
        score -= 5

    return max(0, min(100, score))


def build_smart_rewrite(text: str, missing_elements: List[str], vague_terms: List[str]) -> str:
    actor = extract_actor(text) or "The system"
    action = extract_action_phrase(text) or "process the requested operation"
    condition = extract_condition_phrase(text) or "when a valid request is received"

    # Remove explicit vague terms from action phrase to improve measurability.
    cleaned_action = action
    for term in vague_terms:
        cleaned_action = re.sub(rf"\b{re.escape(term)}\b", "", cleaned_action, flags=re.IGNORECASE)
    cleaned_action = re.sub(r"\s+", " ", cleaned_action).strip() or "process the requested operation"
    if cleaned_action.lower() in {"be", "is", "are"}:
        cleaned_action = "process the requested operation"

    metric_clause = ""
    if has_metric(text):
        metric_clause = " while satisfying the quantitative limits specified in this requirement"
    else:
        metric_clause = " within 2 seconds for at least 1,000 concurrent users"

    rewritten = f"{actor} shall {cleaned_action} {condition}{metric_clause}."
    rewritten = re.sub(r"\s+", " ", rewritten).strip()

    # Ensure sentence starts with a capital letter and has a terminal period.
    rewritten = rewritten[0].upper() + rewritten[1:] if rewritten else rewritten
    if not rewritten.endswith("."):
        rewritten += "."

    return rewritten


def fallback_classification(requirement: str) -> Dict[str, Any]:
    lower = requirement.lower()
    nfr_tags = []
    fr_hit = bool(re.search(r"\b(create|update|delete|process|generate|send|receive|store|retrieve|allow|enable|show|display|calculate)\b", lower))

    for category, keywords in CATEGORY_KEYWORDS.items():
        if any(k in lower for k in keywords):
            nfr_tags.append(category)

    if fr_hit and nfr_tags:
        classification = "MIXED"
        confidence = 0.86
    elif nfr_tags:
        classification = "NFR"
        confidence = 0.9
    else:
        classification = "FR"
        confidence = 0.84

    justification = {
        "FR": "Requirement primarily describes expected system behavior.",
        "NFR": "Requirement primarily describes quality constraints and non-functional attributes.",
        "MIXED": "Requirement combines behavioral intent with quality constraints."
    }[classification]

    return {
        "classification": classification,
        "confidence": confidence,
        "subcategory": nfr_tags,
        "justification": justification,
        "quality_issues": []
    }


def fallback_quality_analysis(requirement: str) -> Dict[str, Any]:
    vague_terms = detect_vague_terms(requirement)
    missing_elements = detect_missing_elements(requirement)
    score = compute_ieee830_score(requirement, vague_terms, missing_elements)
    rewritten_requirement = sanitize_rewritten_requirement(
        build_smart_rewrite(requirement, missing_elements, vague_terms)
    )

    return {
        "score": score,
        "vagueness": bool(vague_terms),
        "missing_elements": missing_elements,
        "rewritten_requirement": rewritten_requirement
    }


def fallback_gap_analysis(requirements: List[str]) -> List[Dict[str, str]]:
    text = " ".join(requirements).lower()
    gaps = []

    for category, keywords in CATEGORY_KEYWORDS.items():
        if not any(keyword in text for keyword in keywords):
            gaps.append({
                "gap": f"No {category.lower()} requirements found",
                "severity": CATEGORY_SEVERITY[category],
                "suggestion": CATEGORY_SUGGESTION[category]
            })

    return gaps


def fallback_traceability(requirements: List[str], architecture_components: List[str]) -> Dict[str, Any]:
    components = [c.strip() for c in architecture_components if c and c.strip()]
    matrix = []
    counts: Dict[str, int] = {c: 0 for c in components}

    domain_hints = {
        "auth": ["Auth Service", "Identity Service", "API Gateway"],
        "login": ["Auth Service", "API Gateway"],
        "token": ["Auth Service", "API Gateway"],
        "user": ["User Service", "API Gateway"],
        "payment": ["Payment Service", "Billing Service"],
        "report": ["Reporting Service", "Analytics Service"],
        "event": ["Event Bus", "Message Broker"],
        "notification": ["Notification Service"],
        "audit": ["Audit Service"],
        "export": ["Export Service"]
    }

    for req in requirements:
        req_lower = req.lower()
        matched: List[str] = []

        for comp in components:
            comp_tokens = [t for t in re.split(r"[^a-z0-9]+", comp.lower()) if len(t) > 2]
            if any(token in req_lower for token in comp_tokens):
                matched.append(comp)

        for hint, mapped_components in domain_hints.items():
            if hint in req_lower:
                for candidate in mapped_components:
                    if candidate in components and candidate not in matched:
                        matched.append(candidate)

        if not matched and components:
            # Reasonable default: user-facing functional requirements usually traverse gateway.
            gateway_candidates = [c for c in components if "gateway" in c.lower()]
            if gateway_candidates and any(k in req_lower for k in ["user", "client", "request", "api"]):
                matched.append(gateway_candidates[0])

        matrix.append({"requirement": req, "components": matched})
        for comp in matched:
            counts[comp] = counts.get(comp, 0) + 1

    untraced_requirements = [entry["requirement"] for entry in matrix if not entry["components"]]

    threshold = max(2, math.ceil(len(requirements) * 0.4)) if requirements else 1
    high_density_components = [
        {"component": comp, "requirement_count": count}
        for comp, count in sorted(counts.items(), key=lambda x: x[1], reverse=True)
        if count >= threshold
    ]

    return {
        "matrix": matrix,
        "untraced_requirements": untraced_requirements,
        "high_density_components": high_density_components
    }


def fallback_risk_assumptions(project_description: str, requirements: List[str]) -> List[Dict[str, str]]:
    description = (project_description or "").lower()
    req_text = " ".join(requirements).lower()
    items: List[Dict[str, str]] = []

    if any(k in req_text for k in ["third-party", "external", "payment gateway", "vendor api", "dependency"]):
        items.append({
            "type": "Risk",
            "description": "Critical functionality depends on external third-party systems with uncertain availability and SLA.",
            "severity": "High",
            "mitigation": "Define fallback workflows, retries with circuit breakers, and contractual SLA monitoring for each external dependency."
        })

    if not any(k in req_text for k in CATEGORY_KEYWORDS["Compliance"]):
        items.append({
            "type": "Risk",
            "description": "Regulatory and compliance requirements are not explicit, creating audit and legal exposure.",
            "severity": "High",
            "mitigation": "Add explicit compliance requirements (e.g., GDPR/PCI/HIPAA), audit logging, and data retention controls."
        })

    if any(term in req_text for term in ["etc", "and so on", "future", "flexible", "as needed"]):
        items.append({
            "type": "Risk",
            "description": "Ambiguous scope language may cause scope creep and unclear acceptance criteria.",
            "severity": "Medium",
            "mitigation": "Convert open-ended requirements into SMART acceptance criteria and baseline them in change control."
        })

    if "real-time" in req_text and not any(k in req_text for k in ["latency", "ms", "p95", "throughput"]):
        items.append({
            "type": "Assumption",
            "description": "The team assumes infrastructure can satisfy real-time behavior without explicit latency targets.",
            "severity": "Medium",
            "mitigation": "Define p95/p99 latency SLOs and execute load testing before release milestones."
        })

    if not items:
        items.append({
            "type": "Assumption",
            "description": "Operational tooling (monitoring, alerting, logging) will be available for production diagnostics.",
            "severity": "Low",
            "mitigation": "Capture observability requirements explicitly and validate with non-functional test cases."
        })

    return items


def requirement_complexity_weight(requirement: str) -> int:
    lower = requirement.lower()
    weight = 3
    heavy_keywords = [
        "real-time", "distributed", "high availability", "multi-tenant", "encryption",
        "compliance", "machine learning", "ai", "event-driven", "failover", "stream"
    ]
    medium_keywords = ["integration", "synchronization", "analytics", "workflow", "concurrent", "scalable"]

    weight += sum(2 for kw in heavy_keywords if kw in lower)
    weight += sum(1 for kw in medium_keywords if kw in lower)

    if has_metric(requirement):
        weight += 1

    return max(1, min(weight, 13))


def fallback_complexity(requirements: List[str]) -> Dict[str, Any]:
    normalized = normalize_requirements(requirements)
    if not normalized:
        return {
            "function_points": 0,
            "story_points": 0,
            "effort_estimate_weeks": 0,
            "top_complex_requirements": []
        }

    weighted: List[Tuple[str, int]] = []
    fr_function_points = 0
    nfr_count = 0

    for req in normalized:
        classification = fallback_classification(req)["classification"]
        weight = requirement_complexity_weight(req)
        weighted.append((req, weight))

        if classification in {"FR", "MIXED"}:
            fr_function_points += weight
        if classification in {"NFR", "MIXED"}:
            nfr_count += 1

    function_points = fr_function_points
    story_points = int(round(function_points * 1.35 + max(1, nfr_count)))
    effort_estimate_weeks = max(1, math.ceil(story_points / 12))

    top_complex_requirements = [req for req, _ in sorted(weighted, key=lambda x: x[1], reverse=True)[:5]]

    return {
        "function_points": function_points,
        "story_points": story_points,
        "effort_estimate_weeks": effort_estimate_weeks,
        "top_complex_requirements": top_complex_requirements
    }


def fallback_novelty(project_description: str, requirements: List[str], domain: str) -> Dict[str, Any]:
    description = (project_description or "").lower()
    req_text = " ".join(requirements).lower()
    combined = f"{description} {req_text}".strip()

    technical = 35
    domain_score = 40
    approach = 35

    if any(k in combined for k in ["machine learning", "ai", "llm", "federated", "blockchain", "edge"]):
        technical += 25
    if any(k in combined for k in ["event-driven", "stream", "real-time", "distributed"]):
        technical += 15
    if any(k in combined for k in ["digital twin", "predictive", "autonomous"]):
        technical += 10

    domain_lower = (domain or "general").lower()
    if domain_lower in {"healthcare", "finance", "aerospace"}:
        domain_score = 58
    elif domain_lower in {"iot", "cybersecurity"}:
        domain_score = 54
    elif domain_lower in {"education", "social media", "general"}:
        domain_score = 42

    if any(k in combined for k in ["microservices", "serverless", "hexagonal", "cqrs", "event sourcing"]):
        approach += 18
    if any(k in combined for k in ["monolithic", "crud"]):
        approach -= 5

    technical = max(0, min(100, technical))
    domain_score = max(0, min(100, domain_score))
    approach = max(0, min(100, approach))

    score = int(round((technical + domain_score + approach) / 3))

    if score >= 75:
        category = "Novel"
    elif score >= 60:
        category = "Moderately Novel"
    elif score >= 40:
        category = "Incremental"
    else:
        category = "Conventional"

    reasoning = (
        "Novelty is driven by technical complexity, domain constraints, and architectural approach. "
        "Higher score indicates stronger differentiation from conventional enterprise solutions."
    )

    return {
        "score": score,
        "category": category,
        "breakdown": {
            "technical": technical,
            "domain": domain_score,
            "approach": approach
        },
        "reasoning": reasoning
    }


def fallback_custom_generate(prompt: str) -> Dict[str, Any]:
    lower_prompt = prompt.lower()

    if "compare architectures" in lower_prompt or "exactly 5" in lower_prompt:
        return {
            "result": [
                "Recommended architecture better handles scalability requirements for this project",
                "It provides stronger modular boundaries and clearer separation of concerns",
                "Deployment and release flexibility are improved for iterative delivery",
                "It aligns more closely with the stated performance and reliability constraints",
                "It supports future evolution and incremental growth with lower coupling"
            ]
        }

    if "explain why" in lower_prompt or ("best for this project" in lower_prompt or "specific reasons" in lower_prompt):
        return {
            "result": [
                "The architecture aligns with the project's core functional and quality requirements",
                "It reduces system coupling and improves maintainability for ongoing changes",
                "It provides a better path for scaling and operational resilience"
            ]
        }

    if "choose best architecture" in lower_prompt or "return only one architecture name" in lower_prompt or "single best architecture" in lower_prompt:
        if any(k in lower_prompt for k in ["real-time", "event", "stream"]):
            return {"result": "Event-Driven"}
        if any(k in lower_prompt for k in ["scale", "scalability", "millions", "distributed"]):
            return {"result": "Microservices"}
        if any(k in lower_prompt for k in ["simple", "mvp", "single deployment"]):
            return {"result": "Monolithic"}
        return {"result": "Microservices"}

    return {"result": "Monolithic"}


# ==============================
# PROMPT UTILITIES
# ==============================
def build_json_prompt(
    task: str,
    objective: str,
    input_payload: Dict[str, Any],
    output_schema: Dict[str, Any],
    example_output: Any
) -> str:
    """Compact prompt template; keeps <INPUT_JSON> tags for fallback parsing."""
    return (
        f"TASK: {task}\n"
        f"Objective: {objective}\n"
        f"Return ONLY valid JSON matching the example structure. No markdown. No prose.\n\n"
        f"<INPUT_JSON>{json.dumps(input_payload, ensure_ascii=False)}</INPUT_JSON>\n\n"
        f"Example output: {json.dumps(example_output, ensure_ascii=False)}"
    )


def extract_task_from_prompt(prompt: str) -> Optional[str]:
    match = re.search(r"TASK:\s*([A-Z0-9_\-]+)", prompt)
    return match.group(1).strip() if match else None


def extract_input_json_from_prompt(prompt: str) -> Dict[str, Any]:
    match = re.search(r"<INPUT_JSON>\s*([\s\S]*?)\s*</INPUT_JSON>", prompt)
    if not match:
        return {}
    try:
        parsed = json.loads(match.group(1).strip())
        return parsed if isinstance(parsed, dict) else {}
    except Exception:
        return {}


def build_classification_prompt(text: str) -> str:
    return build_json_prompt(
        task="CLASSIFICATION",
        objective="Classify requirement as FR, NFR, or MIXED with confidence and subcategory.",
        input_payload={"requirement": text},
        output_schema={
            "classification": "FR | NFR | MIXED",
            "confidence": 0.0,
            "subcategory": ["string"],
            "justification": "string",
            "quality_issues": ["string"]
        },
        example_output={
            "classification": "NFR",
            "confidence": 0.92,
            "subcategory": ["Performance"],
            "justification": "The requirement constrains latency and throughput.",
            "quality_issues": ["Missing explicit actor"]
        }
    )


def build_quality_prompt(text: str) -> str:
    return build_json_prompt(
        task="QUALITY_ANALYSIS",
        objective="Apply IEEE 830 quality scoring and SMART rewrite for one requirement.",
        input_payload={"requirement": text},
        output_schema={
            "score": 0,
            "vagueness": False,
            "missing_elements": ["Actor", "Action", "Condition"],
            "rewritten_requirement": "string"
        },
        example_output={
            "score": 74,
            "vagueness": True,
            "missing_elements": ["Condition"],
            "rewritten_requirement": "The system shall authenticate users using JWT tokens within 150 ms for 95% of login requests."
        }
    )


def build_rewrite_prompt(text: str) -> str:
    return build_json_prompt(
        task="REWRITE_REQUIREMENT",
        objective="Rewrite requirement into SMART, testable wording.",
        input_payload={"requirement": text},
        output_schema={
            "rewritten_requirement": "string",
            "metrics_added": ["string"],
            "issues_fixed": ["string"]
        },
        example_output={
            "rewritten_requirement": "The system shall process user login within 2 seconds for 95% of requests under 1,000 concurrent users.",
            "metrics_added": ["response time <= 2s", "95th percentile", "1,000 concurrent users"],
            "issues_fixed": ["removed vagueness", "added measurable condition"]
        }
    )


def build_gap_prompt(requirements: List[str]) -> str:
    return build_json_prompt(
        task="GAP_ANALYSIS",
        objective="Identify missing requirement categories. A gap exists when NONE of the requirements address a category. Check: Security (auth, encryption, access control), Performance (latency, throughput, scalability), Reliability (uptime, failover, backup), Usability (accessibility, onboarding, UX), Compliance (GDPR, HIPAA, audit trails). Only report genuinely missing categories.",
        input_payload={"requirements": requirements},
        output_schema={
            "gaps": [
                {
                    "gap": "string",
                    "severity": "Critical | High | Medium | Low",
                    "suggestion": "string"
                }
            ]
        },
        example_output={
            "gaps": [
                {
                    "gap": "No security requirements found",
                    "severity": "Critical",
                    "suggestion": "Add requirements for authentication, authorization, and data encryption."
                }
            ]
        }
    )


def build_traceability_prompt(requirements: List[str], architecture_components: List[str]) -> str:
    return build_json_prompt(
        task="TRACEABILITY_MATRIX",
        objective="Map each requirement to the architecture components that implement it. A requirement is 'untraced' if no component handles it. A component is 'high density' if it handles 3+ requirements. Use the actual requirement text and component names provided.",
        input_payload={
            "requirements": requirements,
            "architecture_components": architecture_components
        },
        output_schema={
            "matrix": [{"requirement": "string", "components": ["string"]}],
            "untraced_requirements": ["string"],
            "high_density_components": [{"component": "string", "requirement_count": 0}]
        },
        example_output={
            "matrix": [
                {
                    "requirement": "Users can log in with email and password",
                    "components": ["API Gateway", "Auth Service"]
                }
            ],
            "untraced_requirements": [],
            "high_density_components": [
                {
                    "component": "API Gateway",
                    "requirement_count": 4
                }
            ]
        }
    )


def build_risk_prompt(project_description: str, requirements: List[str]) -> str:
    return build_json_prompt(
        task="RISK_ASSUMPTION",
        objective="Identify 3-8 delivery risks and implicit assumptions. Risk=something that could go wrong (external dependency failure, performance bottleneck, scope creep). Assumption=unstated belief the requirements depend on (e.g. users have internet, third-party API available). Rate severity by business impact.",
        input_payload={
            "project_description": project_description,
            "requirements": requirements
        },
        output_schema={
            "items": [
                {
                    "type": "Risk | Assumption",
                    "description": "string",
                    "severity": "Critical | High | Medium | Low",
                    "mitigation": "string"
                }
            ]
        },
        example_output={
            "items": [
                {
                    "type": "Risk",
                    "description": "External payment provider availability may impact checkout success.",
                    "severity": "High",
                    "mitigation": "Implement retries, circuit breaker, and fallback payment options."
                }
            ]
        }
    )


def build_complexity_prompt(requirements: List[str]) -> str:
    return build_json_prompt(
        task="COMPLEXITY_ESTIMATION",
        objective="Estimate project size. function_points: IFPUG count (inputs+outputs+inquiries+files+interfaces). story_points: total across all requirements using Fibonacci scale. effort_estimate_weeks: for a team of 3-5 developers. top_complex_requirements: pick the 5 hardest to implement.",
        input_payload={"requirements": requirements},
        output_schema={
            "function_points": 0,
            "story_points": 0,
            "effort_estimate_weeks": 0,
            "top_complex_requirements": ["string"]
        },
        example_output={
            "function_points": 42,
            "story_points": 55,
            "effort_estimate_weeks": 5,
            "top_complex_requirements": [
                "System shall process fraud detection in real-time with p95 latency under 100 ms"
            ]
        }
    )


def build_novelty_prompt(project_description: str, requirements: List[str], domain: str) -> str:
    return build_json_prompt(
        task="NOVELTY_ASSESSMENT",
        objective="Rate project novelty 0-100. technical: how unusual is the tech stack/integration complexity. domain: how niche or specialized is the business domain. approach: how unconventional is the solution design. score: weighted average. Conventional=0-25, Incremental=26-50, Moderately Novel=51-75, Novel=76-100.",
        input_payload={
            "project_description": project_description,
            "domain": domain,
            "requirements": requirements
        },
        output_schema={
            "score": 0,
            "category": "Conventional | Incremental | Moderately Novel | Novel",
            "breakdown": {
                "technical": 0,
                "domain": 0,
                "approach": 0
            },
            "reasoning": "string"
        },
        example_output={
            "score": 72,
            "category": "Novel",
            "breakdown": {
                "technical": 78,
                "domain": 66,
                "approach": 72
            },
            "reasoning": "Combines event-driven architecture with domain-specific constraints and measurable quality targets."
        }
    )


def build_combined_analysis_prompt(text: str) -> str:
    """Single-shot prompt combining classification and quality analysis for one requirement."""
    return (
        "Classify and score this software requirement. Return ONLY valid JSON.\n\n"
        f"Requirement: {json.dumps(text)}\n\n"
        "Rules:\n"
        "- classification: FR=user action/business logic, NFR=quality attribute (performance/security/usability/reliability), MIXED=both\n"
        "- confidence: 0.0-1.0 how certain the classification is\n"
        "- subcategory: specific category like Security, Performance, Usability, Data, Authentication, Reporting\n"
        "- score: 0-100 per IEEE 830 (clear, testable, complete, consistent, traceable). Deduct 20 for vague terms (fast/easy/simple/robust/seamless), 15 per missing element (Actor/Action/Condition/Metric)\n"
        "- vagueness: true if vague terms present\n"
        "- missing_elements: from [Actor, Action, Condition, Metric] — only list what is missing\n"
        "- rewritten_requirement: Specific, Measurable, Achievable, Relevant, Time-bound rewrite\n"
        "- priority: High=security/core/data-integrity, Medium=usability/reporting, Low=nice-to-have\n"
        "- entities: actors or systems mentioned\n"
        "- processes: action verbs\n"
        "- data_stores: databases or storage implied\n\n"
        "Example output:\n"
        '{"classification":"NFR","confidence":0.92,"subcategory":["Performance"],'
        '"justification":"Constrains response latency under load.","score":74,"vagueness":true,'
        '"missing_elements":["Condition"],"rewritten_requirement":"The system shall respond to API requests within 200ms for 95% of requests under 500 concurrent users.",'
        '"priority":"High","entities":["System"],'
        '"processes":["respond"],"data_stores":[]}'
    )


# ==============================
# UNIFIED LLM ENGINE
# ==============================


# ==============================
# OLLAMA LOCAL LLM (fallback)
# ==============================
async def _pull_ollama_model_bg():
    """Pull the Ollama model in the background. Retries until Ollama is reachable."""
    global _ollama_ready
    import httpx
    import json
    delay = 10
    attempt = 0
    while True:
        attempt += 1
        try:
            async with httpx.AsyncClient(timeout=15) as client:
                ping = await client.get(f"{OLLAMA_URL}/api/tags")
                if ping.status_code != 200:
                    raise RuntimeError(f"Ollama ping returned {ping.status_code}")

                data = ping.json()
                tags = []
                if isinstance(data, list):
                    tags = data
                elif isinstance(data, dict):
                    tags = data.get("models") or data.get("tags") or []
                already_pulled = any(
                    m.get("name", "").startswith(OLLAMA_MODEL.split(":")[0])
                    for m in tags
                )
                if already_pulled:
                    logger.info("Ollama model '%s' already available.", OLLAMA_MODEL)
                    _ollama_ready = True
                    return

            logger.info("Pulling Ollama model '%s' — this may take a few minutes (4.7GB)...", OLLAMA_MODEL)
            async with httpx.AsyncClient(timeout=3600) as client:
                async with client.stream(
                    "POST",
                    f"{OLLAMA_URL}/api/pull",
                    json={"name": OLLAMA_MODEL},
                ) as resp:
                    last_status = None
                    async for line in resp.aiter_lines():
                        try:
                            data = json.loads(line)
                            status = data.get("status", "")
                            if "percent" in data:
                                # Progress update
                                logger.info("Ollama pull: %s (%.1f%%)", status, data.get("completed", 0) / max(data.get("total", 1), 1) * 100)
                            if status == "success":
                                logger.info("Ollama model pull completed successfully")
                                break
                        except json.JSONDecodeError:
                            pass
            
            # Verify model is actually available after pull completes
            async with httpx.AsyncClient(timeout=15) as client:
                verify = await client.get(f"{OLLAMA_URL}/api/tags")
                data = verify.json()
                tags = []
                if isinstance(data, list):
                    tags = data
                elif isinstance(data, dict):
                    tags = data.get("models") or data.get("tags") or []
                if any(m.get("name", "").startswith(OLLAMA_MODEL.split(":")[0]) for m in tags):
                    _ollama_ready = True
                    logger.info("Ollama model '%s' ready and verified.", OLLAMA_MODEL)
                    return
                else:
                    raise RuntimeError("Model pull completed but model not found in tags")
        except Exception as exc:
            logger.warning("Ollama not yet reachable (attempt %d): %s — retrying in %ds", attempt, exc, delay)
            await asyncio.sleep(delay)
            delay = min(delay * 2, 120)  # cap at 2 min


@app.on_event("startup")
async def startup_event():
    global _ollama_semaphore
    # Limit concurrent Ollama calls
    _ollama_semaphore = asyncio.Semaphore(8)
    asyncio.create_task(_pull_ollama_model_bg())


async def call_ollama(prompt: str) -> str:
    """Send a prompt to the local Ollama model and return the text response."""
    import httpx
    sem = _ollama_semaphore or asyncio.Semaphore(4)
    estimated_tokens = len(prompt) // 3  # rough char-to-token ratio
    num_ctx = 2048 if estimated_tokens > 600 else 1024
    async with sem:
        payload = {
            "model": OLLAMA_MODEL,
            "messages": [{"role": "user", "content": prompt}],
            "stream": False,
            "keep_alive": -1,
            "options": {
                "temperature": 0.0,   # fully deterministic — faster + more consistent JSON
                "num_ctx": num_ctx,
                "num_predict": 512,   # all JSON responses fit in 512 tokens; prevents runaway
            },
        }
        async with httpx.AsyncClient(timeout=OLLAMA_TIMEOUT_SECONDS) as client:
            resp = await client.post(
                f"{OLLAMA_URL}/v1/chat/completions",
                json=payload,
            )
            resp.raise_for_status()
            data = resp.json()
            return data["choices"][0]["message"]["content"]


def fallback_from_prompt(prompt: str) -> Dict[str, Any]:
    task = extract_task_from_prompt(prompt)
    payload = extract_input_json_from_prompt(prompt)

    if task == "CLASSIFICATION":
        return fallback_classification(payload.get("requirement", ""))

    if task == "QUALITY_ANALYSIS":
        return fallback_quality_analysis(payload.get("requirement", ""))

    if task == "REWRITE_REQUIREMENT":
        req = payload.get("requirement", "")
        quality = fallback_quality_analysis(req)
        return {
            "rewritten_requirement": quality["rewritten_requirement"],
            "metrics_added": [
                "response-time target",
                "concurrency/load target",
                "explicit trigger condition"
            ],
            "issues_fixed": [
                "added measurable criteria",
                "clarified actor/action/condition"
            ]
        }

    if task == "GAP_ANALYSIS":
        requirements = normalize_requirements(payload.get("requirements", []))
        return {"gaps": fallback_gap_analysis(requirements)}

    if task == "TRACEABILITY_MATRIX":
        requirements = normalize_requirements(payload.get("requirements", []))
        components = normalize_requirements(payload.get("architecture_components", []))
        return fallback_traceability(requirements, components)

    if task == "RISK_ASSUMPTION":
        requirements = normalize_requirements(payload.get("requirements", []))
        return {"items": fallback_risk_assumptions(payload.get("project_description", ""), requirements)}

    if task == "COMPLEXITY_ESTIMATION":
        requirements = normalize_requirements(payload.get("requirements", []))
        return fallback_complexity(requirements)

    if task == "NOVELTY_ASSESSMENT":
        requirements = normalize_requirements(payload.get("requirements", []))
        return fallback_novelty(
            payload.get("project_description", ""),
            requirements,
            payload.get("domain", "General")
        )

    # Backward compatibility for /llm/generate free prompts
    return fallback_custom_generate(prompt)


async def call_llm(prompt: str) -> Dict[str, Any]:
    """
    Unified LLM entrypoint (required by architecture rules).
    Uses local Ollama model (llama3.1:8b) with heuristic fallback.
    Always returns JSON-compatible dict.
    """
    clean_prompt = (prompt or "").strip()
    if not clean_prompt:
        return {"error": "empty_prompt"}

    last_error: Optional[str] = None

    # --- Tier 1: local Ollama model (llama3.1:8b) ---
    if _ollama_ready:
        try:
            raw = await asyncio.wait_for(call_ollama(clean_prompt), timeout=OLLAMA_TIMEOUT_SECONDS)
            parsed = extract_json_content(raw)
            if isinstance(parsed, dict):
                return parsed
            return {"result": parsed}
        except Exception as exc:
            last_error = str(exc)
            logger.warning("Ollama call failed: %s", exc)

    fallback = fallback_from_prompt(clean_prompt)

    # --- Tier 2: heuristic fallback ---
    if last_error and isinstance(fallback, dict):
        fallback.setdefault("_fallback_reason", last_error)
    return fallback if isinstance(fallback, dict) else {"result": fallback}


# ==============================
# MODULE SERVICE FUNCTIONS
# ==============================
async def classify_requirement_llm(text: str) -> Dict[str, Any]:
    fallback = fallback_classification(text)
    response = await call_llm(build_classification_prompt(text))
    payload = unwrap_result(response)

    if not isinstance(payload, dict):
        return fallback

    classification = str(payload.get("classification", fallback["classification"])).upper().strip()
    if classification not in {"FR", "NFR", "MIXED"}:
        classification = fallback["classification"]

    confidence = payload.get("confidence", fallback["confidence"])
    try:
        confidence = max(0.0, min(1.0, float(confidence)))
    except Exception:
        confidence = float(fallback["confidence"])

    return {
        "classification": classification,
        "confidence": confidence,
        "subcategory": normalize_string_list(payload.get("subcategory", fallback.get("subcategory", []))),
        "justification": str(payload.get("justification", fallback["justification"])).strip(),
        "quality_issues": normalize_string_list(payload.get("quality_issues", []))
    }


async def quality_analysis_llm(text: str) -> Dict[str, Any]:
    fallback = fallback_quality_analysis(text)
    response = await call_llm(build_quality_prompt(text))
    payload = unwrap_result(response)

    if not isinstance(payload, dict):
        return fallback

    rewritten = str(payload.get("rewritten_requirement", fallback["rewritten_requirement"])).strip() or fallback["rewritten_requirement"]

    return {
        "score": clamp_int(payload.get("score"), 0, 100, fallback["score"]),
        "vagueness": normalize_bool(payload.get("vagueness"), fallback["vagueness"]),
        "missing_elements": normalize_string_list(payload.get("missing_elements", fallback["missing_elements"])),
        "rewritten_requirement": sanitize_rewritten_requirement(rewritten)
    }


async def run_gap_analysis(requirements: List[str]) -> List[Dict[str, str]]:
    normalized = normalize_requirements(requirements)
    fallback = fallback_gap_analysis(normalized)
    response = await call_llm(build_gap_prompt(normalized))
    payload = unwrap_result(response)

    if isinstance(payload, dict):
        payload = payload.get("gaps")

    if not isinstance(payload, list):
        return fallback

    gaps = []
    for item in payload:
        if not isinstance(item, dict):
            continue
        gap_text = str(item.get("gap", "")).strip()
        suggestion = str(item.get("suggestion", "")).strip()
        if not gap_text or not suggestion:
            continue
        gaps.append({
            "gap": gap_text,
            "severity": normalize_severity(item.get("severity"), "Medium"),
            "suggestion": suggestion
        })

    return gaps or fallback


async def run_traceability(requirements: List[str], architecture_components: List[str]) -> Dict[str, Any]:
    reqs = normalize_requirements(requirements)
    comps = normalize_requirements(architecture_components)
    fallback = fallback_traceability(reqs, comps)

    response = await call_llm(build_traceability_prompt(reqs, comps))
    payload = unwrap_result(response)

    if not isinstance(payload, dict):
        return fallback

    matrix_raw = payload.get("matrix")
    if not isinstance(matrix_raw, list):
        return fallback

    matrix = []
    for row in matrix_raw:
        if not isinstance(row, dict):
            continue
        requirement = str(row.get("requirement", "")).strip()
        components = normalize_string_list(row.get("components", []))
        if requirement:
            matrix.append({"requirement": requirement, "components": components})

    untraced = normalize_string_list(payload.get("untraced_requirements", []))

    density_raw = payload.get("high_density_components", [])
    high_density = []
    if isinstance(density_raw, list):
        for item in density_raw:
            if not isinstance(item, dict):
                continue
            component = str(item.get("component", "")).strip()
            count = clamp_int(item.get("requirement_count"), 0, 10_000, 0)
            if component:
                high_density.append({"component": component, "requirement_count": count})

    if not matrix:
        return fallback

    return {
        "matrix": matrix,
        "untraced_requirements": untraced,
        "high_density_components": high_density
    }


async def run_risk_assumptions(project_description: str, requirements: List[str]) -> List[Dict[str, str]]:
    reqs = normalize_requirements(requirements)
    fallback = fallback_risk_assumptions(project_description, reqs)

    response = await call_llm(build_risk_prompt(project_description, reqs))
    payload = unwrap_result(response)

    if isinstance(payload, dict):
        payload = payload.get("items")

    if not isinstance(payload, list):
        return fallback

    items = []
    for row in payload:
        if not isinstance(row, dict):
            continue
        item_type = str(row.get("type", "")).strip().title()
        if item_type not in {"Risk", "Assumption"}:
            continue
        description = str(row.get("description", "")).strip()
        mitigation = str(row.get("mitigation", "")).strip()
        if not description or not mitigation:
            continue
        items.append({
            "type": item_type,
            "description": description,
            "severity": normalize_severity(row.get("severity"), "Medium"),
            "mitigation": mitigation
        })

    return items or fallback


async def run_complexity(requirements: List[str]) -> Dict[str, Any]:
    reqs = normalize_requirements(requirements)
    fallback = fallback_complexity(reqs)

    response = await call_llm(build_complexity_prompt(reqs))
    payload = unwrap_result(response)

    if not isinstance(payload, dict):
        return fallback

    return {
        "function_points": clamp_int(payload.get("function_points"), 0, 100_000, fallback["function_points"]),
        "story_points": clamp_int(payload.get("story_points"), 0, 100_000, fallback["story_points"]),
        "effort_estimate_weeks": clamp_int(payload.get("effort_estimate_weeks"), 0, 10_000, fallback["effort_estimate_weeks"]),
        "top_complex_requirements": normalize_string_list(payload.get("top_complex_requirements", fallback["top_complex_requirements"]))[:5]
    }


async def run_novelty(project_description: str, requirements: List[str], domain: str) -> Dict[str, Any]:
    reqs = normalize_requirements(requirements)
    fallback = fallback_novelty(project_description, reqs, domain)

    response = await call_llm(build_novelty_prompt(project_description, reqs, domain))
    payload = unwrap_result(response)

    if not isinstance(payload, dict):
        return fallback

    breakdown = payload.get("breakdown", {})
    if not isinstance(breakdown, dict):
        breakdown = {}

    return {
        "score": clamp_int(payload.get("score"), 0, 100, fallback["score"]),
        "category": str(payload.get("category", fallback["category"])).strip() or fallback["category"],
        "breakdown": {
            "technical": clamp_int(breakdown.get("technical"), 0, 100, fallback["breakdown"]["technical"]),
            "domain": clamp_int(breakdown.get("domain"), 0, 100, fallback["breakdown"]["domain"]),
            "approach": clamp_int(breakdown.get("approach"), 0, 100, fallback["breakdown"]["approach"])
        },
        "reasoning": str(payload.get("reasoning", fallback["reasoning"])).strip() or fallback["reasoning"]
    }


# ==============================
# ENTITY / PROCESS / DATA STORE EXTRACTION (HEURISTIC FALLBACKS)
# ==============================
def fallback_priority(requirement: str, classification: str) -> str:
    lower = requirement.lower()
    if any(k in lower for k in ["security", "auth", "encrypt", "password", "token", "compliance", "gdpr", "hipaa"]):
        return "High"
    if any(k in lower for k in ["shall", "must", "critical", "real-time", "transaction", "data integrity"]):
        return "High"
    if classification == "NFR" and any(k in lower for k in ["performance", "availability", "reliability"]):
        return "High"
    if any(k in lower for k in ["report", "dashboard", "display", "view", "notification", "email"]):
        return "Medium"
    if any(k in lower for k in ["nice", "optional", "future", "may", "could", "color", "theme"]):
        return "Low"
    return "Medium"


def fallback_extract_entities(requirement: str) -> List[str]:
    lower = requirement.lower()
    entities = []
    entity_map = {
        "admin": "Admin",
        "administrator": "Administrator",
        "user": "User",
        "customer": "Customer",
        "client": "Client",
        "operator": "Operator",
        "doctor": "Doctor",
        "patient": "Patient",
        "manager": "Manager",
        "system": "System",
        "service": "External Service",
        "api": "External API",
        "payment gateway": "Payment Gateway",
        "third-party": "Third-Party Service",
        "database": "Database",
    }
    for keyword, entity in entity_map.items():
        if keyword in lower and entity not in entities:
            entities.append(entity)
    return entities or ["System"]


def fallback_extract_processes(requirement: str) -> List[str]:
    lower = requirement.lower()
    process_keywords = [
        "authenticate", "authorize", "login", "register", "create", "update", "delete",
        "process", "generate", "send", "receive", "store", "retrieve", "validate",
        "encrypt", "decrypt", "export", "import", "notify", "schedule", "monitor",
        "analyze", "classify", "filter", "search", "sort", "calculate", "verify",
        "upload", "download", "backup", "restore", "log", "audit", "synchronize"
    ]
    found = []
    for proc in process_keywords:
        if proc in lower:
            found.append(proc.capitalize())
    return found or ["Process"]


def fallback_extract_data_stores(requirement: str) -> List[str]:
    lower = requirement.lower()
    stores = []
    store_map = {
        "user": "User Database",
        "patient": "Patient Records",
        "audit": "Audit Log",
        "log": "System Logs",
        "session": "Session Store",
        "config": "Configuration Store",
        "report": "Report Store",
        "document": "Document Store",
        "file": "File Storage",
        "cache": "Cache Store",
        "credential": "Credential Store",
        "token": "Token Store",
        "notification": "Notification Queue",
        "event": "Event Store",
        "transaction": "Transaction Log",
    }
    for keyword, store in store_map.items():
        if keyword in lower and store not in stores:
            stores.append(store)
    return stores


async def analyze_requirement_pair(req_text: str) -> Tuple[str, Dict[str, Any], Dict[str, Any]]:
    """
    Run classification + quality analysis for one requirement.
    Uses a single combined LLM call (2→1), halving the number of round-trips.
    Falls back to separate heuristics if the LLM returns an unusable response.
    """
    cls_fallback = fallback_classification(req_text)
    qual_fallback = fallback_quality_analysis(req_text)

    combined_prompt = build_combined_analysis_prompt(req_text)

    # Try Ollama first (primary local LLM)
    raw: Optional[str] = None
    if _ollama_ready:
        try:
            raw = await asyncio.wait_for(call_ollama(combined_prompt), timeout=OLLAMA_TIMEOUT_SECONDS)
        except Exception as exc:
            logger.warning("Combined analysis: Ollama failed: %s", exc)

    if raw is None:
        qual_fallback["priority"] = fallback_priority(req_text, cls_fallback["classification"])
        qual_fallback["entities"] = fallback_extract_entities(req_text)
        qual_fallback["processes"] = fallback_extract_processes(req_text)
        qual_fallback["data_stores"] = fallback_extract_data_stores(req_text)
        return req_text, cls_fallback, qual_fallback

    # Parse the single combined response
    try:
        payload = extract_json_content(raw)
        payload = unwrap_result(payload) if isinstance(payload, dict) else payload
        if not isinstance(payload, dict) or "classification" not in payload:
            raise ValueError("Missing 'classification' key")
    except Exception as exc:
        logger.warning("Combined analysis: JSON parse failed (%s) — using heuristics", exc)
        qual_fallback["priority"] = fallback_priority(req_text, cls_fallback["classification"])
        qual_fallback["entities"] = fallback_extract_entities(req_text)
        qual_fallback["processes"] = fallback_extract_processes(req_text)
        qual_fallback["data_stores"] = fallback_extract_data_stores(req_text)
        return req_text, cls_fallback, qual_fallback

    # --- Build classification result ---
    classification = str(payload.get("classification", cls_fallback["classification"])).upper().strip()
    if classification not in {"FR", "NFR", "MIXED"}:
        classification = cls_fallback["classification"]

    confidence = payload.get("confidence", cls_fallback["confidence"])
    try:
        confidence = max(0.0, min(1.0, float(confidence)))
    except Exception:
        confidence = float(cls_fallback["confidence"])

    cls_result: Dict[str, Any] = {
        "classification": classification,
        "confidence": confidence,
        "subcategory": normalize_string_list(payload.get("subcategory", cls_fallback.get("subcategory", []))),
        "justification": str(payload.get("justification", cls_fallback["justification"])).strip(),
        "quality_issues": normalize_string_list(payload.get("quality_issues", [])),
    }

    # --- Build quality result ---
    rewritten = str(payload.get("rewritten_requirement", qual_fallback["rewritten_requirement"])).strip()
    if not rewritten:
        rewritten = qual_fallback["rewritten_requirement"]

    # --- Extract priority ---
    priority = str(payload.get("priority", "")).strip().title()
    if priority not in {"High", "Medium", "Low"}:
        priority = fallback_priority(req_text, classification)

    qual_result: Dict[str, Any] = {
        "score": clamp_int(payload.get("score"), 0, 100, qual_fallback["score"]),
        "vagueness": normalize_bool(payload.get("vagueness"), qual_fallback["vagueness"]),
        "missing_elements": normalize_string_list(payload.get("missing_elements", qual_fallback["missing_elements"])),
        "rewritten_requirement": sanitize_rewritten_requirement(rewritten),
        "priority": priority,
        "entities": normalize_string_list(payload.get("entities", fallback_extract_entities(req_text))),
        "processes": normalize_string_list(payload.get("processes", fallback_extract_processes(req_text))),
        "data_stores": normalize_string_list(payload.get("data_stores", fallback_extract_data_stores(req_text))),
    }

    return req_text, cls_result, qual_result


# ==============================
# API: ANALYZE (BACKWARD COMPAT)
# ==============================
@app.post("/analyze")
async def analyze_requirements(request: AnalysisRequest):
    try:
        requirements = normalize_requirements(request.requirements)
        analysis_result = {
            "project_id": request.project_id,
            "analyzed_at": datetime.utcnow(),
            "classifications": [],
            "overall_quality": 0,
            "quality_summary": {
                "ambiguous_count": 0,
                "high_risk": 0
            }
        }

        total_score = 0

        analyzed = await asyncio.gather(*(analyze_requirement_pair(req) for req in requirements))

        for idx, (req_text, cls, quality) in enumerate(analyzed):

            quality_issues = list(dict.fromkeys(cls.get("quality_issues", [])))
            if quality["vagueness"] and "Ambiguous" not in quality_issues:
                quality_issues.append("Ambiguous")
            if quality["missing_elements"]:
                quality_issues.append(f"Missing: {', '.join(quality['missing_elements'])}")

            if quality["vagueness"]:
                analysis_result["quality_summary"]["ambiguous_count"] += 1
            if quality["score"] < 60:
                analysis_result["quality_summary"]["high_risk"] += 1

            item = {
                "requirement_index": idx,
                "text": req_text,

                # Backward-compatible fields
                "classification": cls["classification"],
                "confidence": cls["confidence"],
                "subcategories": cls["subcategory"],

                # Enhanced fields
                "justification": cls.get("justification", ""),
                "quality_issues": quality_issues,
                "improved_requirement": quality["rewritten_requirement"],
                "rewritten_requirement": quality["rewritten_requirement"],
                "missing_elements": quality["missing_elements"],

                # Quality module output
                "is_vague": quality["vagueness"],
                "quality_score": quality["score"],

                # New: priority & extraction fields
                "priority": quality.get("priority", "Medium"),
                "entities": quality.get("entities", []),
                "processes": quality.get("processes", []),
                "data_stores": quality.get("data_stores", []),
            }

            analysis_result["classifications"].append(item)
            total_score += quality["score"]

        analysis_result["overall_quality"] = total_score // len(requirements) if requirements else 0

        result = analysis_collection.insert_one(analysis_result)

        return {
            "analysis_id": str(result.inserted_id),
            "project_id": request.project_id,
            "status": "completed",
            "classifications_count": len(analysis_result["classifications"]),
            "overall_quality": analysis_result["overall_quality"]
        }

    except Exception as exc:
        logger.exception("Analyze endpoint failed")
        raise HTTPException(status_code=500, detail=str(exc))


# ==============================
# API: GET ANALYSIS
# ==============================
@app.get("/analysis/{project_id}")
async def get_analysis(project_id: str):
    try:
        analysis = analysis_collection.find_one({"project_id": project_id}, sort=[("analyzed_at", -1)])
        if not analysis:
            raise HTTPException(status_code=404, detail="Analysis not found")

        analysis["_id"] = str(analysis["_id"])
        analyzed_at = analysis.get("analyzed_at")
        if isinstance(analyzed_at, datetime):
            analysis["analyzed_at"] = analyzed_at.isoformat()

        return analysis

    except HTTPException:
        raise
    except Exception as exc:
        logger.exception("Get analysis failed")
        raise HTTPException(status_code=500, detail=str(exc))


# ==============================
# API: REWRITE
# ==============================
@app.post("/rewrite-requirement")
async def rewrite_requirement(payload: RewriteRequest):
    try:
        requirement_text = payload.requirement_text.strip()
        if not requirement_text:
            raise HTTPException(status_code=400, detail="requirement_text is required")

        response = await call_llm(build_rewrite_prompt(requirement_text))
        parsed = unwrap_result(response)

        fallback_quality = fallback_quality_analysis(requirement_text)
        rewritten = fallback_quality["rewritten_requirement"]

        if isinstance(parsed, dict):
            rewritten = str(parsed.get("rewritten_requirement", rewritten)).strip() or rewritten
        rewritten = sanitize_rewritten_requirement(rewritten)

        return {
            "original": requirement_text,
            "rewritten": rewritten,
            "improved_specificity": True
        }

    except HTTPException:
        raise
    except Exception as exc:
        logger.exception("Rewrite endpoint failed")
        raise HTTPException(status_code=500, detail=str(exc))


# ==============================
# MODULE ENDPOINTS
# ==============================
@app.post("/quality/enhanced")
async def enhanced_quality_analysis(request: EnhancedQualityRequest):
    try:
        requirements = normalize_requirements(request.requirements)
        rows = []
        total = 0

        quality_rows = await asyncio.gather(*(quality_analysis_llm(req) for req in requirements))

        for req, quality in zip(requirements, quality_rows):
            rows.append({
                "requirement": req,
                "score": quality["score"],
                "vagueness": quality["vagueness"],
                "missing_elements": quality["missing_elements"],
                "rewritten_requirement": quality["rewritten_requirement"]
            })
            total += quality["score"]

        return {
            "project_id": request.project_id,
            "requirements": rows,
            "overall_quality": total // len(rows) if rows else 0
        }

    except Exception as exc:
        logger.exception("Enhanced quality endpoint failed")
        raise HTTPException(status_code=500, detail=str(exc))


@app.post("/gap-analysis")
async def requirement_gap_analysis(request: GapAnalysisRequest):
    try:
        requirements = normalize_requirements(request.requirements)
        gaps = await run_gap_analysis(requirements)
        return {"gaps": gaps}
    except Exception as exc:
        logger.exception("Gap analysis endpoint failed")
        raise HTTPException(status_code=500, detail=str(exc))


@app.post("/traceability-matrix")
async def traceability_matrix(request: TraceabilityRequest):
    try:
        requirements = normalize_requirements(request.requirements)
        components = normalize_requirements(request.architecture_components)
        matrix = await run_traceability(requirements, components)
        return matrix
    except Exception as exc:
        logger.exception("Traceability endpoint failed")
        raise HTTPException(status_code=500, detail=str(exc))


@app.post("/risk-assumptions")
async def risk_assumptions(request: RiskAssumptionRequest):
    try:
        requirements = normalize_requirements(request.requirements)
        items = await run_risk_assumptions(request.project_description or "", requirements)
        return {"items": items}
    except Exception as exc:
        logger.exception("Risk/assumption endpoint failed")
        raise HTTPException(status_code=500, detail=str(exc))


@app.post("/complexity-estimation")
async def complexity_estimation(request: ComplexityRequest):
    try:
        requirements = normalize_requirements(request.requirements)
        result = await run_complexity(requirements)
        return result
    except Exception as exc:
        logger.exception("Complexity endpoint failed")
        raise HTTPException(status_code=500, detail=str(exc))


@app.post("/novelty-assessment")
async def novelty_assessment(request: NoveltyRequest):
    try:
        requirements = normalize_requirements(request.requirements)
        result = await run_novelty(request.project_description or "", requirements, request.domain or "General")
        return result
    except Exception as exc:
        logger.exception("Novelty endpoint failed")
        raise HTTPException(status_code=500, detail=str(exc))


# ==============================
# API: CUSTOM PROMPT GENERATE
# ==============================
@app.post("/llm/generate")
async def generate_from_prompt(payload: PromptRequest):
    prompt = (payload.prompt or "").strip()
    if not prompt:
        raise HTTPException(status_code=400, detail="prompt is required")

    response = await call_llm(prompt)
    parsed = unwrap_result(response)

    if isinstance(parsed, (dict, list, str, int, float, bool)):
        return {"result": parsed}

    return {"result": str(parsed)}


class ChatRequest(BaseModel):
    question: str
    project_context: Optional[str] = None
    project_id: Optional[str] = None


class DiagramRequest(BaseModel):
    prompt: str
    diagram_type: str = "component"


DIAGRAM_TYPE_EXAMPLES = {
    "component": (
        "@startuml\n"
        "component \"API Gateway\" as GW\n"
        "component \"Auth Service\" as AUTH\n"
        "component \"Data Service\" as DATA\n"
        "database \"Database\" as DB\n"
        "GW --> AUTH : authenticate\n"
        "GW --> DATA : request\n"
        "DATA --> DB : read/write\n"
        "@enduml"
    ),
    "sequence": (
        "@startuml\n"
        "actor User\n"
        "participant \"API Gateway\" as GW\n"
        "participant \"Auth Service\" as AUTH\n"
        "participant \"Service\" as SVC\n"
        "User -> GW : request\n"
        "GW -> AUTH : verify token\n"
        "AUTH --> GW : ok\n"
        "GW -> SVC : process\n"
        "SVC --> GW : result\n"
        "GW --> User : response\n"
        "@enduml"
    ),
    "dfd": (
        "@startuml\n"
        "actor \"End User\" as U\n"
        "rectangle \"System\" as SYS\n"
        "database \"Data Store\" as DS\n"
        "U --> SYS : input\n"
        "SYS --> DS : read/write\n"
        "SYS --> U : output\n"
        "@enduml"
    ),
    "usecase": (
        "@startuml\n"
        "actor User\n"
        "actor Admin\n"
        "usecase (Login) as UC1\n"
        "usecase (View Dashboard) as UC2\n"
        "usecase (Manage Users) as UC3\n"
        "User --> UC1\n"
        "User --> UC2\n"
        "Admin --> UC3\n"
        "@enduml"
    ),
    "er": (
        "@startuml\n"
        "entity User {\n"
        "  * id : UUID\n"
        "  --\n"
        "  name : String\n"
        "  email : String\n"
        "}\n"
        "entity Project {\n"
        "  * id : UUID\n"
        "  --\n"
        "  name : String\n"
        "  description : String\n"
        "}\n"
        "entity Requirement {\n"
        "  * id : UUID\n"
        "  --\n"
        "  text : String\n"
        "  type : String\n"
        "}\n"
        "User ||--o{ Project : creates\n"
        "Project ||--o{ Requirement : contains\n"
        "@enduml"
    ),
}


def fallback_diagram_code(prompt: str, diagram_type: str) -> str:
    """Generate a context-aware PlantUML skeleton when the LLM is unavailable."""
    example = DIAGRAM_TYPE_EXAMPLES.get(diagram_type, DIAGRAM_TYPE_EXAMPLES["component"])
    return example


def build_diagram_prompt(prompt: str, diagram_type: str) -> str:
    return (
        f"Generate a {diagram_type} diagram in PlantUML for this request:\n"
        f"{prompt}\n\n"
        f"Return ONLY JSON: {{\"plantuml_code\": \"@startuml ...your code... @enduml\"}}\n\n"
        f"Rules:\n"
        f"1. Must start with @startuml and end with @enduml.\n"
        f"2. Use descriptive node names and relationship labels specific to the request.\n"
        f"3. No prose, no markdown — only the JSON object."
    )


def _extract_plantuml(raw: str) -> Optional[str]:
    """Extract PlantUML code from LLM output — handles JSON-wrapped or plain-text responses."""
    if not raw:
        return None
    # Try JSON parsing first (LLM followed instructions)
    try:
        parsed = extract_json_content(raw)
        if isinstance(parsed, dict):
            code = parsed.get("plantuml_code") or parsed.get("result", "")
            if isinstance(code, str) and "@startuml" in code:
                return code.strip()
    except Exception:
        pass
    # Regex fallback: find @startuml...@enduml block anywhere in the raw text
    match = re.search(r"(@startuml[\s\S]*?@enduml)", raw, re.IGNORECASE)
    if match:
        return match.group(1).strip()
    return None


@app.post("/generate-diagram")
async def generate_diagram(request: DiagramRequest):
    try:
        prompt = (request.prompt or "").strip()
        diagram_type = (request.diagram_type or "component").strip().lower()

        if not prompt:
            raise HTTPException(status_code=422, detail="prompt is required")

        plantuml_code: Optional[str] = None
        llm_prompt = build_diagram_prompt(prompt, diagram_type)

        # Tier 1: Ollama (local LLM)
        if _ollama_ready:
            try:
                raw = await asyncio.wait_for(call_ollama(llm_prompt), timeout=OLLAMA_TIMEOUT_SECONDS)
                plantuml_code = _extract_plantuml(raw)
            except Exception as exc:
                logger.warning("Diagram Ollama call failed: %s", exc)

        # Tier 3: static fallback template
        if not plantuml_code:
            plantuml_code = fallback_diagram_code(prompt, diagram_type)

        return {"plantuml_code": plantuml_code, "diagram_type": diagram_type}

    except HTTPException:
        raise
    except Exception as exc:
        logger.exception("generate-diagram endpoint failed")
        raise HTTPException(status_code=500, detail=str(exc))



# ==============================
# API: CHAT
# ==============================
def build_chat_prompt(question: str, context: Optional[str]) -> str:
    base = "You are a software engineering assistant. Answer concisely about requirements, architecture, and quality analysis.\n\n"
    if context:
        base += f"Context:\n{context}\n\n"
    base += f"Question: {question}"
    return base


@app.post("/chat")
async def chat_endpoint(request: ChatRequest):
    try:
        global _gemini_disabled
        question = (request.question or "").strip()
        if not question:
            raise HTTPException(status_code=422, detail="question is required")

        context = request.project_context

        # Auto-fetch project context from MongoDB if a project_id was given
        if not context and request.project_id:
            try:
                from bson import ObjectId

                # Fetch project metadata (name, description, domain)
                try:
                    proj = projects_collection.find_one({"_id": ObjectId(request.project_id)})
                except Exception:
                    proj = None

                # Fetch latest analysis for classified requirements
                analysis = analysis_collection.find_one(
                    {"project_id": request.project_id},
                    sort=[("analyzed_at", -1)]
                )

                context_parts = []
                if proj:
                    context_parts.append(
                        f"Project: {proj.get('name', 'Unknown')}\n"
                        f"Domain: {proj.get('domain', 'General')}\n"
                        f"Architecture: {proj.get('proposed_architecture', 'Not specified')}\n"
                        f"Description: {proj.get('description', '')}"
                    )

                if analysis:
                    # The analysis stores items under "classifications" (not "requirements")
                    classifications = analysis.get("classifications", [])
                    if classifications:
                        req_lines = []
                        for r in classifications[:15]:
                            text = r.get("text", "") if isinstance(r, dict) else str(r)
                            category = r.get("classification", "") if isinstance(r, dict) else ""
                            score = r.get("quality_score", "")
                            vague = " [vague]" if r.get("is_vague") else ""
                            line = f"- [{category}]{vague} {text}"
                            if score != "":
                                line += f" (quality: {score}/100)"
                            req_lines.append(line)
                        context_parts.append("Requirements:\n" + "\n".join(req_lines))
                    overall = analysis.get("overall_quality")
                    if overall is not None:
                        context_parts.append(f"Overall quality score: {overall}/100")

                if context_parts:
                    context = "\n\n".join(context_parts)
            except Exception as ctx_exc:
                logger.warning("Could not fetch project context: %s", ctx_exc)

        response_text: Optional[str] = None

        # Use local Ollama model for chat
        if _ollama_ready:
            try:
                prompt = build_chat_prompt(question, context)
                response_text = await asyncio.wait_for(
                    call_ollama(prompt), timeout=OLLAMA_TIMEOUT_SECONDS
                )
                logger.info("Chat served by local Ollama model.")
            except Exception as exc:
                logger.warning("Chat Ollama call failed: %r", exc)

        if not response_text:
            response_text = (
                "I can help you understand your project requirements and architecture. "
                "Ask me about specific requirements, architectural decisions, quality issues, "
                "or best practices. (Local Ollama model is still loading — try again shortly.)"
            )

        return {"response": response_text}

    except HTTPException:
        raise
    except Exception as exc:
        logger.exception("Chat endpoint failed")
        raise HTTPException(status_code=500, detail=str(exc))


# ==============================
# HEALTH
# ==============================
@app.get("/health")
async def health():
    return {
        "status": "ok",
        "service": "llm",
        "llm_ready": True,
        "ollama_ready": _ollama_ready,
        "ollama_model": OLLAMA_MODEL,
        "ollama_url": OLLAMA_URL
    }


# ==============================
# MAIN
# ==============================
if __name__ == "__main__":
    import uvicorn

    uvicorn.run(app, host="0.0.0.0", port=8002)
