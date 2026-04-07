# Student Guest Pass Screen (US-10/US-11/US-12)

## Screen Layout: StudentGuestPassFragment

### Purpose
Interface for students to generate temporary guest passes, set expiry duration, view pass history, and cancel active passes.

### Key Components

#### Guest Pass Form
- **Guest Full Name** (text input)
  - Placeholder: "Enter guest name"
  - Required field
  - ID: `input_pass_guest_name`
  
- **Guest ID/Email** (text input)
  - Placeholder: "Enter guest ID or email"
  - Required field
  - ID: `input_pass_guest_id`
  
- **Expiry Duration in Hours** (text input)
  - Placeholder: "e.g., 24"
  - Required field (numeric)
  - ID: `input_pass_expiry_hours`
  - Constraint: Minimum 1 hour

#### Create Pass Button
- **Create Guest Pass** (Material Button)
  - ID: `button_create_pass`
  - Color: Primary accent
  - Action: Generates pass with unique code

#### Guest Passes List
- **RecyclerView** (LinearLayoutManager vertical)
  - ID: `recycler_guest_passes`
  - Adapter: `GuestPassAdapter`
  - Shows active and cancelled passes
  - Each item includes:
    - Guest name and ID
    - Pass code (8-character alphanumeric)
    - Status (active/cancelled)
    - Expiry timestamp
    - Cancel button (only for active passes)

#### Empty State
- **Empty Message TextView**
  - ID: `text_guest_pass_empty`
  - Text: "No guest passes created yet"
  - Visible when no passes exist

#### Navigation
- Bottom navigation bar (bound to `RoleDestination.PASSES`)

### User Flow - Create Pass (US-10)
1. Student fills guest name, ID/email, and expiry hours
2. Clicks "Create Guest Pass"
3. System generates unique 8-char pass code
4. Pass stored in Firestore with expiry timestamp
5. Confirmation snackbar: "Guest pass created"
6. New pass appears in list with code visible

### User Flow - Cancel Pass (US-12)
1. Student views active pass in list
2. Clicks "Cancel" button on pass
3. System updates pass status to `cancelled`
4. Pass becomes unusable
5. Confirmation snackbar: "Guest pass cancelled"

### Data Model (GuestPass)
```
{
  "id": "doc_id_auto",
  "sponsorUid": "uid_student",
  "sponsorRole": "student",
  "sponsorName": "Student Display Name",
  "sponsorEmail": "student@university.edu",
  "guestName": "John Visitor",
  "guestIdNumber": "VIS-001",
  "passCode": "ABC12XYZ",              // 8-char generated code
  "status": "active",                  // active or cancelled
  "expiresAt": "timestamp",            // currentTime + expiryHours
  "createdAt": "timestamp",
  "updatedAt": "timestamp"
}
```

### Fragment Implementation Details
- **Class**: `StudentGuestPassFragment extends Fragment implements GuestPassAdapter.GuestPassActionListener`
- **Layout**: `R.layout.fragment_student_guest_pass`
- **Repository**: `GuestPassRepository`
- **Auth Required**: Yes
- **Listener Pattern**: `listenGuestPasses(uid, listener)` and `cancelGuestPass(passId, callback)`

### Known Constraints
- Expiry unit limited to whole hours in v1
- Minimum expiry: 1 hour (auto-adjusted if user enters 0 or negative)
- Expiry enforcement checked client-side during QR scan (v1)
- Pass code lookup available via `findPassByCode()` for scanning

### Validation Rules
- All fields required for pass creation
- Expiry hours must be valid positive integer
- Empty field submission: "Please fill all required fields"
- Invalid number entry: "Invalid expiry hours format"

### Error Scenarios
- Network error: "Failed to create guest pass" or "Failed to load passes"
- Invalid profile: No passes shown, empty state
- Server error: Snackbar with error message and reason
- Cancel operation failed: "Failed to cancel guest pass"

### Data Source
- **Firestore Path**: `guest_passes`
- **Queries**: 
  - `where sponsorUid = currentUid` (for list view)
  - `where passCode = scannedCode` (for QR lookup)
- **Listener Type**: Real-time snapshot listener with client-side sort
- **Sort Default**: Creation date descending

### Pass Code Format
- **Length**: 8 characters
- **Characters**: Alphanumeric uppercase
- **Generation**: UUID.randomUUID().substring(0, 8).toUpperCase()
- **Used For**: QR code scanning and manual entry

### UI Patterns
- Form at top with Material Design inputs
- List below showing pass history
- Status badge distinguishes active vs cancelled
- Cancel action accessible directly from list item
- Real-time updates without manual refresh

