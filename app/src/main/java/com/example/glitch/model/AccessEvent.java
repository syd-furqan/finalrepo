package com.example.glitch.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.Timestamp;

import java.util.Map;

/**
 * Represents an immutable audit event for gate activity.
 * Pattern: Immutable POJO for admin audit log and incident traces.
 * Known issue: event metadata is intentionally summarized for list views in v1.
 */
public class AccessEvent {
	private final String id;
	private final String eventType;
	private final String actorUid;
	private final String actorRole;
	private final String requestId;
	private final String description;
	private final Timestamp createdAt;

	/**
	 * Creates an immutable access event used by audit screens.
	 */
	public AccessEvent(
			@NonNull String id,
			@NonNull String eventType,
			@NonNull String actorUid,
			@NonNull String actorRole,
			@NonNull String requestId,
			@NonNull String description,
			@Nullable Timestamp createdAt
	) {
		this.id = id;
		this.eventType = eventType;
		this.actorUid = actorUid;
		this.actorRole = actorRole;
		this.requestId = requestId;
		this.description = description;
		this.createdAt = createdAt;
	}

	/**
	 * Maps Firestore document data into an {@link AccessEvent}.
	 *
	 * @param id Firestore document id.
	 * @param map Firestore field map.
	 * @return parsed event instance, or a safe empty fallback when {@code map} is null.
	 */
	@NonNull
	public static AccessEvent fromMap(@NonNull String id, @Nullable Map<String, Object> map) {
		if (map == null) {
			return new AccessEvent(id, "", "", "", "", "", null);
		}
		return new AccessEvent(
				id,
				asString(map.get("eventType")),
				asString(map.get("actorUid")),
				asString(map.get("actorRole")),
				asString(map.get("requestId")),
				asString(map.get("description")),
				asTimestamp(map.get("createdAt"))
		);
	}

	/**
	 * @return document id for this event.
	 */
	@NonNull
	public String getId() {
		return id;
	}

	/**
	 * @return normalized event type such as ENTRY or DENY.
	 */
	@NonNull
	public String getEventType() {
		return eventType;
	}

	/**
	 * @return uid of the user/actor that generated the event.
	 */
	@NonNull
	public String getActorUid() {
		return actorUid;
	}

	/**
	 * @return role label of the actor (guard/admin/etc.).
	 */
	@NonNull
	public String getActorRole() {
		return actorRole;
	}

	/**
	 * @return related entry request id for correlation in logs.
	 */
	@NonNull
	public String getRequestId() {
		return requestId;
	}

	/**
	 * @return human-readable description shown in admin log list.
	 */
	@NonNull
	public String getDescription() {
		return description;
	}

	/**
	 * @return creation timestamp from Firestore, if available.
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
}