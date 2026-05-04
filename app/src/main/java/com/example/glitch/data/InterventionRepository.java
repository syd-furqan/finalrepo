package com.example.glitch.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.glitch.model.FineCaseRecord;
import com.example.glitch.model.GuestBanRecord;
import com.example.glitch.model.IncidentInterventionRecord;

import java.util.List;

/**
 * Contract for admin intervention operations (ban/fine) and incident linkage.
 */
public interface InterventionRepository {

    void isGuestBanned(@NonNull String cnic, @NonNull BanCheckCallback callback);

    void banGuest(
            @NonNull String cnic,
            @NonNull String adminUid,
            @NonNull String reasonCode,
            @NonNull String sourceAlertId,
            @NonNull String sourceRequestId,
            @NonNull OperationCallback callback
    );

    void unbanGuest(
            @NonNull String banId,
            @NonNull String adminUid,
            @NonNull OperationCallback callback
    );

    void issueFine(
            @NonNull String sponsorUid,
            @NonNull String requestId,
            @NonNull String alertId,
            double amount,
            @NonNull String currency,
            @NonNull String reasonCode,
            @NonNull String adminUid,
            @NonNull OperationCallback callback
    );

    void createChargeForAlert(
            @NonNull String alertId,
            @NonNull String requestId,
            @NonNull String sponsorUid,
            @NonNull String guestName,
            @NonNull String guestIdNumber,
            @NonNull String adminUid,
            @NonNull OperationCallback callback
    );

    void resolveChargePaid(
            @NonNull String chargeId,
            @NonNull String adminUid,
            @NonNull OperationCallback callback
    );

    void resolveChargeRemoved(
            @NonNull String chargeId,
            @NonNull String adminUid,
            @NonNull OperationCallback callback
    );

    void waiveFineByRequestId(
            @NonNull String requestId,
            @NonNull String adminUid,
            @NonNull OperationCallback callback
    );

    void settleFineByRequestId(
            @NonNull String requestId,
            @NonNull String adminUid,
            @NonNull OperationCallback callback
    );

    void closeIncident(
            @NonNull String alertId,
            @NonNull String requestId,
            @NonNull String adminUid,
            @NonNull String summary,
            @NonNull OperationCallback callback
    );

    void listenBans(@NonNull BanListListener listener);

    void listenFineCases(@NonNull FineListListener listener);

    void listenInterventions(@NonNull InterventionListListener listener);

    void removeListeners();

    interface BanCheckCallback {
        void onResult(boolean banned, @Nullable GuestBanRecord record, @NonNull String message);
    }

    interface OperationCallback {
        void onComplete(boolean success, @NonNull String message, @Nullable Exception exception);
    }

    interface BanListListener {
        void onData(@NonNull List<GuestBanRecord> bans);

        void onError(@NonNull Exception exception);
    }

    interface FineListListener {
        void onData(@NonNull List<FineCaseRecord> fineCases);

        void onError(@NonNull Exception exception);
    }

    interface InterventionListListener {
        void onData(@NonNull List<IncidentInterventionRecord> interventions);

        void onError(@NonNull Exception exception);
    }
}
