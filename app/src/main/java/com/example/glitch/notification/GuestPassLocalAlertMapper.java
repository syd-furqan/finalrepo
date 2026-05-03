package com.example.glitch.notification;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;

/**
 * Maps Firestore guest-pass change metadata into local notification event types.
 */
public final class GuestPassLocalAlertMapper {
    private GuestPassLocalAlertMapper() {
    }

    @Nullable
    public static GuestPassLocalAlertType resolve(
            @NonNull String changeType,
            @Nullable String previousStatus,
            @NonNull String currentStatus
    ) {
        String normalizedChange = normalize(changeType);
        String previous = normalize(previousStatus);
        String current = normalize(currentStatus);

        if ("added".equals(normalizedChange)) {
            return "active".equals(current) ? GuestPassLocalAlertType.CREATED : null;
        }

        if (!"modified".equals(normalizedChange) || previous.equals(current)) {
            return null;
        }

        switch (current) {
            case "cancelled":
                return GuestPassLocalAlertType.CANCELLED;
            case "used":
                return GuestPassLocalAlertType.ADMITTED;
            case "denied":
                return GuestPassLocalAlertType.DENIED;
            case "overdue":
                return GuestPassLocalAlertType.OVERDUE;
            case "exited":
                return GuestPassLocalAlertType.EXITED;
            default:
                return null;
        }
    }

    @NonNull
    private static String normalize(@Nullable String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().toLowerCase(Locale.getDefault());
    }
}
