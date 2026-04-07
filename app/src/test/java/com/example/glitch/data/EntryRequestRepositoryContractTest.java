package com.example.glitch.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.glitch.model.DashboardState;
import com.example.glitch.model.EntryRequest;
import com.google.firebase.Timestamp;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Contract test for write behavior using a fake repository implementation.
 */
public class EntryRequestRepositoryContractTest {

    @Test
    public void logExit_marksRequestExited() {
        FakeEntryRequestRepository repository = new FakeEntryRequestRepository();
        final boolean[] callbackSuccess = {false};

        repository.logExit("request-1", (success, message, exception) -> callbackSuccess[0] = success);

        assertTrue(callbackSuccess[0]);
        assertEquals("exited", repository.statusByRequestId.get("request-1"));
    }

    private static class FakeEntryRequestRepository implements EntryRequestRepository {
        final Map<String, String> statusByRequestId = new HashMap<>();

        FakeEntryRequestRepository() {
            statusByRequestId.put("request-1", "active");
        }

        @Override
        public void listenActiveRequests(@NonNull RequestListListener listener) {
            listener.onData(new ArrayList<>());
        }

        @Override
        public void listenDashboardState(@NonNull DashboardStateListener listener) {
            listener.onData(DashboardState.defaultState());
        }

        @Override
        public void searchRequests(@NonNull String query, @NonNull RequestListListener listener) {
            listener.onData(new ArrayList<>());
        }

        @Override
        public void verifyCredential(@NonNull String identifier, @NonNull CredentialListener listener) {
            listener.onData(new com.example.glitch.model.CredentialVerificationResult(
                    false,
                    identifier,
                    "",
                    "not implemented in fake"
            ));
        }

        @Override
        public void createEntryRequest(
                @NonNull String requesterUid,
                @NonNull String requesterRole,
                @NonNull String guestName,
                @NonNull String guestIdNumber,
                @NonNull String gateLabel,
                @NonNull String hostName,
                @Nullable Timestamp expiresAt,
                @NonNull CompletionCallback callback
        ) {
            callback.onComplete(true, "ok", null);
        }

        @Override
        public void logEntry(@NonNull String requestId, @NonNull String gateLabel, @NonNull CompletionCallback callback) {
            statusByRequestId.put(requestId, "active");
            callback.onComplete(true, "ok", null);
        }

        @Override
        public void logExit(@NonNull String requestId, @NonNull CompletionCallback callback) {
            statusByRequestId.put(requestId, "exited");
            callback.onComplete(true, "ok", null);
        }

        @Override
        public void denyRequest(@NonNull String requestId, @NonNull String reason, @NonNull CompletionCallback callback) {
            statusByRequestId.put(requestId, "denied");
            callback.onComplete(true, "ok", null);
        }

        @Override
        public void removeListeners() {
            // No-op for fake repository.
        }
    }
}
