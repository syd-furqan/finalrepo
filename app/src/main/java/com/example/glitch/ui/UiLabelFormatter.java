package com.example.glitch.ui;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;

/**
 * Small UI-only formatter for converting storage/debug tokens into readable labels.
 */
public final class UiLabelFormatter {
    private UiLabelFormatter() {
    }

    @NonNull
    public static String normalizeToken(@Nullable String raw) {
        if (raw == null) {
            return "";
        }
        String normalized = raw.trim()
                .toLowerCase(Locale.getDefault())
                .replace('-', '_')
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("_+", "_");
        if (normalized.startsWith("_")) {
            normalized = normalized.substring(1);
        }
        if (normalized.endsWith("_")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    @NonNull
    public static String humanizeToken(@Nullable String raw) {
        String normalized = normalizeToken(raw);
        if (normalized.isEmpty()) {
            return "";
        }
        if ("na".equals(normalized) || "n_a".equals(normalized)) {
            return "N/A";
        }
        return toTitleCase(normalized.replace('_', ' '));
    }

    @NonNull
    public static String friendlyAuditEventType(@Nullable String raw) {
        String token = normalizeToken(raw);
        switch (token) {
            case "entry_allowed":
                return "Entry Allowed";
            case "entry_denied":
                return "Entry Denied";
            case "exit_logged":
                return "Exit Logged";
            case "request_overdue":
                return "Overdue";
            case "entry_reported_manual":
                return "Reported";
            case "entry_reported_overdue":
                return "Auto Reported";
            case "pass_used":
                return "Pass Used";
            case "pass_denied":
                return "Pass Denied";
            case "pass_reported":
                return "Pass Reported";
            case "pass_exited":
                return "Pass Exited";
            case "entry_invalidated_ban":
                return "Banned";
            default:
                return humanizeToken(token);
        }
    }

    @NonNull
    public static String toTitleCase(@NonNull String value) {
        if (value.trim().isEmpty()) {
            return "";
        }
        String[] words = value.trim().split("\\s+");
        StringBuilder out = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            if (out.length() > 0) {
                out.append(' ');
            }
            out.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) {
                out.append(word.substring(1).toLowerCase(Locale.getDefault()));
            }
        }
        return out.toString();
    }
}
