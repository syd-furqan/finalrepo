package com.example.glitch.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.glitch.model.RegisteredVehicleRecord;
import com.example.glitch.model.VehicleRemovalDraft;
import com.example.glitch.model.VehicleRequestRecord;
import com.example.glitch.model.VehicleRegistrationDraft;

import java.util.List;

/**
 * Contract for sponsor vehicle registration/removal applications and admin review.
 */
public interface VehicleRequestRepository {

    void submitRegistrationApplication(
            @NonNull VehicleRegistrationDraft draft,
            @NonNull OperationCallback callback
    );

    default void submitRegistrationApplication(
            @NonNull VehicleRegistrationDraft draft,
            @NonNull OperationCallback callback,
            @NonNull UploadProgressListener progressListener
    ) {
        submitRegistrationApplication(draft, callback);
    }

    void submitRemovalApplication(
            @NonNull VehicleRemovalDraft draft,
            @NonNull OperationCallback callback
    );

    void listenVehicleRequests(@NonNull String requesterUid, @NonNull RequestListListener listener);

    void listenOpenVehicleRequest(@NonNull String requesterUid, @NonNull SingleRequestListener listener);

    void listenAllVehicleRequests(@NonNull RequestListListener listener);

    void listenRegisteredVehicles(@NonNull String requesterUid, @NonNull RegisteredVehicleListListener listener);

    void cancelVehicleRequest(@NonNull String requestId, @NonNull OperationCallback callback);

    void markVehicleRequestReceived(
            @NonNull String requestId,
            @NonNull String reviewerUid,
            @NonNull OperationCallback callback
    );

    void reviewVehicleRequest(
            @NonNull String requestId,
            @NonNull String reviewerUid,
            boolean approved,
            @NonNull String reviewNote,
            @NonNull OperationCallback callback
    );

    void removeListeners();

    interface RequestListListener {
        void onData(@NonNull List<VehicleRequestRecord> requests);

        void onError(@NonNull Exception exception);
    }

    interface SingleRequestListener {
        void onData(@Nullable VehicleRequestRecord request);

        void onError(@NonNull Exception exception);
    }

    interface RegisteredVehicleListListener {
        void onData(@NonNull List<RegisteredVehicleRecord> vehicles);

        void onError(@NonNull Exception exception);
    }

    interface OperationCallback {
        void onComplete(boolean success, @NonNull String message, @Nullable Exception exception);
    }

    interface UploadProgressListener {
        void onProgress(int percent, long bytesTransferred, long totalBytes);
    }
}
