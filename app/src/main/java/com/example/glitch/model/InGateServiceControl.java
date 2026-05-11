package com.example.glitch.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Global in-gate service-control state managed by admin announcements.
 */
public class InGateServiceControl {
    public static final String MODE_MANUAL = "manual";
    public static final String MODE_TIMED = "timed";

    private final boolean active;
    @NonNull
    private final List<String> affectedRoles;
    @NonNull
    private final String reason;
    @NonNull
    private final String mode;
    @Nullable
    private final Timestamp endsAt;
    @NonNull
    private final String announcementId;

    public InGateServiceControl(
            boolean active,
            @NonNull List<String> affectedRoles,
            @NonNull String reason,
            @NonNull String mode,
            @Nullable Timestamp endsAt,
            @NonNull String announcementId
    ) {
        this.active = active;
        this.affectedRoles = Collections.unmodifiableList(new ArrayList<>(affectedRoles));
        this.reason = reason;
        this.mode = mode;
        this.endsAt = endsAt;
        this.announcementId = announcementId;
    }

    @NonNull
    public static InGateServiceControl inactive() {
        return new InGateServiceControl(false, Collections.emptyList(), "", "", null, "");
    }

    @NonNull
    public static InGateServiceControl fromMap(@Nullable Map<String, Object> map) {
        if (map == null) {
            return inactive();
        }
        return new InGateServiceControl(
                asBoolean(map.get("isActive")),
                asStringList(map.get("affectedRoles")),
                asString(map.get("reason")),
                asString(map.get("mode")).trim().toLowerCase(Locale.US),
                asTimestamp(map.get("endsAt")),
                asString(map.get("announcementId"))
        );
    }

    public boolean isActive() {
        return active;
    }

    @NonNull
    public List<String> getAffectedRoles() {
        return affectedRoles;
    }

    @NonNull
    public String getReason() {
        return reason;
    }

    @NonNull
    public String getMode() {
        return mode;
    }

    @Nullable
    public Timestamp getEndsAt() {
        return endsAt;
    }

    @NonNull
    public String getAnnouncementId() {
        return announcementId;
    }

    public boolean isBlockingRole(@NonNull String rawRole) {
        if (!active) {
            return false;
        }
        String normalizedRole = rawRole.trim().toLowerCase(Locale.US);
        boolean roleAffected = false;
        for (String role : affectedRoles) {
            if (normalizedRole.equals(role.trim().toLowerCase(Locale.US))) {
                roleAffected = true;
                break;
            }
        }
        if (!roleAffected) {
            return false;
        }
        if (MODE_MANUAL.equals(mode)) {
            return true;
        }
        if (MODE_TIMED.equals(mode)) {
            return endsAt != null && endsAt.toDate().getTime() > System.currentTimeMillis();
        }
        return false;
    }

    private static boolean asBoolean(@Nullable Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return false;
    }

    @NonNull
    private static String asString(@Nullable Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    @NonNull
    private static List<String> asStringList(@Nullable Object value) {
        if (!(value instanceof List)) {
            return Collections.emptyList();
        }
        List<?> raw = (List<?>) value;
        List<String> result = new ArrayList<>();
        for (Object item : raw) {
            if (item == null) {
                continue;
            }
            String normalized = String.valueOf(item).trim().toLowerCase(Locale.US);
            if (!normalized.isEmpty()) {
                result.add(normalized);
            }
        }
        return result;
    }

    @Nullable
    private static Timestamp asTimestamp(@Nullable Object value) {
        if (value instanceof Timestamp) {
            return (Timestamp) value;
        }
        return null;
    }
}
