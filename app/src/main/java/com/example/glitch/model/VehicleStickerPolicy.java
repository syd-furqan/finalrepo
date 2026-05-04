package com.example.glitch.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;

/**
 * Maps role + student category to canonical parking sticker types.
 */
public final class VehicleStickerPolicy {
    public static final String STICKER_FACULTY = "FC";
    public static final String STICKER_MAIN_PARKING = "MP";
    public static final String STICKER_RESIDENT = "RS";
    public static final String STICKER_REDC = "REDC";

    public static final String CATEGORY_DAY_SCHOLAR = "day_scholar";
    public static final String CATEGORY_HOSTELITE = "hostelite";
    public static final String CATEGORY_REDC = "redc";

    private VehicleStickerPolicy() {
    }

    @NonNull
    public static String normalizeStudentCategory(@Nullable String raw) {
        if (raw == null) {
            return "";
        }
        String normalized = raw.trim().toLowerCase(Locale.getDefault());
        if (normalized.isEmpty()) {
            return "";
        }
        if ("day scholar".equals(normalized) || "dayscholar".equals(normalized)) {
            return CATEGORY_DAY_SCHOLAR;
        }
        if ("hostelite".equals(normalized) || "hostelite student".equals(normalized)) {
            return CATEGORY_HOSTELITE;
        }
        if ("redc".equals(normalized) || "redc program".equals(normalized)) {
            return CATEGORY_REDC;
        }
        return normalized;
    }

    @NonNull
    public static String resolveStickerType(@NonNull String role, @Nullable String studentCategory) {
        String normalizedRole = role.trim().toLowerCase(Locale.getDefault());
        if ("faculty".equals(normalizedRole)) {
            return STICKER_FACULTY;
        }
        if (!"student".equals(normalizedRole)) {
            return "";
        }
        String category = normalizeStudentCategory(studentCategory);
        if (CATEGORY_REDC.equals(category)) {
            return STICKER_REDC;
        }
        if (CATEGORY_HOSTELITE.equals(category)) {
            return STICKER_RESIDENT;
        }
        if (CATEGORY_DAY_SCHOLAR.equals(category)) {
            return STICKER_MAIN_PARKING;
        }
        return "";
    }

    public static boolean isSupportedStudentCategory(@Nullable String raw) {
        String category = normalizeStudentCategory(raw);
        return CATEGORY_DAY_SCHOLAR.equals(category)
                || CATEGORY_HOSTELITE.equals(category)
                || CATEGORY_REDC.equals(category);
    }
}
