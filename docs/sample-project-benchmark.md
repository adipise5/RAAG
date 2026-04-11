# MediTrack Patient Portal — Benchmark Sample

## What this is

A realistic healthcare project input designed to exercise every analysis feature in RAAG. Copy-paste the values from `sample-project-benchmark.json` into the New Project form, or POST the JSON directly to the API.

---

## How to submit via the UI

1. Open [http://localhost:3000](http://localhost:3000)
2. Click **New Project**
3. Fill in the fields from the table below
4. Paste each requirement from the list one by one
5. Click **Create Project Analysis**

---

## Field values

| Field | Value |
|---|---|
| **Project Name** | MediTrack Patient Portal |
| **Description** | *(see JSON file)* |
| **Proposed Architecture** | Microservices |
| **Domain** | Healthcare |

---

## Requirements (15 total)

Deliberately includes a mix to stress-test every analysis dimension:

| # | Type | What to expect |
|---|---|---|
| 1 | FR | Well-formed: actor + action + condition + metric → high quality score |
| 2 | FR | Well-formed with measurable SLA |
| 3 | FR | EHR integration + performance constraint |
| 4 | NFR (Security) | Encryption spec — should classify NFR/Security |
| 5 | FR | Event-driven reminder flow |
| 6 | FR + NFR | Audit trail — expect MIXED classification |
| 7 | FR (vague) | **Intentionally vague** ("fast", "easy", "quickly") → low quality score, rewrite triggered |
| 8 | NFR (Compliance) | PCI-DSS — should flag Compliance subcategory |
| 9 | NFR (Security) | RBAC with measurable SLA on alerts |
| 10 | NFR (vague) | **Intentionally vague** ("reliable", "not go down too much") → lowest quality score |
| 11 | FR | FHIR R4 API integration spec |
| 12 | NFR (Reliability) | 99.9% uptime + 5k concurrent users + RTO ≤ 60s |
| 13 | NFR (Compliance) | HIPAA compliance report generation |
| 14 | NFR (Security + Compliance) | Encryption at rest + 7-year retention |
| 15 | FR (vague) | **Intentionally vague** — no metric, no condition |

---

## What good analysis output looks like

- **Overall quality score**: ~62–72 (pulled down by the 3 vague requirements)
- **FR count**: ~8, **NFR count**: ~5, **MIXED**: ~2
- **Gap analysis**: should find no Security or Compliance gaps (both are covered), but may flag Usability
- **Complexity**: ~45–60 function points, ~6–8 weeks effort
- **Novelty**: Moderately Novel (~60–65) — healthcare domain + FHIR integration + event-driven elements
- **Architecture recommendation**: Event-Driven or Microservices
- **Vague requirements rewritten**: #7, #10, #15

---

## Direct API submission

```powershell
$body = Get-Content docs\sample-project-benchmark.json | ConvertFrom-Json | ConvertTo-Json -Depth 10
Invoke-RestMethod -Uri http://localhost:8000/projects -Method POST -Body $body -ContentType "application/json"
```

The response will contain a `project_id`. Then trigger analysis:

```powershell
$projectId = "<project_id from above>"
$reqs = (Get-Content docs\sample-project-benchmark.json | ConvertFrom-Json).requirements.text
$analyzeBody = @{ project_id = $projectId; requirements = $reqs } | ConvertTo-Json
Invoke-RestMethod -Uri http://localhost:8000/analyze -Method POST -Body $analyzeBody -ContentType "application/json"
```

Then open [http://localhost:3000](http://localhost:3000) and navigate to **Analysis Dashboard**.
