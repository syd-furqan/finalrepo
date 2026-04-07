# Deny Entry Request Screen (US-05)

## Screen Layout: GuardDenyFragment

### Purpose
Allow guards to deny an entry request and document the denial reason for security audit records.

### Key Components

- **Request Summary**
  - Guest name, host information, gate label
  - Request ID and timestamp
  - ID: `request_summary`

- **Denial Reason Input**
  - Text field for explanation
  - Placeholder: "Enter denial reason"
  - ID: `input_denial_reason`
  - Required field

- **Confirm Deny Button**
  - Submits denial and updates request status
  - ID: `button_confirm_deny`

- **Cancel Button**
  - Returns to dashboard without action

### User Flow
1. Guard selects a request to deny.
2. Guard enters the denial reason.
3. Guard confirms the denial.
4. System updates request status to "denied" and logs the event.
5. Notification is sent to the requester (if applicable).

### Data Model
- DenyRequest call includes:
  - `requestId`
  - `reason` (denial explanation)
  - Firestore updates: `status` = "denied", `denialReason`, `updatedAt`

### Fragment Implementation
- **Class**: `GuardDenyFragment extends Fragment`
- **Layout**: `R.layout.fragment_guard_deny`
- **Repository**: `EntryRequestRepository.denyRequest(requestId, reason, callback)`
- **Callback**: Snackbar confirmation or error message

### Known Constraints
- Denial reason is mandatory for audit trail
- Once denied, request cannot be reopened (design decision)
