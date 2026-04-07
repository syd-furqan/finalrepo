# Credential Verification Screen (US-03)

## Screen Layout: VerificationResultFragment

### Purpose
Display real-time credential verification results so guards can quickly approve or deny entry requests based on database validation.

### Key Components

- **Verification Result Card**
  - Status badge (Valid / Invalid)
  - Color coded: green for pass, red for fail
  - ID: `verification_status`

- **Credential Details**
  - Identifier (ID number, vehicle plate)
  - Holder name
  - Expiry date (if applicable)
  - ID: `credential_info`

- **Verification Details**
  - Rule enforcement indicators
  - Failure reason (if applicable)
  - Banned list status
  - Expiry check result

- **Action Buttons**
  - "Grant Entry" → logs entry and navigates to dashboard
  - "Deny Request" → navigates to denial form
  - "Request More Info" → optional escalation

- **Entry Details Sheet**
  - Bottom sheet dialog for detailed inspection

### User Flow
1. Guard scans or selects a visitor.
2. System verifies credential against rules and database.
3. Result displays: credential valid/invalid.
4. Guard taps "Grant Entry" or proceeds to denial.

### Data Model
- CredentialVerificationResult object with:
  - `isValid` (boolean)
  - `identifier`, `holderName`
  - `message` (success or failure reason)

### Fragment Implementation
- **Class**: `VerificationResultFragment extends Fragment`
- **Layout**: `R.layout.fragment_verification_result`
- **Repository**: `EntryRequestRepository.verifyCredential()`
- **Rule Evaluation**: Checks expiry, banned lists, etc.

### Known Constraints
- Verification depends on university database availability
- Rules are fetched at runtime from Firestore
