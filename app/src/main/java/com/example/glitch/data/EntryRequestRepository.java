package com.example.glitch.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.glitch.model.CredentialVerificationResult;
import com.example.glitch.model.DashboardState;
import com.example.glitch.model.EntryRequest;

import com.google.firebase.Timestamp;
import java.util.List;

/**
 * Contract for reading and updating guard dashboard entry requests.
 * Pattern: Repository abstraction to isolate Firestore from UI logic.
 * Known issue: v1 supports only active request list + log exit workflow.
 * US-09: listenRequestsByRequester added for staff access-request status view.
 */
public interface EntryRequestRepository {

	/**
	 * Starts realtime updates for active entry requests.
	 */
	void listenActiveRequests(@NonNull RequestListListener listener);

	/**
	 * Starts realtime updates for dashboard status/protocol document.
	 */
	void listenDashboardState(@NonNull DashboardStateListener listener);

	/**
	 * Performs one-shot search by guest name, id number, host or request id.
	 */
	void searchRequests(@NonNull String query, @NonNull RequestListListener listener);

	/**
	 * Verifies a credential identifier against the university registry proxy collection.
	 */
	void verifyCredential(@NonNull String identifier, @NonNull CredentialListener listener);

	/**
	 * Creates a new entry request (used by faculty submissions).
	 */
	void createEntryRequest(
			@NonNull String requesterUid,
			@NonNull String requesterRole,
			@NonNull String guestName,
			@NonNull String guestIdNumber,
			@NonNull String hostName,
			@Nullable Timestamp expiresAt,
			@NonNull CompletionCallback callback
	);

	/**
	 * Writes request approval/entry state and appends an access event.
	 */
	void logEntry(@NonNull String requestId, @NonNull CompletionCallback callback);

	/**
	 * Writes request exit state for a given entry request document id.
	 */
	void logExit(@NonNull String requestId, @NonNull CompletionCallback callback);

	/**
	 * Denies request and persists denial reason.
	 */
	void denyRequest(@NonNull String requestId, @NonNull String reason, @NonNull CompletionCallback callback);

	/**
	 * Starts realtime updates for all requests submitted by a specific requester (US-09).
	 */
	void listenRequestsByRequester(@NonNull String requesterUid, @NonNull RequestListListener listener);

	/**
	 * Stops all active realtime listeners.
	 */
	void removeListeners();

	interface RequestListListener {
		void onData(@NonNull List<EntryRequest> requests);

		void onError(@NonNull Exception exception);
	}

	interface DashboardStateListener {
		void onData(@NonNull DashboardState state);

		void onError(@NonNull Exception exception);
	}

	interface CredentialListener {
		void onData(@NonNull CredentialVerificationResult result);

		void onError(@NonNull Exception exception);
	}

	interface CompletionCallback {
		void onComplete(boolean success, @NonNull String message, @Nullable Exception exception);
	}
}
