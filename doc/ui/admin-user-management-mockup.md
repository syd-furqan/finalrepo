# Admin User Management Screen (US-14)

## Screenshot
![Admin User Management Screen](admin%20user%20management.png)

## Screen Layout: AdminUserManagementFragment

### Purpose
Enable admins to add, edit, and deactivate user accounts across all roles (guards, faculty, staff, students) to keep access permissions current.

### Key Components

- **User List RecyclerView**
  - Shows all users with name, email, role, status
  - Adapter: UserManagementAdapter
  - Search/filter by name or role
  - ID: `recycler_users`

- **User Item Actions**
  - Edit button → opens form to modify user details
  - Deactivate button → disables account
  - Reset Password → sends reset link
  - ID: `user_action_menu`

- **Create New User Button**
  - Opens form for new user registration
  - ID: `button_create_user`

- **User Form**
  - Name, email, role (guard/faculty/staff/student)
  - Status (active/inactive)
  - ID: `form_user_details`

- **Confirmation Dialogs**
  - Deactivation requires confirmation
  - Success/error messages shown as snackbars

### User Flow
1. Admin views user list.
2. Admin searches for a user or filters by role.
3. Admin selects a user to edit or deactivate.
4. Changes are saved to Firestore.
5. Status updates in the list.

### Data Model
- UserProfile object with:
  - `uid`, `email`, `displayName`
  - `role` (admin, guard, faculty, staff, student)
  - `status` (active, deactivated)
  - Timestamps: `createdAt`, `updatedAt`

### Fragment Implementation
- **Class**: `AdminUserManagementFragment extends Fragment`
- **Layout**: `R.layout.fragment_admin_user_management`
- **Repository**: `FirestoreUserManagementRepository` or similar
- **Adapter**: `UserManagementAdapter`

### Known Constraints
- User deletion is not supported; deactivation is permanent
- Role changes require admin authentication
