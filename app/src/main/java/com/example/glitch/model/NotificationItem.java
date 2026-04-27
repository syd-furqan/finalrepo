package com.example.glitch.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.Timestamp;

import java.util.Map;

/**
 * Represents one user notification item.
 * Pattern: Immutable POJO for notifications inbox rendering.
 * Known issue: read receipts are tracked client-side and do not include actor metadata.
 */
public class NotificationItem {
	private final String id;
	private final String title;
	private final String message;
	private final String type;
	private final boolean isRead;
	private final Timestamp createdAt;

	/**
	 * Creates one immutable notification row for inbox rendering.
	 */
	public NotificationItem(
			@NonNull String id,
			@NonNull String title,
			@NonNull String message,
			@NonNull String type,
			boolean isRead,
			@Nullable Timestamp createdAt
	) {
		this.id = id;
		this.title = title;
		this.message = message;
		this.type = type;
		this.isRead = isRead;
		this.createdAt = createdAt;
	}

	/**
	 * Maps Firestore document data into a {@link NotificationItem}.
	 *
	 * @param id Firestore document id.
	 * @param map Firestore field map.
	 * @return parsed notification or empty fallback when map is null.
	 */
	@NonNull
	public static NotificationItem fromMap(@NonNull String id, @Nullable Map<String, Object> map) {
		if (map == null) {
			return new NotificationItem(id, "", "", "", false, null);
		}
		return new NotificationItem(
				id,
				asString(map.get("title")),
				asString(map.get("message")),
				asString(map.get("type")),
				asBoolean(map.get("isRead")),
				asTimestamp(map.get("createdAt"))
		);
	}

	/**
	 * @return Firestore document id.
	 */
	@NonNull
	public String getId() {
		return id;
	}

	/**
	 * @return notification title text.
	 */
	@NonNull
	public String getTitle() {
		return title;
	}

	/**
	 * @return notification body text.
	 */
	@NonNull
	public String getMessage() {
		return message;
	}

	/**
	 * @return notification type key (approval/denial/system).
	 */
	@NonNull
	public String getType() {
		return type;
	}

	/**
	 * @return true when this notification has been acknowledged by the user.
	 */
	public boolean isRead() {
		return isRead;
	}

	/**
	 * @return timestamp when this notification was created.
	 */
	@Nullable
	public Timestamp getCreatedAt() {
		return createdAt;
	}

	@NonNull
	private static String asString(@Nullable Object value) {
		return value == null ? "" : String.valueOf(value);
	}

	@Nullable
	private static Timestamp asTimestamp(@Nullable Object value) {
		if (value instanceof Timestamp) {
			return (Timestamp) value;
		}
		return null;
	}

	private static boolean asBoolean(@Nullable Object value) {
		if (value instanceof Boolean) {
			return (Boolean) value;
		}
		return false;
	}
}