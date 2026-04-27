# Faculty Access Request Screen (US-06)

## Screenshot
![Faculty Access Request Screen](faculty%20access.png)

## Screen Layout: FacultyAccessRequestFragment

### Purpose
Form interface for faculty to sponsor guest entry into the campus checkpoint system.

### Key Components

#### Form Fields
- **Guest Full Name** (text input)
  - Placeholder: "Enter guest name"
  - Required field
  - ID: `input_guest_name`
  
- **Guest ID Number** (text input)
  - Placeholder: "Enter guest ID"
  - Required field
  - ID: `input_guest_id`
  
- **Gate Label** (text input)
  - Placeholder: "e.g., Main Gate, North Entrance"
  - Required field
  - ID: `input_gate_label`

#### Action Button
- **Submit Request** (Material Button)
  - ID: `button_submit_request`
  - Color: Primary accent
  - Action: Creates entry request and displays confirmation

#### Navigation
- Bottom navigation bar (bound to `RoleDestination.DASHBOARD`)

### User Flow
1. Faculty user navigates to Faculty role home
2. Opens "Request Guest Entry" section
3. Fills form with guest information
4. Submits request
5. System validates required fields
6. On success: Snackbar shows "Entry request submitted"
7. On error: Snackbar shows validation error message

### Data Model (EntryRequest)
```
{
  "type": "request",
  "status": "pending",
  "fullName": "Guest Name",
  "guestIdNumber": "ID123456",
  "gateLabel": "Main Gate",
  "hostName": "Faculty Display Name",
  "roleTag": "Guest",
  "iconType": "guest",
  "expiresAt": null,  // v1: omitted
  "requesterUid": "uid_faculty",
  "requesterRole": "faculty",
  "createdAt": "timestamp",
  "updatedAt": "timestamp"
}
```

### Fragment Implementation Details
- **Class**: `FacultyAccessRequestFragment extends Fragment`
- **Layout**: `R.layout.fragment_faculty_access_request`
- **Repository**: `EntryRequestRepository`
- **Auth Required**: Yes - `AuthUiGuard.requireProfile()`

### Known Constraints
- Request scheduling fields omitted in v1 for simpler checkpoint flow
- No request modification after submission (v1)
- Requires valid user profile context

### Error Scenarios
- Empty field validation: "Please fill all required fields"
- No active profile: User is prompted to authenticate
- Server error: "Failed to create entry request"

