# Admin Audit Log Screen (US-13)

## Screenshot
![Admin Audit Log Screen](admin%20logs.png)

## Screen Layout: AdminAuditLogFragment

### Purpose
Display a full security audit history of all gate activities for incident investigation and compliance verification.

### Key Components

- **Audit Log RecyclerView**
  - Table-like list of events
  - Columns: timestamp, event type, actor, request ID, description
  - Adapter: SecurityAuditLogAdapter
  - Sorted by most recent first
  - ID: `recycler_audit_log`

- **Filter Options**
  - Date range picker
  - Event type filter (REQUEST_CREATED, ENTRY, EXIT, DENY, etc.)
  - Actor role filter (guard, faculty, admin)
  - ID: `filter_panel`

- **Empty State**
  - "No audit entries found" when no results

- **Export Button**
  - Exports filtered log to CSV/PDF
  - ID: `button_export_log`

### User Flow
1. Admin opens audit log.
2. Logs are fetched from Firestore's access_events collection.
3. Admin applies filters by date range or event type.
4. Admin selects entries and exports as report.

### Data Model
- AccessEvent object with fields:
  - `eventType` (REQUEST_CREATED, ENTRY, EXIT, DENY, etc.)
  - `requestId`, `actorUid`, `actorRole`
  - `description`, `createdAt` (server timestamp)

### Fragment Implementation
- **Class**: `AdminAuditLogFragment extends Fragment`
- **Layout**: `R.layout.fragment_admin_audit_log`
- **Repository**: `EntryRequestRepository.listenAccessEvents()` or similar
- **Adapter**: `SecurityAuditLogAdapter` or similar

### Known Constraints
- Audit log is append-only and immutable
- Performance may be impacted by large log sizes (pagination recommended)
