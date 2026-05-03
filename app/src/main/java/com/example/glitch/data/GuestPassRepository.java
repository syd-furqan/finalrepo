package com.example.glitch.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.glitch.model.GuestPass;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.List;

/**
 * Contract for creating, listing and cancelling guest passes.
 * Pattern: Repository interface for student and faculty pass lifecycle flows.
 */
public interface GuestPassRepository {

	void createGuestPass(
			@NonNull String sponsorUid,
			@NonNull String sponsorRole,
			@NonNull String sponsorName,
			@NonNull String sponsorEmail,
			@NonNull String guestName,
			@NonNull String guestIdNumber,
			int expiryHours,
			@NonNull OperationCallback callback
	);

	void issueGuestPassWithEntryRequest(
			@NonNull String sponsorUid,
			@NonNull String sponsorRole,
			@NonNull String sponsorName,
			@NonNull String sponsorEmail,
			@NonNull String guestName,
			@NonNull String guestIdNumber,
			int expiryHours,
			@NonNull IssueCallback callback
	);

	@Nullable
	ListenerRegistration listenGuestPasses(@NonNull String sponsorUid, @NonNull PassListListener listener);

	@Nullable
	ListenerRegistration listenArchivedGuestPasses(@NonNull String sponsorUid, @NonNull PassListListener listener);

	void cancelGuestPass(@NonNull String passId, @NonNull OperationCallback callback);

	void findPassByCode(@NonNull String passCode, @NonNull PassLookupListener listener);

	void markPassAdmitted(
			@NonNull String passId,
			@NonNull String admittedByUid,
			@NonNull String admissionMethod,
			@NonNull OperationCallback callback
	);

	void markPassAdmittedByEntryRequestId(
			@NonNull String entryRequestId,
			@NonNull String admittedByUid,
			@NonNull String admissionMethod,
			@NonNull OperationCallback callback
	);

	void markPassDeniedByEntryRequestId(
			@NonNull String entryRequestId,
			@NonNull String deniedByUid,
			@NonNull OperationCallback callback
	);

	/**
	 * Updates the linked guest pass to 'exited' status.
	 */
	void markPassExitedByEntryRequestId(
			@NonNull String entryRequestId,
			@NonNull OperationCallback callback
	);

	interface PassListListener {
		void onData(@NonNull List<GuestPass> passes);

		void onError(@NonNull Exception exception);
	}

	interface PassLookupListener {
		void onData(@Nullable GuestPass pass);

		void onError(@NonNull Exception exception);
	}

	interface IssueCallback {
		void onComplete(
				boolean success,
				@NonNull String message,
				@Nullable GuestPass issuedPass,
				@Nullable Exception exception
		);
	}

	interface OperationCallback {
		void onComplete(boolean success, @NonNull String message, @Nullable Exception exception);
	}
}
