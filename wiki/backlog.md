# Campus Gate Access System — Product Backlog (Current)

**Team GLITCH** | CS360 Software Engineering  
**Last Updated:** 2026-05-04

## Documentation Contract
- **Canonical backlog IDs:** IDs in this file are the source of truth for planning and sprint mapping.
- **Status vocabulary:** `Done`, `In Progress`, `Planned` only.
- **Active roles only:** `admin`, `guard`, `faculty`, `student`.
- **Canonical terms:** `Pass Code`, `In-Gate`, `Guest Pass`, `Entry Request`, `Archived`.

## Current Product Behavior (As Built)
- Guest entry is created by **coupled issuance**: `Guest Pass + Entry Request` in one atomic workflow.
- Guest Pass supports two verification inputs at gate: **QR scan** or **Pass Code**.
- Guard scan/pass verification routes to a **mandatory pending decision fragment** with `Allow` or `Deny` (no dismiss path).
- Successful `Allow` updates linked request/pass state; `Deny` persists denial and archives pass state.
- Guard dashboard focuses on admitted/active entries and logs exit from details.
- Single gate policy is canonical: **In-Gate** (`in-gate` stored value).
- Guest Pass status lifecycle in active use: `active`, `used`, `overdue`, `denied`, `cancelled`, `expired`, `exited`.
- Archived passes are non-shareable and remain visible in archive/details.
- Issuer input standards are enforced:
- CNIC format: `xxxxx-xxxxxxx-x`
- Guest vehicle plate format: `AAA-xxx(x)`

---

## Backlog by Epic

### Epic A — Guest Entry Lifecycle

| ID | Story | Primary Role | Status | Priority | Notes |
|---|---|---|---|---|---|
| GEL-01 | Issue Guest Pass and Entry Request atomically in one operation. | faculty/student | Done | P0 | Prevents orphan pass/request records. |
| GEL-02 | Admit guest via guard decision flow after QR/Pass Code verification. | guard | Done | P0 | Admission is explicit; not auto-approved on scan. |
| GEL-03 | Persist deny outcomes for request and linked pass. | guard | Done | P0 | Denied pass is archived. |
| GEL-04 | Log exit from request details and update linked pass status. | guard | Done | P0 | Exit closes visit lifecycle. |
| GEL-05 | Enforce CNIC + optional guest-vehicle capture in issuer forms. | faculty/student | Done | P0 | CNIC/plate normalization enforced in UI + repository. |
| GEL-06 | Support archived pass viewing with details and non-shareable enforcement. | faculty/student | Done | P1 | Archived = cancelled/expired/denied/exited. |
| GEL-07 | Maintain sponsor-scoped local notifications for guest pass lifecycle events. | faculty/student | Done | P1 | Local notification-center alerts. |
| GEL-08 | Finalize parity checks for faculty no-overdue policy vs student overdue policy. | admin/guard | In Progress | P1 | Student overdue remains enforced; faculty behavior must stay no-overdue after admission. |

### Epic B — Guard Operations

| ID | Story | Primary Role | Status | Priority | Notes |
|---|---|---|---|---|---|
| GOP-01 | Submit incident report for a corresponding entry without full case handling in guard UI. | guard | Planned | P0 | Fast reporting handoff to admin. |
| GOP-02 | Add guard incident list/status visibility for submitted reports. | guard | Planned | P1 | Read-only tracking is sufficient for v1. |
| GOP-03 | Add guard shift view (today/next shift) in operational screens. | guard | Planned | P2 | Depends on admin shift assignment model. |

### Epic C — Admin Governance and Oversight

| ID | Story | Primary Role | Status | Priority | Notes |
|---|---|---|---|---|---|
| AGO-01 | Complete advanced audit workflows (structured event readability, strong filter/export ergonomics). | admin | In Progress | P1 | Existing audit foundation is present; UX/report depth expansion pending. |
| AGO-02 | Receive and triage guard-submitted incidents from a dedicated admin queue. | admin | Planned | P0 | Linked to guard incident reporting. |
| AGO-03 | Record intervention actions (fine sponsor, ban guest) with audit trail. | admin | Planned | P0 | Must retain decision provenance. |
| AGO-04 | Traffic monitoring dashboard with statistics and historical trend views. | admin | Planned | P1 | Includes admitted/denied/exited trend slices. |
| AGO-05 | Guard management for day/night shift assignment and rotation. | admin | Planned | P1 | Shift model and assignment UX required. |

### Epic D — Sponsor Vehicle Program (**Vehicle-Program First Priority**)

| ID | Story | Primary Role | Status | Priority | Notes |
|---|---|---|---|---|---|
| SVP-01 | Faculty one-time personal vehicle registration request. | faculty | Planned | P0 | Separate from guest-vehicle info capture. |
| SVP-02 | Student one-time personal vehicle registration request. | student | Planned | P0 | Separate from guest-vehicle info capture. |
| SVP-03 | Admin approval/denial pipeline for personal vehicle registration. | admin | Planned | P0 | Required to activate vehicle credentials. |
| SVP-04 | Persist approved personal vehicle credential for gate verification. | admin/guard | Planned | P0 | Must integrate with verification subsystem. |
| SVP-05 | Sponsor visibility into personal vehicle registration status/history. | faculty/student | Planned | P1 | Submission + decision transparency. |

---

## Locked Next-Phase Priority Order
1. Sponsor Vehicle Program (`SVP-01` to `SVP-05`)
2. Guard incident reporting (`GOP-01`, `GOP-02`)
3. Admin intervention workflow (`AGO-02`, `AGO-03`)
4. Traffic monitoring statistics/history (`AGO-04`)
5. Guard shift management (`GOP-03`, `AGO-05`)

## Out of Active Scope
- Legacy Staff role workflows are not part of active product scope.
- Temporary debug/test toggles (for off-hours testing) are not product requirements.
