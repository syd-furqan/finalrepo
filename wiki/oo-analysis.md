# Object-Oriented Analysis (CRC Cards)
CS360 – Campus Gate Access System  
Team GLITCH

Afnan Ahmad Baig 27100206

---

## 1. Introduction

This document presents the initial object-oriented analysis of our Campus Gate Access System.

We use CRC cards (Class–Responsibility–Collaborator) to identify the main classes in the system, what each class is responsible for, and which other classes it works with.

The classes below are based directly on the user stories in our product backlog.

---

# 2. CRC Cards

---

## Class: Visitor

**Responsibilities**
- Store visitor information (name, ID/CNIC, vehicle number).
- Submit an access request to enter campus (UserStory[US]-06, US-10).
- Receive or generate a guest pass.
- View the status of a request (US-09).
- Cancel a guest pass if needed (US-12).

**Collaborators**
- AccessRequest  
- CredentialVerifier  
- LogEntry  

---

## Class: Guard

**Responsibilities**
- View the real-time dashboard of active requests (US-01).
- Search visitor details by name, ID, or vehicle number (US-02).
- Verify credentials before granting access (US-03).
- Approve or deny access requests (US-05).
- Scan QR-based guest passes (US-17).
- Record entry and exit events (US-04).

**Collaborators**
- Visitor  
- AccessRequest  
- CredentialVerifier  
- LogEntry  

---

## Class: AccessRequest

**Responsibilities**
- Store all request details (visitor, sponsor, timestamps).
- Track the request status (Pending / Approved / Denied).
- Connect visitor and sponsor (faculty, staff, or student).
- Handle expiry time for guest passes (US-11).
- Trigger notifications when status changes (US-07).

**Collaborators**
- Visitor  
- Guard  
- CredentialVerifier  
- LogEntry  

---

## Class: LogEntry

**Responsibilities**
- Record entry time (US-04).
- Record exit time (US-04).
- Maintain a full audit history for security review (US-13).
- Provide data for exporting reports (US-15).

**Collaborators**
- Guard  
- Visitor  
- AccessRequest  

---

## Class: CredentialVerifier

**Responsibilities**
- Verify identity using university database (US-03).
- Check ID expiry and rule compliance (US-16).
- Check banned lists (US-16).
- Detect repeated failed attempts and trigger alerts (US-18).

**Collaborators**
- Visitor  
- Guard  
- AccessRequest  

---

## Class: Admin

**Responsibilities**
- Add, edit, or deactivate user accounts (US-14).
- View full audit logs (US-13).
- Export audit reports as CSV or PDF (US-15).
- Configure verification rules (US-16).

**Collaborators**
- LogEntry  
- CredentialVerifier  
- Guard  
- Visitor  

---

## Class: GuestPass

**Responsibilities**
- Store guest pass details (pass ID, QR code, expiry time).
- Ensure the pass is single-use or time-limited (US-11).
- Allow cancellation of a pass (US-12).
- Connect student-issued passes to access requests (US-10).

**Collaborators**
- Visitor  
- AccessRequest  
- Guard  
- CredentialVerifier  

---

# 3. Responsibility Mapping Summary

- **Visitor** starts the process by creating or receiving an access request.
- **AccessRequest** acts as the central object that connects users, guards, and request status.
- **Guard** reviews requests, verifies credentials, and controls entry at the gate.
- **CredentialVerifier** handles all security checks and policy rules.
- **LogEntry** ensures that every entry and exit is properly recorded for audit purposes.
- **Admin** manages users, reports, and system rules.
- **GuestPass** manages QR-based visitor passes and expiry control.

---

# 4. Design Notes

- The system separates responsibilities clearly to follow good object-oriented design principles.
- Security logic is kept inside the CredentialVerifier class to avoid mixing it with other responsibilities.
- AccessRequest is the central coordinating class in the system.
- LogEntry ensures traceability and accountability for all gate activities.

This design keeps the system modular, secure, and easy to maintain in future development stages.

---

# End of Document
