# GLITCH System Architecture - Class Diagram

## Key Classes & Relationships

### User Hierarchy
- **User** (Abstract Base)
  - Attributes: userId, email, password, role, createdAt
  - Methods: authenticate(), getProfile(), updateProfile()
  
- **Resident** (extends User)
  - Attributes: residentId, communityId, unitNumber, moveInDate, verified
  - Methods: registerVehicle(), requestGuestPass(), getGuestPasses(), updateVerification()

- **AdminUser** (extends User)
  - Attributes: adminId, permissions, canModifyRules, canViewAudit
  - Methods: verifyResident(), createRule(), generateReport(), revokeGuestPass()

### Core Domain Objects
- **GuestPass**
  - Attributes: passId, residentId, startTime, endTime, vehicleInfo, isActive
  - Methods: activate(), revoke(), extend(), isExpired()

- **VerificationRule**
  - Attributes: ruleId, ruleName, description, requiresApproval
  - Methods: evaluate(), update(), getValidators()

- **AuditLog**
  - Attributes: logId, userId, action, timestamp, details
  - Methods: recordAction(), getActionHistory()

### Repository Pattern
- **FirestoreRepository** (Abstract)
  - Methods: save(), delete(), query(), update()

- **FirestoreUserRepository** (extends FirestoreRepository)
  - Methods: getUserById(), getAllUsers(), getUsersByRole()

- **FirestoreVerificationRepository** (extends FirestoreRepository)
  - Methods: createRule(), getRules(), updateRule(), evaluateResidentVerification()

### System Services
- **AlertSystem**
  - Attributes: alertRecipients, messaging
  - Methods: sendAlert(), notifyAdmins(), parseAlertTrigger()

## Relationships
- Resident → GuestPass (1 to Many)
- VerificationRule → Resident (1 to Many)
- AuditLog → User (Many to 1)
- AlertSystem → AdminUser (1 to Many)
- FirestoreRepository ← All Repository implementations

## Design Patterns Applied
- Repository Pattern for data access abstraction
- Observer Pattern for alert notifications
- Strategy Pattern for verification rule evaluation
- Factory Pattern for user creation (Resident vs Admin)
