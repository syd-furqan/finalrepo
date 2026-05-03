# Object-Oriented Analysis (Updated CRC View)

**Campus Gate Access System**  
**Team GLITCH** | CS360 Software Engineering  
**Last Updated:** 2026-05-04

This CRC-oriented analysis reflects the current product model and near-term roadmap.
Active roles in scope: `admin`, `guard`, `faculty`, `student`.

---

## 1. Domain Core Classes

### Class: `GuestPass`
**Responsibilities**
- Represent sponsor-issued pass data (guest identity, optional guest vehicle, Pass Code, QR payload linkage).
- Maintain lifecycle state (`active`, `used`, `overdue`, `denied`, `cancelled`, `expired`, `exited`).
- Link one-to-one with `EntryRequest` via `entryRequestId`.
- Expose share/archive behavior boundaries.

**Collaborators**
- `EntryRequest`
- `GuestPassRepository`
- `GuestPassStatusRules`
- `GuestPassTimePolicy`

### Class: `EntryRequest`
**Responsibilities**
- Represent gate admission intent and visit state.
- Store admitted/denied/exited progression and linkage to issuer metadata.
- Carry operational metadata used by guard dashboard and audit.

**Collaborators**
- `GuestPass`
- `EntryRequestRepository`
- `AuditEvent`

### Class: `GuardPendingDecision`
**Responsibilities**
- Persist unresolved guard decision context after scan/pass verification.
- Restore mandatory unresolved decision across navigation/app return.
- Provide details needed for final allow/deny resolution.

**Collaborators**
- `GuardPendingDecisionStore`
- `GuestPass`
- `EntryRequestRepository`

### Class: `AuditEvent`
**Responsibilities**
- Model immutable access/security activity records.
- Capture actor role/uid, entity correlation, source, outcome, and metadata.
- Support list/filter/export/reporting workflows.

**Collaborators**
- `AuditLogRepository`
- `EntryRequestRepository`
- `GuestPassRepository`

### Class: `VerificationRules`
**Responsibilities**
- Represent configurable credential verification policy inputs.
- Drive verification and alert-trigger behavior.

**Collaborators**
- `EntryRequestRepository`
- Guard verification flows

---

## 2. Repository and Policy Classes

### Class: `GuestPassRepository` (role-aware data service)
**Responsibilities**
- Atomically issue `Guest Pass + Entry Request`.
- Enforce lifecycle transitions (cancel/admit/deny/exit linkage).
- Lookup pass by Pass Code and normalize stale states.

**Collaborators**
- `GuestPass`
- `EntryRequestRepository`
- `GuestIdentityPolicy`
- `GuestPassTimePolicy`

### Class: `EntryRequestRepository`
**Responsibilities**
- Serve active dashboard request stream.
- Persist allow/deny/exit state changes.
- Coordinate credential verification and alert generation.

**Collaborators**
- `EntryRequest`
- `AuditEvent`
- `VerificationRules`

### Class: `GuestIdentityPolicy`
**Responsibilities**
- Normalize and validate guest CNIC and vehicle plate formats.
- Provide canonical representation for persistence and comparisons.

**Collaborators**
- Issuer UI fragments
- `GuestPassRepository`
- Guard decision surface

### Class: `GatePolicy`
**Responsibilities**
- Enforce single-gate canonical value (`in-gate`) and display label (`In-Gate`).

**Collaborators**
- `GuestPass`
- `EntryRequest`
- repositories/UI formatters

### Class: `GuestPassTimePolicy`
**Responsibilities**
- Enforce daytime issuance/entry window and expiration semantics.
- Detect out-of-policy active records for normalization.

**Collaborators**
- `GuestPassRepository`
- Guard verification flows

### Class: `GuestPassStatusRules`
**Responsibilities**
- Define status interpretation for archive/in-progress/share/blocking logic.

**Collaborators**
- Issuer UI adapters/fragments
- `GuestPassRepository`

---

## 3. Responsibility Mapping
- **Faculty/Student** initiate guest entry by issuing a `GuestPass`; this atomically creates `EntryRequest`.
- **Guard** verifies pass (QR or Pass Code), resolves pending decision (`Allow`/`Deny`), and logs exit from details.
- **Admin** governs users/rules/audit/alerts and will own intervention workflows (fine/ban) and vehicle approvals in next phase.
- **Policies** (`GatePolicy`, `GuestIdentityPolicy`, `GuestPassTimePolicy`, `GuestPassStatusRules`) constrain data consistency and behavior.

---

## 4. Current Invariants and Constraints
- `GuestPass` must not exist without linked `EntryRequest` at issuance time.
- Single-gate domain: all records normalize to `in-gate`.
- Pass identification fallback is always `Pass Code`.
- Student issuance is constrained by active/overdue lifecycle restrictions.
- Guard unresolved decision is persisted and must be resolved explicitly.

---

## 5. Near-Term Extension Points
- Sponsor personal vehicle registration entities/workflows (faculty/student).
- Admin vehicle approval model and credential activation.
- Guard incident-report entity linked to entry request.
- Admin intervention action entity (fine/ban) with audit correlations.
