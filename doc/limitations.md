# GLITCH — Known Limitations & Next Steps (Part 3 Checkpoint)

This document lists the main limitations of the current **Part 3 prototype** of the *Campus Gate Access System* and the next steps planned for later milestones.  
Scope and terminology match the implemented roles and flows in the app (**guard, faculty, staff, student, admin**) and the current Firebase/Firestore architecture.

---

## Known limitations at Part 3 checkpoint

### Account provisioning / signup
- **No signup flow:** The app is login-only. Accounts must be provisioned by an admin in **Firebase Authentication** and must also have a matching **Firestore profile** under `users/{uid}`.
- **Admin user management does not create Firebase Auth users:** The admin UI can manage Firestore profile metadata (and active status), but it does not create/update Firebase Auth credentials.

### QR guest pass scanning
- **No camera-based QR scanning yet:** The guard “QR scan” milestone is implemented as a **pass-code input flow** rather than a camera scanner.

### Export / reporting
- **Audit export is CSV-first:** The current export workflow prioritizes **CSV export**; PDF export (if required) is not implemented at this checkpoint.

### Search / performance (prototype tradeoffs)
- Some lookup/search behavior is implemented in a **prototype-friendly way** (e.g., limited queries / client-side filtering in places) and may not scale to very large datasets without pagination/indexing improvements.

### Offline behavior
- The prototype assumes **active internet connectivity** for core workflows (authentication, dashboard updates, writes). Offline-first behavior is not implemented.

### Security hardening
- The prototype relies on Firebase Authentication baseline security and does not yet include additional hardening such as:
  - advanced rate-limiting / lockout policies at the app layer,
  - role-based permission modeling beyond the defined roles (guard/faculty/staff/student/admin).

---

## Planned next steps

- Implement **camera-based QR scanning** for guest pass verification.
- Improve **query scalability** (pagination, indexes, and reducing client-side filtering where possible).
- Expand **export/reporting** (PDF export if required).
- Add **robust error handling** and retry strategies for transient Firestore/network failures.
- Strengthen **admin provisioning workflow** (documented process, clearer constraints, optional tooling to assist creating Auth + profile consistently).
