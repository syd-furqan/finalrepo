# Guard Dashboard Screen (US-01)

## Screen Layout: DashboardFragment

### Purpose
Real-time dashboard for guards to view active entry requests and manage gate traffic efficiently.

### Key Components

- **Active Requests RecyclerView**
  - List of pending/active entry requests
  - Shows: guest name, host, gate label, request status
  - Adapter: EntryRequestAdapter
  - Sorted by most recent

- **Summary Tiles**
  - Active visitors count
  - Pending requests count
  - Dashboard metrics from real-time Firestore feed

- **Quick Action Buttons**
  - "Scan QR Code" button
  - Navigation to search/lookup
  - Links to verify and approve workflow

- **Bottom Navigation**
  - Routes to different guard views and admin/audit sections

### User Flow
1. Guard opens the dashboard.
2. System loads active requests via real-time listener.
3. Guard selects a request to review.
4. Guard proceeds to verify credentials or deny request.

### Data Model
- EntryRequest objects with fields:
  - `fullName`, `hostName`, `gateLabel`
  - `status` (pending, active, denied, exited)
  - `createdAt`, `enteredAt`, `exitedAt`
  - `requesterUid`, `requesterRole`

### Fragment Implementation
- **Class**: `DashboardFragment extends Fragment`
- **Layout**: `R.layout.fragment_dashboard`
- **Repository**: `EntryRequestRepository` with real-time listener
- **Adapter**: `EntryRequestAdapter`

### Known Constraints
- Assumes active listeners auto-update on request status changes
- Real-time feed depends on Firestore connectivity
