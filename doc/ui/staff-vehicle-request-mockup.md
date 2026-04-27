# Staff Vehicle Request Screen (US-08/US-09)

## Screenshot
![Staff Vehicle Request Screen](staff%20vehicle.png)

## Screen Layout: StaffVehicleRequestFragment

### Purpose
Form + list interface for staff to register vehicle information and track vehicle request status history.

### Key Components

#### Vehicle Registration Form
- **Vehicle Make** (text input)
  - Placeholder: "e.g., Toyota, Honda"
  - Optional field
  - ID: `input_vehicle_make`
  
- **Vehicle Model** (text input)
  - Placeholder: "e.g., Civic, CR-V"
  - Required field
  - ID: `input_vehicle_model`
  
- **License Plate Number** (text input)
  - Placeholder: "e.g., ABC-1234"
  - Required field (converted to uppercase on submission)
  - ID: `input_plate_number`

#### Submit Button
- **Submit Vehicle Request** (Material Button)
  - ID: `button_submit_vehicle_request`
  - Color: Primary accent
  - Action: Creates vehicle request record

#### Vehicle Requests History List
- **RecyclerView** (LinearLayoutManager vertical)
  - ID: `recycler_vehicle_requests`
  - Adapter: `VehicleRequestAdapter`
  - Shows request status (pending, approved, etc.)
  - Sorted by creation date (newest first)

#### Empty State
- **Empty Message TextView**
  - ID: `text_vehicle_empty`
  - Text: "No vehicle requests yet"
  - Visible when no requests exist

#### Navigation
- Bottom navigation bar (bound to `RoleDestination.VEHICLES`)

### User Flow
1. Staff opens vehicle request section
2. Fills make, model, and plate number
3. Submits request
4. System validates plate/model non-empty constraint
5. Vehicle request added to history list
6. Requests update in real-time as status changes

### Data Model (VehicleRequestRecord)
```
{
  "requesterUid": "uid_staff",
  "plateNumber": "ABC1234",           // Stored uppercase
  "vehicleModel": "Toyota Civic",
  "status": "pending",                // pending, approved, rejected
  "createdAt": "timestamp",
  "updatedAt": "timestamp"
}
```

### Fragment Implementation Details
- **Class**: `StaffVehicleRequestFragment extends Fragment`
- **Layout**: `R.layout.fragment_staff_vehicle_request`
- **Repository**: `VehicleRequestRepository`
- **Auth Required**: Yes
- **Listener Pattern**: `listenVehicleRequests(uid, listener)`

### Known Constraints
- Editing submitted requests is unsupported (v1)
- Validation limited to non-empty plate/model values
- Plate numbers normalized to uppercase
- Make field combined with model for full vehicle description

### Error Scenarios
- Empty plate or model: "Please fill all required fields"
- Network error: "Failed to submit request" or "Failed to load vehicles"
- No active profile: Prompts authentication
- Server error on submission: Snackbar with error message

### Data Source
- **Firestore Path**: `vehicle_requests`
- **Query**: `where requesterUid = currentUid`
- **Listener Type**: Real-time snapshot listener with client-side sort
- **Sort Default**: Creation date descending (no composite index required)

### UI Patterns
- Form at top, list below
- Material Design input fields with hints
- Real-time list updates without page refresh
- Request status color-coded in history view

