# Admin Verification Rules Screen (US-16)

## Screenshot
![Admin Verification Rules Screen](admin%20verification%20rule.png)

## Screen Layout: AdminVerificationRulesFragment

### Purpose
Allow admins to configure credential verification rules such as ID expiry enforcement and banned identifier lists to maintain security policies.

### Key Components

- **Rule Settings Form**
  - Toggle: "Enforce ID Expiry"
    - When enabled, credentials with past expiry dates are rejected
    - ID: `toggle_enforce_expiry`
  - Threshold: "Alert After N Failed Attempts"
    - Controls when security alerts are triggered
    - ID: `input_alert_threshold`

- **Banned Identifiers Section**
  - List of blocked identifiers (ID numbers, etc.)
  - Add button to enter new banned identifier
  - Remove button for each banned entry
  - ID: `list_banned_identifiers`, `input_banned_id`

- **Save Button**
  - Persists all rule changes to Firestore
  - ID: `button_save_rules`

- **Confirmation Messages**
  - Snackbar shows success or error

### User Flow
1. Admin opens verification rules screen.
2. Admin enables/disables expiry enforcement.
3. Admin adjusts the alert threshold.
4. Admin adds identifiers to the banned list (comma-separated or one-by-one).
5. Admin saves the rules.
6. Rules take effect immediately for credential verification.

### Data Model
- VerificationRules object with:
  - `enforceIdExpiry` (boolean)
  - `alertThreshold` (integer)
  - `bannedIdentifiersCsv` (CSV string of banned items)
  - Stored in Firestore collection `verification_rules/current`

### Fragment Implementation
- **Class**: `AdminVerificationRulesFragment extends Fragment`
- **Layout**: `R.layout.fragment_admin_verification_rules`
- **Repository**: `FirestoreVerificationRulesRepository` or similar
- **Save Method**: Persists rules object to Firestore

### Known Constraints
- Rules changes apply to all subsequent verifications immediately
- No audit trail per change (entire rules doc is versioned)
