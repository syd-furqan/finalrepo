package com.example.glitch.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;

/**
 * Single-gate policy shared across model, repository and UI layers.
 */
public final class GatePolicy {
    public static final String STORED_VALUE = "in-gate";
    public static final String DISPLAY_LABEL = "In-Gate";

    private GatePolicy() {
    }

    /**
     * Normalizes any raw gate input to canonical storage value.
     */
    @NonNull
    public static String normalizeStoredValue(@Nullable String rawValue) {
        return STORED_VALUE;
    }

    /**
     * Returns true only when a stored value is already canonical.
     */
    public static boolean isCanonicalStoredValue(@Nullable String rawValue) {
        if (rawValue == null) {
            return false;
        }
        return STORED_VALUE.equals(rawValue.trim().toLowerCase(Locale.getDefault()));
    }

    /**
     * Converts any stored/legacy gate value into user-facing display label.
     */
    @NonNull
    public static String toDisplayLabel(@Nullable String rawValue) {
        return DISPLAY_LABEL;
    }
}
