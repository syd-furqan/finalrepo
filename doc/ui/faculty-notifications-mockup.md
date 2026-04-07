# Faculty Notifications Screen (US-07)

## Screenshot
![Faculty Notifications Screen](faculty%20notif.png)

## Screen Layout: FacultyNotificationsFragment

### Purpose
Inbox notification feed for faculty users to receive approval/denial status of submitted entry requests and other system notifications.

### Key Components

#### Notifications List
- **RecyclerView** (LinearLayoutManager vertical)
  - ID: `recycler_notifications`
  - Adapter: `NotificationAdapter`
  - Real-time updates from Firestore

#### Empty State
- **Empty Message TextView**
  - ID: `text_notifications_empty`
  - Text: "No notifications yet"
  - Visible when no notifications exist

#### Notification Item Layout
Each item displays:
- **Title** (notification title)
- **Message** (notification body text)
- **Timestamp** (createdAt field)
- **Type Badge** (approval/denial/system)
- **Associated Request ID** (optional link to request details)

#### Navigation
- Bottom navigation bar (bound to `RoleDestination.PASSES`)

### User Flow
1. Faculty opens notifications screen
2. System loads notifications from `notifications/{uid}/items` collection
3. Items appear in reverse chronological order (newest first)
4. Tap on notification for additional details (future feature)
5. Notifications auto-update via real-time listener

### Data Model (NotificationItem)
```
{
  "title": "Request approved",
  "message": "Your guest request has been approved at Main Gate.",
  "type": "approval",
  "requestId": "req_123456",
  "createdAt": "timestamp"
}
```

Typical Notification Types:
- **approval**: Request was accepted at checkpoint
- **denial**: Request was denied with reason
- **system**: General system announcements

### Fragment Implementation Details
- **Class**: `FacultyNotificationsFragment extends Fragment`
- **Layout**: `R.layout.fragment_faculty_notifications`
- **Repository**: `NotificationRepository`
- **Auth Required**: Yes
- **Listener Pattern**: Real-time `listenNotifications(uid, listener)`

### Known Constraints
- Notification read state acknowledgement deferred to future iteration
- Ordering assumes `createdAt` exists on all notification documents
- Notifications only show for authenticated faculty users

### Error Scenarios
- Network error loading notifications: "Failed to load notifications"
- Invalid user profile: Empty state displayed
- Firestore collection unavailable: Shows error snackbar with retry option

### Data Source
- **Firestore Path**: `notifications/{userId}/items`
- **Query**: `orderBy('createdAt', descending)`
- **Listener Type**: Real-time snapshot listener

