# GLITCH - Known Limitations & Gaps

## Rapid Fire Limitations

- **Single-Organization Deployment**: System designed for single institution; multi-org support not implemented
- **No Guest Pass Inheritance**: New resident guests must be re-registered if resident moves communities
- **Firebase Realtime Dependency**: No offline persistence; all operations require active internet connection
- **Manual Entry Verification**: No automatic vehicle/license plate verification against DMV/public databases
- **No SMS Notifications**: Alerts currently email-only; SMS integration pending
- **Dashboard Real-time Lag**: Admin dashboard refreshes on 30-second interval, not true real-time
- **No Bulk Operations**: Admin cannot bulk-verify residents or batch-update permissions
- **Single Session Per User**: No support for concurrent logins across devices
- **No Audit Trail Retention Limits**: Audit logs grow indefinitely with no automatic purging
- **Limited Role Granularity**: Only 3 roles (admin, resident, staff); no custom permission sets
- **No API Rate Limiting**: External integrations can overwhelm system with requests
- **Mobile-Only Verification**: Verification rules cannot be enforced via web interface
- **No Two-Factor Auth**: Security relies solely on password authentication
- **Placeholder UML Checkpoint**: Initial UML diagrams created; architecture evolved during development

## Known Gaps

### Architecture
- Guest pass module lacks comprehensive exception handling for edge cases
- No circuit breaker pattern for Firestore connection failures
- Missing retry logic for failed security repository operations

### Security
- Password reset tokens have no expiration; require manual admin revocation
- Verification rules stored unencrypted; migrating to encrypted storage in Phase 4
- No protection against brute-force login attempts

### Performance
- User management queries scale poorly beyond 5,000 residents
- Alert caching not implemented; each alert fetch queries Firestore directly
- No pagination on audit log views; loading all logs causes UI lag

### Compliance
- No GDPR delete compliance for resident data purging
- Audit trail does not track data deletion events

## Future Improvements
- Implement offline-first architecture with sync queue
- Add OAuth2 multi-provider authentication
- Multi-organization support with tenant isolation
- Comprehensive analytics dashboard
- Mobile app for iOS
