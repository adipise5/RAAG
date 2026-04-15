# RAAG System Design Update: DFD + RTM

This document updates the project design artifacts with a strict Data Flow Diagram structure and a simple Requirements Traceability Matrix (RTM).

## 1) Data Flow Diagram (DFD) Update

### Level 0 DFD (Context Diagram)

At Level 0, the whole RAAG platform is represented as **one single process**.
No internal subprocesses and no data stores are shown.

**Single process:**
- `P0: RAAG Requirement Analysis System`

**External entities:**
- `E1: Project User` (student/analyst/team member)
- `E2: Admin`
- `E3: External Services` (LLM provider, export/render backends, notification providers)

**Input data flows to system:**
- `F1: Project details + requirements` (from E1)
- `F2: Admin configuration/policies` (from E2)
- `F3: External responses/data` (from E3)

**Output data flows from system:**
- `F4: Analysis results + recommendations + reports` (to E1)
- `F5: Audit/operations status + control feedback` (to E2)
- `F6: Requests for inference/export/integration` (to E3)

#### Level 0 pseudo-diagram (text)

```text
E1: Project User  ---- F1 ---->  [ P0: RAAG Requirement Analysis System ] ---- F4 ----> E1: Project User
E2: Admin         ---- F2 ---->  [ P0: RAAG Requirement Analysis System ] ---- F5 ----> E2: Admin
E3: External Svcs ---- F3 ---->  [ P0: RAAG Requirement Analysis System ] ---- F6 ----> E3: External Svcs
```

---

### Level 1 DFD (Decomposition of P0)

At Level 1, process `P0` is decomposed into major subprocesses.
This level includes interactions with data stores and shows internal data movement.

**Subprocesses:**
- `P1: Capture Project Input`
- `P2: Classify & Score Requirements`
- `P3: Generate Architecture & DFD Suggestions`
- `P4: Build Dashboard/Chat Responses`
- `P5: Export Report`
- `P6: Record Audit Trail`

**Data stores:**
- `D1: Project Repository`
- `D2: Analysis Repository`
- `D3: Audit Log Store`

**Main flows (consistent with Level 0):**
- E1 -> P1: project metadata + requirements
- P1 -> D1: saved project
- P1 -> P2: normalized requirement set
- P2 <-> E3: inference request/response
- P2 -> D2: classification + quality scores
- P2 -> P3: classified requirements + constraints
- P3 <-> E3: architecture generation request/response
- P3 -> D2: architecture recommendation artifacts
- P4 <- D1/D2: project + analysis retrieval
- P4 -> E1: dashboard views/chat answers
- P5 <- D1/D2: exportable dataset
- P5 <-> E3: rendering/export helpers
- P5 -> E1: downloadable report
- P1/P2/P3/P4/P5 -> P6: audit events
- P6 -> D3: immutable audit records
- E2 <-> P4/P6: admin monitoring and control

#### Level 1 pseudo-diagram (text)

```text
E1 --> P1 --> D1
P1 --> P2 <--> E3
P2 --> D2
P2 --> P3 <--> E3
P3 --> D2
D1 --> P4 <-- D2
P4 --> E1
D1 --> P5 <-- D2
P5 <--> E3
P5 --> E1
P1,P2,P3,P4,P5 --> P6 --> D3
E2 <--> P4
E2 <--> P6
```

**Consistency check with Level 0:**
- All Level 1 external exchanges map back to Level 0 entities/flows (User, Admin, External Services).
- Level 0 remains abstract; Level 1 introduces internal subprocesses and stores.

---

## 2) Requirements Traceability Matrix (RTM)

The RTM below is intentionally shown in **matrix format (not a prose table)**.

```text
        C1   C2   C3   C4   C5   C6
R1      X    X
R2           X    X
R3                X    X
R4                     X    X
R5                          X    X
R6      X                   X         X
```

### Legend

**Requirements**
- `R1`: System accepts project details and requirements from users.
- `R2`: System classifies and quality-scores requirements.
- `R3`: System generates architecture recommendation and DFD guidance.
- `R4`: System provides dashboard/chat-based analysis access.
- `R5`: System exports analysis reports.
- `R6`: System logs all key actions for audit/compliance.

**Components / Modules**
- `C1`: API Gateway (`services/api-gateway`)
- `C2`: Ingestion + LLM Analysis (`services/ingestion-service`, `services/llm-service`)
- `C3`: Quality + Architecture Services (`services/quality-service`, `services/architecture-service`)
- `C4`: Frontend Dashboard & Chat UI (`frontend/src`, `services/chatbot-service`)
- `C5`: Export Service (`services/export-service`)
- `C6`: Audit Service (`services/audit-service`)

---

## 3) Frontend Simplification Scope

The UI simplification is implemented in frontend code with these principles:
- Basic palette (white/gray/soft blue)
- Arial/sans-serif typography
- Traditional page structure (header, nav, forms, tables, footer)
- No gradients, glass effects, or non-essential animation
- Reduced visual noise to improve readability
