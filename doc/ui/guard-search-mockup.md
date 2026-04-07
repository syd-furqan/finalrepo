# Guard Visitor Search Screen (US-02)

## Screenshot
![Guard Search Screen](guard%20search.png)

## Screen Layout: GuardSearchFragment

### Purpose
Enable guards to manually search and look up visitor information by name, ID, or vehicle number when QR scanning is unavailable.

### Key Components

- **Search Input**
  - Text field with placeholder "Search by name, ID, or vehicle"
  - ID: `search_query`
  - Real-time filtering as user types

- **Results RecyclerView**
  - Shows matching entry requests
  - Adapter displays guest name, host, status, gate
  - Clickable items for detailed review

- **Empty State**
  - Message when no results found

- **Bottom Navigation**
  - Routes back to dashboard

### User Flow
1. Guard opens search screen.
2. Guard enters search term (name, ID, vehicle number).
3. System filters requests in real-time.
4. Guard selects a result to proceed with verification.

### Data Model
- Query filtered EntryRequest objects
- Searches across: `fullName`, `guestIdNumber`, vehicle info fields
- Returns matching records from Firestore

### Fragment Implementation
- **Class**: `GuardSearchFragment extends Fragment`
- **Layout**: `R.layout.fragment_guard_search`
- **Repository**: `EntryRequestRepository` with search method
- **Search Pattern**: Case-insensitive partial match

### Known Constraints
- Search is client-side filtered after fetching from Firestore
- Limited to 50 results for performance
