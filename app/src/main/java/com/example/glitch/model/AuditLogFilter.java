package com.example.glitch.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Query filter model used by admin audit-list pagination and export.
 */
public class AuditLogFilter {
    private static final long DAY_MILLIS = 24L * 60L * 60L * 1000L;

    private final long fromInclusiveMillis;
    private final long toInclusiveMillis;
    private final List<String> eventTypes;
    private final List<String> actorRoles;
    private final String searchText;

    public AuditLogFilter(
            long fromInclusiveMillis,
            long toInclusiveMillis,
            @Nullable List<String> eventTypes,
            @Nullable List<String> actorRoles,
            @Nullable String searchText
    ) {
        this.fromInclusiveMillis = fromInclusiveMillis;
        this.toInclusiveMillis = toInclusiveMillis;
        this.eventTypes = normalizeList(eventTypes, false);
        this.actorRoles = normalizeList(actorRoles, true);
        this.searchText = normalize(searchText);
    }

    @NonNull
    public static AuditLogFilter lastDays(int days) {
        long now = System.currentTimeMillis();
        int safeDays = Math.max(1, days);
        long from = now - (safeDays * DAY_MILLIS);
        return new AuditLogFilter(from, now, Collections.emptyList(), Collections.emptyList(), "");
    }

    @NonNull
    public static AuditLogFilter last7Days() {
        return lastDays(7);
    }

    public long getFromInclusiveMillis() {
        return fromInclusiveMillis;
    }

    public long getToInclusiveMillis() {
        return toInclusiveMillis;
    }

    @NonNull
    public List<String> getEventTypes() {
        return eventTypes;
    }

    @NonNull
    public List<String> getActorRoles() {
        return actorRoles;
    }

    @NonNull
    public String getSearchText() {
        return searchText;
    }

    @NonNull
    public String getSearchToken() {
        String trimmed = searchText.trim().toLowerCase(Locale.getDefault());
        if (trimmed.isEmpty()) {
            return "";
        }
        int firstSpace = trimmed.indexOf(' ');
        if (firstSpace <= 0) {
            return trimmed;
        }
        return trimmed.substring(0, firstSpace);
    }

    public boolean isDateInRange(long createdAtMillis) {
        return createdAtMillis >= fromInclusiveMillis && createdAtMillis <= toInclusiveMillis;
    }

    public boolean matchesEventType(@Nullable String rawType) {
        if (eventTypes.isEmpty()) {
            return true;
        }
        String normalized = normalize(rawType);
        for (String type : eventTypes) {
            if (type.equalsIgnoreCase(normalized)) {
                return true;
            }
        }
        return false;
    }

    public boolean matchesActorRole(@Nullable String rawRole) {
        if (actorRoles.isEmpty()) {
            return true;
        }
        String normalized = normalize(rawRole);
        for (String role : actorRoles) {
            if (role.equalsIgnoreCase(normalized)) {
                return true;
            }
        }
        return false;
    }

    public boolean matchesText(@NonNull AccessEvent event) {
        if (searchText.isEmpty()) {
            return true;
        }
        String needle = searchText.toLowerCase(Locale.getDefault());
        return contains(event.getRequestId(), needle)
                || contains(event.getEntityId(), needle)
                || contains(event.getDescription(), needle)
                || contains(event.getActorUid(), needle);
    }

    private boolean contains(@Nullable String value, @NonNull String needle) {
        if (value == null) {
            return false;
        }
        return value.toLowerCase(Locale.getDefault()).contains(needle);
    }

    @NonNull
    private static List<String> normalizeList(@Nullable List<String> raw, boolean lowerCase) {
        if (raw == null || raw.isEmpty()) {
            return Collections.emptyList();
        }
        Set<String> deduped = new LinkedHashSet<>();
        for (String item : raw) {
            String normalized = normalize(item);
            if (normalized.isEmpty()) {
                continue;
            }
            deduped.add(lowerCase ? normalized.toLowerCase(Locale.getDefault()) : normalized);
        }
        return Collections.unmodifiableList(new ArrayList<>(deduped));
    }

    @NonNull
    private static String normalize(@Nullable String value) {
        return value == null ? "" : value.trim();
    }
}
