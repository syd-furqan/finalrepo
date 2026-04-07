package com.example.glitch.data;

import androidx.annotation.NonNull;

import com.example.glitch.model.NotificationItem;

import java.util.List;

/**
 * Contract for realtime user notification feeds.
 * Pattern: Read-focused repository for notification timelines.
 * Known issue: marking notifications as read is not implemented in v1.
 */
public interface NotificationRepository {

	void listenNotifications(@NonNull String uid, @NonNull NotificationListener listener);

	void removeListeners();

	interface NotificationListener {
		void onData(@NonNull List<NotificationItem> notifications);

		void onError(@NonNull Exception exception);
	}
}