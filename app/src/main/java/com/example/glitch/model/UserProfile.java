package com.example.glitch.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.Timestamp;

import java.util.Locale;
import java.util.Map;

/**
 * Represents an authenticated application user profile loaded from Firestore.
 * Pattern: Immutable POJO for session and role-based routing.
 * Known issue: role values are expected to be normalized to lowercase in Firestore.
 */
public class UserProfile {
	private final String uid;
	private final String email;
	private final String role;
	private final boolean isActive;
	private final String displayName;
	private final String studentCategory;
	private final String studentId;
	private final Timestamp createdAt;
	private final Timestamp updatedAt;

	/**
	 * Creates an immutable user profile.
	 */
	public UserProfile(
			@NonNull String uid,
			@NonNull String email,
			@NonNull String role,
			boolean isActive,
			@NonNull String displayName,
			@NonNull String studentCategory,
			@NonNull String studentId,
			@Nullable Timestamp createdAt,
			@Nullable Timestamp updatedAt
	) {
		this.uid = uid;
		this.email = email;
		this.role = role;
		this.isActive = isActive;
		this.displayName = displayName;
		this.studentCategory = studentCategory;
		this.studentId = studentId.trim();
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
	}

	public UserProfile(
			@NonNull String uid,
			@NonNull String email,
			@NonNull String role,
			boolean isActive,
			@NonNull String displayName,
			@NonNull String studentCategory,
			@Nullable Timestamp createdAt,
			@Nullable Timestamp updatedAt
	) {
		this(uid, email, role, isActive, displayName, studentCategory, "", createdAt, updatedAt);
	}

	public UserProfile(
			@NonNull String uid,
			@NonNull String email,
			@NonNull String role,
			boolean isActive,
			@NonNull String displayName,
			@Nullable Timestamp createdAt,
			@Nullable Timestamp updatedAt
	) {
		this(uid, email, role, isActive, displayName, "", "", createdAt, updatedAt);
	}

	/**
	 * Maps Firestore data into a user profile.
	 */
	@NonNull
	public static UserProfile fromMap(@NonNull String uid, @Nullable Map<String, Object> map) {
		if (map == null) {
			return new UserProfile(uid, "", "", false, "", "", "", null, null);
		}
		return new UserProfile(
				uid,
				asString(map.get("email")),
				asString(map.get("role")).trim().toLowerCase(Locale.getDefault()),
				asBoolean(map.get("isActive")),
				asString(map.get("displayName")),
				asString(map.get("studentCategory")).trim().toLowerCase(Locale.getDefault()),
				asString(map.get("studentId")),
				asTimestamp(map.get("createdAt")),
				asTimestamp(map.get("updatedAt"))
		);
	}

	/**
	 * Returns true when role is one of the expected domain roles.
	 */
	public boolean hasSupportedRole() {
		return "guard".equals(role)
				|| "faculty".equals(role)
				|| "student".equals(role)
				|| "admin".equals(role)
				|| "monitor".equals(role);
	}

	/**
	 * @return authenticated Firebase uid.
	 */
	@NonNull
	public String getUid() {
		return uid;
	}

	/**
	 * @return user email address.
	 */
	@NonNull
	public String getEmail() {
		return email;
	}

	/**
	 * @return normalized role value.
	 */
	@NonNull
	public String getRole() {
		return role;
	}

	/**
	 * @return true when account is allowed to access app flows.
	 */
	public boolean isActive() {
		return isActive;
	}

	/**
	 * @return display name used across role home and list screens.
	 */
	@NonNull
	public String getDisplayName() {
		return displayName;
	}

	/**
	 * @return student category value used for parking-sticker eligibility.
	 */
	@NonNull
	public String getStudentCategory() {
		return studentCategory;
	}

	/**
	 * @return institution student id for student users.
	 */
	@NonNull
	public String getStudentId() {
		return studentId;
	}

	/**
	 * @return profile creation timestamp.
	 */
	@Nullable
	public Timestamp getCreatedAt() {
		return createdAt;
	}

	/**
	 * @return profile last update timestamp.
	 */
	@Nullable
	public Timestamp getUpdatedAt() {
		return updatedAt;
	}

	@NonNull
	private static String asString(@Nullable Object value) {
		return value == null ? "" : String.valueOf(value);
	}

	private static boolean asBoolean(@Nullable Object value) {
		if (value instanceof Boolean) {
			return (Boolean) value;
		}
		if (value instanceof Number) {
			return ((Number) value).intValue() != 0;
		}
		if (value instanceof String) {
			String normalized = ((String) value).trim().toLowerCase(Locale.getDefault());
			return "true".equals(normalized) || "1".equals(normalized) || "yes".equals(normalized);
		}
		return false;
	}

	@Nullable
	private static Timestamp asTimestamp(@Nullable Object value) {
		if (value instanceof Timestamp) {
			return (Timestamp) value;
		}
		return null;
	}
}
