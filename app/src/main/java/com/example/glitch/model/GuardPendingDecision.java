package com.example.glitch.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.Timestamp;

import java.util.Date;
import java.util.Locale;

/**
 * Persisted guard decision context for one unresolved scanned pass.
 */
public class GuardPendingDecision {
    private static final long TS_UNSET = -1L;
    private static final char SEP = '|';
    private static final char ESC = '\\';

    private final String guardUid;
    private final String passCode;
    private final String verificationMethod;
    private final String entryRequestId;
    private final String guestName;
    private final String guestIdNumber;
    private final boolean hasVehicle;
    private final String vehiclePlate;
    private final String guestType;
    private final String sponsorName;
    private final String sponsorRole;
    private final String sponsorEmail;
    private final String gateLabel;
    private final long createdAtMillis;
    private final long expiresAtMillis;

    public GuardPendingDecision(
            @NonNull String guardUid,
            @NonNull String passCode,
            @NonNull String verificationMethod,
            @NonNull String entryRequestId,
            @NonNull String guestName,
            @NonNull String guestIdNumber,
            boolean hasVehicle,
            @NonNull String vehiclePlate,
            @NonNull String guestType,
            @NonNull String sponsorName,
            @NonNull String sponsorRole,
            @NonNull String sponsorEmail,
            @NonNull String gateLabel,
            long createdAtMillis,
            long expiresAtMillis
    ) {
        this.guardUid = guardUid;
        this.passCode = passCode;
        this.verificationMethod = verificationMethod;
        this.entryRequestId = entryRequestId;
        this.guestName = guestName;
        this.guestIdNumber = guestIdNumber;
        this.hasVehicle = hasVehicle;
        this.vehiclePlate = vehiclePlate;
        this.guestType = guestType;
        this.sponsorName = sponsorName;
        this.sponsorRole = sponsorRole;
        this.sponsorEmail = sponsorEmail;
        this.gateLabel = gateLabel;
        this.createdAtMillis = createdAtMillis;
        this.expiresAtMillis = expiresAtMillis;
    }

    @NonNull
    public static GuardPendingDecision fromPass(
            @NonNull String guardUid,
            @NonNull GuestPass pass,
            @NonNull String verificationMethod
    ) {
        return new GuardPendingDecision(
                guardUid.trim(),
                pass.getPassCode().trim().toUpperCase(),
                verificationMethod.trim(),
                pass.getEntryRequestId().trim(),
                pass.getGuestName().trim(),
                pass.getGuestIdNumber().trim(),
                pass.hasVehicle(),
                pass.getVehiclePlate().trim(),
                pass.getGuestType().trim(),
                pass.getSponsorName().trim(),
                pass.getSponsorRole().trim(),
                pass.getSponsorEmail().trim(),
                pass.getGateLabel().trim(),
                toMillis(pass.getCreatedAt()),
                toMillis(pass.getExpiresAt())
        );
    }

    @Nullable
    public static GuardPendingDecision fromJson(@Nullable String json) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        String[] fields = splitEscaped(json);
        if (fields.length != 15) {
            return null;
        }
        return new GuardPendingDecision(
                unescape(fields[0]),
                unescape(fields[1]),
                unescape(fields[2]),
                unescape(fields[3]),
                unescape(fields[4]),
                unescape(fields[5]),
                parseBoolean(fields[6]),
                unescape(fields[7]),
                unescape(fields[8]),
                unescape(fields[9]),
                unescape(fields[10]),
                unescape(fields[11]),
                unescape(fields[12]),
                parseLong(fields[13]),
                parseLong(fields[14])
        );
    }

    @Nullable
    public String toJson() {
        return String.join(
                String.valueOf(SEP),
                escape(guardUid),
                escape(passCode),
                escape(verificationMethod),
                escape(entryRequestId),
                escape(guestName),
                escape(guestIdNumber),
                String.format(Locale.getDefault(), "%b", hasVehicle),
                escape(vehiclePlate),
                escape(guestType),
                escape(sponsorName),
                escape(sponsorRole),
                escape(sponsorEmail),
                escape(gateLabel),
                String.format(Locale.getDefault(), "%d", createdAtMillis),
                String.format(Locale.getDefault(), "%d", expiresAtMillis)
        );
    }

    public boolean isValid() {
        return !guardUid.trim().isEmpty()
                && !passCode.trim().isEmpty()
                && !entryRequestId.trim().isEmpty();
    }

    @NonNull
    public String getGuardUid() {
        return guardUid;
    }

    @NonNull
    public String getPassCode() {
        return passCode;
    }

    @NonNull
    public String getVerificationMethod() {
        return verificationMethod;
    }

    @NonNull
    public String getEntryRequestId() {
        return entryRequestId;
    }

    @NonNull
    public String getGuestName() {
        return guestName;
    }

    @NonNull
    public String getGuestIdNumber() {
        return guestIdNumber;
    }

    public boolean hasVehicle() {
        return hasVehicle;
    }

    @NonNull
    public String getVehiclePlate() {
        return vehiclePlate;
    }

    @NonNull
    public String getGuestType() {
        return guestType;
    }

    @NonNull
    public String getSponsorName() {
        return sponsorName;
    }

    @NonNull
    public String getSponsorRole() {
        return sponsorRole;
    }

    @NonNull
    public String getSponsorEmail() {
        return sponsorEmail;
    }

    @NonNull
    public String getGateLabel() {
        return gateLabel;
    }

    public long getCreatedAtMillis() {
        return createdAtMillis;
    }

    public long getExpiresAtMillis() {
        return expiresAtMillis;
    }

    private static long toMillis(@Nullable Timestamp timestamp) {
        if (timestamp == null) {
            return TS_UNSET;
        }
        Date date = timestamp.toDate();
        return date.getTime();
    }

    private static long parseLong(@Nullable String value) {
        if (value == null || value.trim().isEmpty()) {
            return TS_UNSET;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ignored) {
            return TS_UNSET;
        }
    }

    private static boolean parseBoolean(@Nullable String value) {
        if (value == null) {
            return false;
        }
        return Boolean.parseBoolean(value.trim());
    }

    @NonNull
    private static String escape(@NonNull String value) {
        StringBuilder out = new StringBuilder(value.length() + 8);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == ESC || c == SEP) {
                out.append(ESC);
            }
            out.append(c);
        }
        return out.toString();
    }

    @NonNull
    private static String unescape(@NonNull String value) {
        StringBuilder out = new StringBuilder(value.length());
        boolean escaping = false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (escaping) {
                out.append(c);
                escaping = false;
            } else if (c == ESC) {
                escaping = true;
            } else {
                out.append(c);
            }
        }
        if (escaping) {
            out.append(ESC);
        }
        return out.toString();
    }

    @NonNull
    private static String[] splitEscaped(@NonNull String value) {
        java.util.List<String> parts = new java.util.ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean escaping = false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (escaping) {
                current.append(c);
                escaping = false;
                continue;
            }
            if (c == ESC) {
                escaping = true;
                continue;
            }
            if (c == SEP) {
                parts.add(current.toString());
                current.setLength(0);
                continue;
            }
            current.append(c);
        }
        if (escaping) {
            current.append(ESC);
        }
        parts.add(current.toString());
        return parts.toArray(new String[0]);
    }
}
