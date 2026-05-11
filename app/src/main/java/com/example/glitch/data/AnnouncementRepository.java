package com.example.glitch.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.glitch.model.InGateServiceControl;

import java.util.Set;

/**
 * Contract for admin announcements and in-gate service inhibition controls.
 */
public interface AnnouncementRepository {

    void publishAnnouncement(
            @NonNull String actorUid,
            @NonNull String actorRole,
            @NonNull String actorName,
            @NonNull String message,
            boolean allCampus,
            @NonNull Set<String> selectedRoles,
            boolean inhibitInGate,
            @NonNull Set<String> affectedRoles,
            boolean timed,
            @Nullable Long endsAtMillis,
            @NonNull OperationCallback callback
    );

    void restoreInGateServices(
            @NonNull String actorUid,
            @NonNull String actorRole,
            @NonNull String actorName,
            @NonNull String message,
            @NonNull OperationCallback callback
    );

    void getCurrentInGateControl(@NonNull ControlStateListener listener);

    interface ControlStateListener {
        void onData(@NonNull InGateServiceControl control);

        void onError(@NonNull Exception exception);
    }

    interface OperationCallback {
        void onComplete(boolean success, @NonNull String message, @Nullable Exception exception);
    }
}
