package com.example.glitch.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Shared identity-format policy for guest issuance and verification.
 */
public final class GuestIdentityPolicy {
    private static final Pattern CNIC_PATTERN = Pattern.compile("^\\d{5}-\\d{7}-\\d$");
    private static final Pattern VEHICLE_PLATE_PATTERN = Pattern.compile("^[A-Z]{3}-\\d{3,4}$");

    private GuestIdentityPolicy() {
    }

    @NonNull
    public static String normalizeCnicOrEmpty(@Nullable String rawValue) {
        String result = normalizeCnic(rawValue);
        return result == null ? "" : result;
    }

    @Nullable
    public static String normalizeCnic(@Nullable String rawValue) {
        String digits = extractDigits(rawValue);
        if (digits.length() != 13) {
            return null;
        }
        return digits.substring(0, 5)
                + "-"
                + digits.substring(5, 12)
                + "-"
                + digits.substring(12);
    }

    @Nullable
    public static String normalizeVehiclePlate(@Nullable String rawValue) {
        String compact = compactAlphaNumeric(rawValue).toUpperCase(Locale.getDefault());
        if (compact.length() < 6 || compact.length() > 7) {
            return null;
        }
        String letters = compact.substring(0, 3);
        String digits = compact.substring(3);
        if (!letters.matches("[A-Z]{3}") || !digits.matches("\\d{3,4}")) {
            return null;
        }
        return letters + "-" + digits;
    }

    public static boolean isValidCnic(@Nullable String value) {
        if (value == null) {
            return false;
        }
        return CNIC_PATTERN.matcher(value.trim()).matches();
    }

    public static boolean isValidVehiclePlate(@Nullable String value) {
        if (value == null) {
            return false;
        }
        return VEHICLE_PLATE_PATTERN.matcher(value.trim().toUpperCase(Locale.getDefault())).matches();
    }

    @NonNull
    public static String formatCnicForInput(@Nullable String rawValue) {
        String digits = extractDigits(rawValue);
        if (digits.length() > 13) {
            digits = digits.substring(0, 13);
        }
        StringBuilder out = new StringBuilder(15);
        for (int i = 0; i < digits.length(); i++) {
            if (i == 5 || i == 12) {
                out.append('-');
            }
            out.append(digits.charAt(i));
        }
        return out.toString();
    }

    @NonNull
    public static String formatVehiclePlateForInput(@Nullable String rawValue) {
        String compact = compactAlphaNumeric(rawValue).toUpperCase(Locale.getDefault());
        StringBuilder letters = new StringBuilder(3);
        StringBuilder digits = new StringBuilder(4);

        for (int i = 0; i < compact.length(); i++) {
            char c = compact.charAt(i);
            if (letters.length() < 3) {
                if (Character.isLetter(c)) {
                    letters.append(c);
                }
                continue;
            }
            if (Character.isDigit(c) && digits.length() < 4) {
                digits.append(c);
            }
        }

        StringBuilder out = new StringBuilder(8);
        out.append(letters);
        if (letters.length() == 3) {
            out.append('-');
            out.append(digits);
        }
        return out.toString();
    }

    @NonNull
    public static String guestTypeFor(boolean hasVehicle) {
        return hasVehicle ? "vehicle" : "non_vehicle";
    }

    @NonNull
    private static String extractDigits(@Nullable String value) {
        if (value == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (Character.isDigit(c)) {
                out.append(c);
            }
        }
        return out.toString();
    }

    @NonNull
    private static String compactAlphaNumeric(@Nullable String value) {
        if (value == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (Character.isLetterOrDigit(c)) {
                out.append(c);
            }
        }
        return out.toString();
    }
}
