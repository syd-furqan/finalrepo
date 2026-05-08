package com.example.glitch.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.glitch.model.FineCaseRecord;
import com.example.glitch.model.GuestBanRecord;
import com.example.glitch.model.StudentWarning;

import java.util.List;

public interface InterventionRepository {

    void isGuestBanned(@NonNull String cnic, @NonNull BanCheckCallback callback);

    void banGuest(
            @NonNull String cnic,
            @NonNull String adminUid,
            @NonNull String reasonCode,
            @NonNull String sourceReportId,
            @NonNull OperationCallback callback
    );

    void unbanGuest(
            @NonNull String banId,
            @NonNull String adminUid,
            @NonNull OperationCallback callback
    );

    void createChargeForReport(
            @NonNull String violationReportId,
            @NonNull String sponsorUid,
            @NonNull String guestName,
            @NonNull String guestIdNumber,
            @NonNull String violationLevel,
            @NonNull String adminUid,
            @NonNull OperationCallback callback
    );

    void issueWarning(
            @NonNull String targetUid,
            @NonNull String targetName,
            @NonNull String targetRole,
            @NonNull String violationReportId,
            @NonNull String violationLevel,
            @NonNull String detail,
            @NonNull String adminUid,
            @NonNull OperationCallback callback
    );

    void requestChargeRemoval(
            @NonNull String chargeId,
            @NonNull String paymentNote,
            @NonNull String studentUid,
            @NonNull OperationCallback callback
    );

    void approveChargeRemoval(
            @NonNull String chargeId,
            @NonNull String adminUid,
            @NonNull OperationCallback callback
    );

    void rejectChargeRemoval(
            @NonNull String chargeId,
            @NonNull String adminUid,
            @NonNull OperationCallback callback
    );

    void listenBans(@NonNull BanListListener listener);

    void listenFineCases(@NonNull FineListListener listener);

    void listenChargesByStudent(@NonNull String studentUid, @NonNull FineListListener listener);

    void listenWarningsByStudent(@NonNull String studentUid, @NonNull WarningListListener listener);

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

    interface WarningListListener {
        void onData(@NonNull List<StudentWarning> warnings);
        void onError(@NonNull Exception exception);
    }
}
