package com.example.glitch.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.glitch.model.NotificationItem;

import java.util.List;

/**
 * Contract for realtime user notification feeds.
 * Pattern: Read-focused repository for notification timelines.
 * Known issue: repository intentionally keeps read-ack state simple (boolean + timestamp).
 */
public interface NotificationRepository {

	void listenNotifications(@NonNull String uid, @NonNull NotificationListener listener);

	void markNotificationRead(
			@NonNull String uid,
			@NonNull String notificationId,
			@NonNull OperationCallback callback
	);

	void markAllNotificationsRead(
			@NonNull String uid,
			@NonNull OperationCallback callback
	);

	void removeListeners();

	interface NotificationListener {
		void onData(@NonNull List<NotificationItem> notifications);

		void onError(@NonNull Exception exception);
	}

	interface OperationCallback {
		void onComplete(boolean success, @NonNull String message, @Nullable Exception exception);
	}
}