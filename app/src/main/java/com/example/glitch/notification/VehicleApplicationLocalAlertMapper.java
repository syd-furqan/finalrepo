package com.example.glitch.notification;

import androidx.annotation.Nullable;

import java.util.Locale;

/**
 * Maps Firestore vehicle-application changes to local alert types.
 */
public final class VehicleApplicationLocalAlertMapper {
    private VehicleApplicationLocalAlertMapper() {
    }

    @Nullable
    public static VehicleApplicationLocalAlertType resolve(
            @Nullable String changeType,
            @Nullable String previousStatus,
            @Nullable String currentStatus,
            @Nullable String requestKind
    ) {
        String normalizedChange = normalize(changeType);
        String prev = normalize(previousStatus);
        String curr = normalize(currentStatus);
        String kind = normalize(requestKind);

        if (curr.isEmpty() || curr.equals(prev)) {
            return null;
        }

        if ("added".equals(normalizedChange) && "submitted".equals(curr)) {
            return VehicleApplicationLocalAlertType.SUBMITTED;
        }

        if ("cancelled".equals(curr)) {
            return VehicleApplicationLocalAlertType.CANCELLED;
        }
        if ("denied".equals(curr)) {
            return VehicleApplicationLocalAlertType.DENIED;
        }
        if ("approved".equals(curr)) {
            if ("remove".equals(kind)) {
                return VehicleApplicationLocalAlertType.REMOVAL_APPROVED;
            }
            return VehicleApplicationLocalAlertType.APPROVED;
        }
        return null;
    }

    private static String normalize(@Nullable String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.getDefault());
    }
}
