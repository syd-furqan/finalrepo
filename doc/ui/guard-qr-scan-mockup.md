# QR Code Scan Screen (US-17)

## Screen Layout: GuardQrScanFragment

### Purpose
Enable guards to scan QR-code-based guest passes for fast and error-free visitor verification.

### Key Components

- **Camera Preview**
  - Full-screen camera viewfinder
  - Centered scanning overlay/frame
  - ID: `camera_preview`

- **Instruction Text**
  - "Point camera at QR code"
  - Simple guidance text
  - ID: `scan_instruction`

- **Scan Result Display**
  - Shows pass code or verification result
  - Success/failure status badge
  - ID: `scan_result`

- **Manual Entry Option**
  - Text field for fallback pass code entry
  - ID: `input_manual_pass_code`
  - Hidden by default, revealed on error

- **Action Buttons**
  - "Verify Pass" → validates pass code
  - "Proceed" → on successful scan

### User Flow
1. Guard opens QR scanner.
2. Guard points camera at guest pass QR code.
3. System scans and decodes the pass code.
4. System looks up the pass and verifies it.
5. Result displays pass validity (active/expired/cancelled).
6. Guard proceeds with entry logging or denial.

### Data Model
- Pass code extracted from QR (8-character alphanumeric)
- Lookup: `GuestPassRepository.findPassByCode(passCode)`
- Returns: GuestPass object with `expiresAt`, `status`, sponsor info

### Fragment Implementation
- **Class**: `GuardQrScanFragment extends Fragment`
- **Layout**: `R.layout.fragment_guard_qr_scan`
- **Repository**: `GuestPassRepository` with pass code lookup
- **Camera Library**: Android's built-in camera or ML Kit for QR detection

### Known Constraints
- QR decode accuracy depends on camera quality
- Fallback manual entry required for poor lighting
- Pass must be active and not expired
