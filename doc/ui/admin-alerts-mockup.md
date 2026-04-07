# Admin Alerts Screen (US-18)

## Screen Layout: AdminAlertsFragment

### Purpose
Display automated security alerts for repeated failed credential checks to help admins respond quickly to potential intrusion attempts.

### Key Components

- **Alert Feed RecyclerView**
  - Displays security alert entries
  - Shows: identifier, timestamp, failure count, severity badge, message
  - Adapter: SecurityAlertAdapter
  - Sorted by most recent first
  - ID: `recycler_alerts`

- **Alert Item**
  - Severity Badge (HIGH / MEDIUM / LOW)
    - Color coded: red (HIGH), orange (MEDIUM), yellow (LOW)
    - Based on fail count vs alert threshold
  - Identifier info (ID number, etc.)
  - Last failed timestamp
  - Failure count
  - Acknowledgement button
  - ID: `alert_item`, `button_acknowledge`

- **Empty State**
  - "No active alerts" when none exist

- **Filter/Sort Options** (optional)
  - Filter by severity
  - Most recent first (default)

### User Flow
1. Admin opens alerts screen.
2. Real-time feed loads failed verification alerts from Firestore.
3. Admin reviews high-severity alerts.
4. Admin acknowledges alert or investigates further.
5. Alert marked as reviewed (or can be deleted).

### Data Model
- SecurityAlert object with:
  - `identifier` (credential ID or similar)
  - `failCount` (number of failed attempts)
  - `severity` (HIGH / MEDIUM / LOW based on threshold)
  - `message` (description of failure reason)
  - `lastFailedAt` (server timestamp)
  - `createdAt` (first alert creation)

### Fragment Implementation
- **Class**: `AdminAlertsFragment extends Fragment`
- **Layout**: `R.layout.fragment_admin_alerts`
- **Repository**: `FirestoreAlertRepository` or similar
- **Real-time Listener**: Listens to alerts collection and updates feed
- **Adapter**: `SecurityAlertAdapter`

### Known Constraints
- Alerts auto-expire after a configurable time period
- Severity is calculated on alert creation based on current rules
