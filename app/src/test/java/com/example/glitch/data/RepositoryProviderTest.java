package com.example.glitch.data;

import static org.junit.Assert.assertSame;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.glitch.auth.AuthRepository;
import com.example.glitch.model.CredentialVerificationResult;
import com.example.glitch.model.DashboardState;
import com.example.glitch.model.UserProfile;
import com.google.firebase.Timestamp;

import org.junit.After;
import org.junit.Test;

import java.util.Collections;

/**
 * Unit tests for repository override wiring used in tests.
 */
public class RepositoryProviderTest {

    @After
    public void tearDown() {
        RepositoryProvider.clearOverride();
    }

    @Test
    public void entryRequestOverride_isReturnedByProvider() {
        EntryRequestRepository fakeA = new FakeEntryRequestRepository();
        EntryRequestRepository fakeB = new FakeEntryRequestRepository();

        RepositoryProvider.setOverrideRepository(fakeA);
        assertSame(fakeA, RepositoryProvider.getRepository());

        RepositoryProvider.clearOverride();
        RepositoryProvider.setOverrideRepository(fakeB);
        assertSame(fakeB, RepositoryProvider.getRepository());
    }

    @Test
    public void authOverride_isReturnedByProvider() {
        AuthRepository fakeA = new FakeAuthRepository();
        AuthRepository fakeB = new FakeAuthRepository();

        RepositoryProvider.setOverrideAuthRepository(fakeA);
        assertSame(fakeA, RepositoryProvider.getAuthRepository());

        RepositoryProvider.clearOverride();
        RepositoryProvider.setOverrideAuthRepository(fakeB);
        assertSame(fakeB, RepositoryProvider.getAuthRepository());
    }

    private static final class FakeEntryRequestRepository implements EntryRequestRepository {
        @Override
        public void listenActiveRequests(@NonNull RequestListListener listener) {
            listener.onData(Collections.emptyList());
        }

        @Override
        public void listenDashboardState(@NonNull DashboardStateListener listener) {
            listener.onData(DashboardState.defaultState());
        }

        @Override
        public void searchRequests(@NonNull String query, @NonNull RequestListListener listener) {
            listener.onData(Collections.emptyList());
        }

        @Override
        public void verifyCredential(@NonNull String identifier, @NonNull CredentialListener listener) {
            listener.onData(new CredentialVerificationResult(false, identifier, "", "Not implemented in fake"));
        }

        @Override
        public void createEntryRequest(
                @NonNull String requesterUid,
                @NonNull String requesterRole,
                @NonNull String guestName,
                @NonNull String guestIdNumber,
                @NonNull String guestPhone,
                @NonNull String hostName,
                @Nullable Timestamp expiresAt,
                @NonNull CompletionCallback callback
        ) {
            callback.onComplete(true, "ok", null);
        }

        @Override
        public void logEntry(@NonNull String requestId, @NonNull CompletionCallback callback) {
            callback.onComplete(true, "ok", null);
        }

        @Override
        public void logExit(@NonNull String requestId, @NonNull CompletionCallback callback) {
            callback.onComplete(true, "ok", null);
        }

        @Override
        public void denyRequest(@NonNull String requestId, @NonNull String reason, @NonNull CompletionCallback callback) {
            callback.onComplete(true, "ok", null);
        }

        @Override
        public void reportViolation(@NonNull String requestId, @NonNull CompletionCallback callback) {
            callback.onComplete(true, "ok", null);
        }

        @Override
        public void listenRequestsByRequester(@NonNull String requesterUid, @NonNull RequestListListener listener) {
            listener.onData(Collections.emptyList());
        }

        @Override
        public void removeListeners() {
        }
    }

    private static final class FakeAuthRepository implements AuthRepository {
        @Override
        public void login(@NonNull String email, @NonNull String password, @NonNull AuthCallback callback) {
            callback.onResult(false, null, "fake");
        }

        @Override
        public void logout() {
        }

        @Override
        public void validateCurrentSession(@NonNull AuthCallback callback) {
            callback.onResult(false, null, "fake");
        }

        @Nullable
        @Override
        public UserProfile getCurrentProfile() {
            return null;
        }
    }
}
