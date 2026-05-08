package com.example.glitch.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.glitch.auth.AuthRepository;
import com.example.glitch.auth.FirebaseAuthRepository;

/**
 * Lightweight repository locator for runtime and test-time wiring.
 * Pattern: Service locator with optional override for instrumentation tests.
 * Known issue: global override is process-wide and must be cleared after tests.
 */
public final class RepositoryProvider {
    private static EntryRequestRepository overrideEntryRequestRepository;
    private static AuthRepository overrideAuthRepository;

    private static EntryRequestRepository entryRequestRepository;
    private static AuthRepository authRepository;
    private static GuestPassRepository guestPassRepository;
    private static VehicleRequestRepository vehicleRequestRepository;
    private static NotificationRepository notificationRepository;
    private static AuditLogRepository auditLogRepository;
    private static UserManagementRepository userManagementRepository;
    private static VerificationRulesRepository verificationRulesRepository;
    private static AlertRepository alertRepository;
    private static InterventionRepository interventionRepository;
    private static ViolationReportRepository violationReportRepository;

    private RepositoryProvider() {
    }

    /**
     * Returns active repository instance.
     */
    @NonNull
    public static EntryRequestRepository getRepository() {
        if (overrideEntryRequestRepository != null) {
            return overrideEntryRequestRepository;
        }
        if (entryRequestRepository == null) {
            entryRequestRepository = new FirestoreEntryRequestRepository();
        }
        return entryRequestRepository;
    }

    /**
     * Returns auth repository instance.
     */
    @NonNull
    public static AuthRepository getAuthRepository() {
        if (overrideAuthRepository != null) {
            return overrideAuthRepository;
        }
        if (authRepository == null) {
            authRepository = new FirebaseAuthRepository();
        }
        return authRepository;
    }

    /**
     * Returns guest pass repository instance.
     */
    @NonNull
    public static GuestPassRepository getGuestPassRepository() {
        if (guestPassRepository == null) {
            guestPassRepository = new FirestoreGuestPassRepository();
        }
        return guestPassRepository;
    }

    /**
     * Returns vehicle request repository instance.
     */
    @NonNull
    public static VehicleRequestRepository getVehicleRequestRepository() {
        if (vehicleRequestRepository == null) {
            vehicleRequestRepository = new FirestoreVehicleRequestRepository();
        }
        return vehicleRequestRepository;
    }

    /**
     * Returns notification repository instance.
     */
    @NonNull
    public static NotificationRepository getNotificationRepository() {
        if (notificationRepository == null) {
            notificationRepository = new FirestoreNotificationRepository();
        }
        return notificationRepository;
    }

    /**
     * Returns audit log repository instance.
     */
    @NonNull
    public static AuditLogRepository getAuditLogRepository() {
        if (auditLogRepository == null) {
            auditLogRepository = new FirestoreAuditLogRepository();
        }
        return auditLogRepository;
    }

    /**
     * Returns user management repository instance.
     */
    @NonNull
    public static UserManagementRepository getUserManagementRepository() {
        if (userManagementRepository == null) {
            userManagementRepository = new FirestoreUserManagementRepository();
        }
        return userManagementRepository;
    }

    /**
     * Returns verification rules repository instance.
     */
    @NonNull
    public static VerificationRulesRepository getVerificationRulesRepository() {
        if (verificationRulesRepository == null) {
            verificationRulesRepository = new FirestoreVerificationRulesRepository();
        }
        return verificationRulesRepository;
    }

    /**
     * Returns security alert repository instance.
     */
    @NonNull
    public static AlertRepository getAlertRepository() {
        if (alertRepository == null) {
            alertRepository = new FirestoreAlertRepository();
        }
        return alertRepository;
    }

    /**
     * Returns intervention repository instance.
     */
    @NonNull
    public static InterventionRepository getInterventionRepository() {
        if (interventionRepository == null) {
            interventionRepository = new FirestoreInterventionRepository();
        }
        return interventionRepository;
    }

    /**
     * Returns violation report repository instance.
     */
    @NonNull
    public static ViolationReportRepository getViolationReportRepository() {
        if (violationReportRepository == null) {
            violationReportRepository = new FirestoreViolationReportRepository();
        }
        return violationReportRepository;
    }

    /**
     * Injects test repository override.
     */
    public static void setOverrideRepository(@Nullable EntryRequestRepository repository) {
        overrideEntryRequestRepository = repository;
    }

    /**
     * Injects auth repository override for tests.
     */
    public static void setOverrideAuthRepository(@Nullable AuthRepository repository) {
        overrideAuthRepository = repository;
    }

    /**
     * Clears any test override and restores Firestore-backed default.
     */
    public static void clearOverride() {
        overrideEntryRequestRepository = null;
        overrideAuthRepository = null;
        entryRequestRepository = null;
        authRepository = null;
        guestPassRepository = null;
        vehicleRequestRepository = null;
        notificationRepository = null;
        auditLogRepository = null;
        userManagementRepository = null;
        verificationRulesRepository = null;
        alertRepository = null;
        interventionRepository = null;
        violationReportRepository = null;
    }
}
