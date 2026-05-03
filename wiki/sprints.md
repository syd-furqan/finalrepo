# Sprint / Milestone Ledger (Current)

**Team GLITCH** | CS360 Software Engineering  
**Last Updated:** 2026-05-04

This ledger replaces the old generic sprint narrative with a product-state and next-work view.

## Team Roles
- Syed Furqan Abdullah (26100229) — Scrum Master
- Muhammad Aaffan Khan Niazi (26100015) — Requirement Lead
- Mohammad Ali Wajid (27100125) — UI Designer A
- Abyaan Shivjani (27100110) — UI Designer B
- Afnan Ahmad Baig (27100206) — System Architect

---

## Completed Milestones

### M1 — Core Auth + Role Routing
**Status:** Done  
- Role-authenticated app entry and role-based navigation model established.

### M2 — Coupled Guest Entry Issuance
**Status:** Done  
- Guest Pass and Entry Request are created together atomically.
- Single-gate normalization (`in-gate` / `In-Gate`) is enforced.

### M3 — Guard Verification and Decision Enforcement
**Status:** Done  
- Guard verifies with QR or Pass Code.
- Mandatory unresolved decision flow (`Allow` / `Deny`) persists across return.
- No auto-admission from raw scan event.

### M4 — Guest Lifecycle Completion
**Status:** Done  
- Admit, deny, cancel, expire, overdue, exit lifecycle handling integrated.
- Exit is logged from details and propagates to linked pass state.

### M5 — Pass UX and Share Surface
**Status:** Done  
- Pass details and archive views available.
- Active pass sharing includes pass card and fallback Pass Code.
- Archived passes are non-shareable.

### M6 — Data Quality and Input Standards
**Status:** Done  
- CNIC normalization/validation (`xxxxx-xxxxxxx-x`).
- Guest vehicle plate normalization/validation (`AAA-xxx(x)`).

---

## Current Technical Caveats (Doc-Level)
- Some legacy code/docs still reference old staff-era modules; active product scope is now admin/guard/faculty/student.
- Off-hours policy bypass can be temporarily enabled for local testing; this is a dev/testing switch, not product behavior.
- Vehicle program for sponsor-owned cars is not implemented yet (separate from guest vehicle capture).

---

## Next Milestones (Locked Priority: Vehicle-Program First)

### N1 — Sponsor Vehicle Program
**Priority:** P0  
- Faculty one-time personal vehicle registration.
- Student one-time personal vehicle registration.
- Admin approval/denial flow.
- Approved vehicle credential activation for verification.

### N2 — Guard Incident Reporting
**Priority:** P0  
- Guard can report incident linked to corresponding entry request.
- Minimal structured payload for fast gate operation.

### N3 — Admin Intervention Workflow
**Priority:** P0  
- Admin incident/alert triage queue.
- Intervention actions: fine sponsor, ban guest.
- Full audit correlation for interventions.

### N4 — Traffic Monitoring and History
**Priority:** P1  
- Admin traffic statistics dashboards and historical trend slices.

### N5 — Guard Shift Management
**Priority:** P1  
- Admin shift assignment/rotation model.
- Guard-facing shift visibility.

---

## Maintenance Rules
- Update this ledger whenever a milestone changes state.
- Keep milestone IDs stable and map them to backlog IDs.
- Keep active role scope aligned with `wiki/backlog.md`.
