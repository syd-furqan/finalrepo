package com.example.glitch.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.Timestamp;

import java.util.Collections;
import java.util.HashMap;
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
	private final int schemaVersion;
	private final String entityType;
	private final String entityId;
	private final String source;
	private final String outcome;
	private final String reasonCode;
	private final String gateLabel;
	private final Map<String, Object> metadata;

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
		this(
				id,
				eventType,
				actorUid,
				actorRole,
				requestId,
				description,
				createdAt,
				1,
				"",
				"",
				"",
				"",
				"",
				"",
				Collections.emptyMap()
		);
	}

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
			@Nullable Timestamp createdAt,
			int schemaVersion,
			@NonNull String entityType,
			@NonNull String entityId,
			@NonNull String source,
			@NonNull String outcome,
			@NonNull String reasonCode,
			@NonNull String gateLabel,
			@NonNull Map<String, Object> metadata
	) {
		this.id = id;
		this.eventType = eventType;
		this.actorUid = actorUid;
		this.actorRole = actorRole;
		this.requestId = requestId;
		this.description = description;
		this.createdAt = createdAt;
		this.schemaVersion = schemaVersion;
		this.entityType = entityType;
		this.entityId = entityId;
		this.source = source;
		this.outcome = outcome;
		this.reasonCode = reasonCode;
		this.gateLabel = gateLabel;
		this.metadata = Collections.unmodifiableMap(new HashMap<>(metadata));
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
		String requestId = asString(map.get("requestId"));
		String entityId = asString(map.get("entityId"));
		String entityType = asString(map.get("entityType"));
		if (requestId.isEmpty() && "entry_request".equals(entityType)) {
			requestId = entityId;
		}
		if (entityId.isEmpty()) {
			entityId = requestId;
		}
		return new AccessEvent(
				id,
				asString(map.get("eventType")),
				asString(map.get("actorUid")),
				asString(map.get("actorRole")),
				requestId,
				asString(map.get("description")),
				asTimestamp(map.get("createdAt")),
				asInt(map.get("schemaVersion"), 1),
				entityType,
				entityId,
				asString(map.get("source")),
				asString(map.get("outcome")),
				asString(map.get("reasonCode")),
				asString(map.get("gateLabel")),
				asMap(map.get("metadata"))
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

	public int getSchemaVersion() {
		return schemaVersion;
	}

	@NonNull
	public String getEntityType() {
		return entityType;
	}

	@NonNull
	public String getEntityId() {
		return entityId;
	}

	@NonNull
	public String getSource() {
		return source;
	}

	@NonNull
	public String getOutcome() {
		return outcome;
	}

	@NonNull
	public String getReasonCode() {
		return reasonCode;
	}

	@NonNull
	public String getGateLabel() {
		return gateLabel;
	}

	@NonNull
	public Map<String, Object> getMetadata() {
		return metadata;
	}

	@NonNull
	public String getCorrelationId() {
		if (!entityId.trim().isEmpty()) {
			return entityId;
		}
		return requestId;
	}

	@NonNull
	private static String asString(@Nullable Object value) {
		return value == null ? "" : String.valueOf(value);
	}

	private static int asInt(@Nullable Object value, int fallback) {
		if (value instanceof Number) {
			return ((Number) value).intValue();
		}
		if (value == null) {
			return fallback;
		}
		try {
			return Integer.parseInt(String.valueOf(value));
		} catch (NumberFormatException ignored) {
			return fallback;
		}
	}

	@Nullable
	private static Timestamp asTimestamp(@Nullable Object value) {
		if (value instanceof Timestamp) {
			return (Timestamp) value;
		}
		return null;
	}

	@NonNull
	private static Map<String, Object> asMap(@Nullable Object value) {
		if (!(value instanceof Map)) {
			return Collections.emptyMap();
		}
		Map<?, ?> raw = (Map<?, ?>) value;
		Map<String, Object> result = new HashMap<>();
		for (Map.Entry<?, ?> entry : raw.entrySet()) {
			if (entry.getKey() == null) {
				continue;
			}
			result.put(String.valueOf(entry.getKey()), entry.getValue());
		}
		return result;
	}
}
