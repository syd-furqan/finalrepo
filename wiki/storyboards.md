# Storyboards — Current Flows and Next Blocks

**Team GLITCH** | CS360 Software Engineering  
**Last Updated:** 2026-05-04

Primary design reference: [GateSync Figma](https://www.figma.com/design/VYLrX3lyUCgolPVw6XCKvd/GateSync?node-id=0-1&p=f)

## Active Role Flows

### 1. Authentication and Role Landing
- User logs in with a valid account profile.
- Supported active roles: `admin`, `guard`, `faculty`, `student`.
- Role landing routes users into role-specific navigation and actions.

### 2. Issuer Flow (Faculty / Student)

#### Step I1 — Start Guest Pass Issuance
- Issuer opens Guest Pass creation.
- Required fields:
- Guest full name
- Guest CNIC (canonical: `xxxxx-xxxxxxx-x`)
- Optional guest vehicle toggle
- If vehicle toggle is true: guest vehicle plate (`AAA-xxx(x)`)

#### Step I2 — Validation and Policy Checks
- CNIC and plate are normalized before persistence.
- Issuance is blocked by daytime policy (08:30–22:30, campus timezone).
- Student-specific restriction: if an active/overdue visit already exists, new issuance is blocked.

#### Step I3 — Coupled Creation
- System creates `Guest Pass + Entry Request` together atomically.
- Pass includes `Pass Code`, QR content, guest metadata, sponsor metadata, `entryRequestId`, and `In-Gate`.

#### Step I4 — Share / Manage
- Issuer can share active pass card (QR + Pass Code).
- Issuer can cancel active pass.
- Issuer can view details for all records.
- Archived records (`expired/cancelled/denied/exited`) remain visible and non-shareable.

### 3. Guard Verification and Admission Flow

#### Step G1 — Verification Input
- Guard opens scan/verification screen.
- Guard verifies using either:
- QR scan
- Pass Code manual input

#### Step G2 — Pending Decision Routing
- If pass is valid and linked, system persists a pending decision context and routes to a mandatory decision fragment.
- The pending decision is restored if guard leaves and returns.

#### Step G3 — Mandatory Decision Fragment
- Guard sees full details:
- guest identity, pass code, entry request link, sponsor details, gate, timestamps
- Required checkpoints:
- guest identity verified
- if vehicle guest: vehicle verified checkbox
- Actions: `Allow` or `Deny` only.

#### Step G4 — Outcomes
- `Allow`:
- entry request becomes active/admitted
- linked guest pass becomes used/admitted with metadata
- route to dashboard
- `Deny`:
- request denied is persisted
- linked guest pass marked denied (archived)
- route to dashboard

### 4. Guard Dashboard and Exit Flow
- Dashboard lists admitted/active entries.
- Entry cards expose `Details`.
- From details, guard logs exit.
- Exit updates linked request/pass lifecycle and audit trail.

### 5. Admin Oversight (Current)
- Admin can access audit, user management, rules, and alerts surfaces.
- Audit foundation and export exist with filter/pagination.
- Security alerts are reported-entry incident queue with intervention actions in progress (ban/fine).
- Shift management and analytics surfaces are in progress.

---

## Next Storyboard Blocks (Planned)

### A. Sponsor Personal Vehicle Registration (Faculty/Student)
- One-time personal vehicle registration request.
- Submission status tracking for requester.

### B. Admin Vehicle Approval Pipeline
- Admin queue for sponsor vehicle requests.
- Approve/deny decision flow with audit events.
- Approved vehicle credentials become gate-verifiable.

### C. Guard Incident Reporting
- Guard reports incident linked to an entry request.
- Minimal capture for fast gate operations.

### D. Admin Alert Intervention
- Admin receives incident/alert queue.
- Intervention actions include fine sponsor / ban guest.
- All interventions are auditable and historically reviewable.

---

## Terminology Lock
- `Pass Code` is the manual fallback identifier.
- `In-Gate` is the only gate label shown in UI (stored as `in-gate`).
- `Guest Pass` issuance is the canonical trigger for `Entry Request` creation.
