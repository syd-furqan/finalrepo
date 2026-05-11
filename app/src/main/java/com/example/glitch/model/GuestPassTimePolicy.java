package com.example.glitch.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.Timestamp;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Shared daytime-window policy for guest-pass issuance and entry validation.
 * Issuance window: 08:30–22:00 (exclusive). Entry/expiry window: 08:30–22:30 (exclusive). Campus timezone.
 */
public final class GuestPassTimePolicy {
    public static final String CAMPUS_TIME_ZONE_ID = "Asia/Karachi";
    private static volatile boolean testingBypassEnabled;

    private static final int OPEN_HOUR = 8;
    private static final int OPEN_MINUTE = 30;
    private static final int ISSUE_CLOSE_HOUR = 22;
    private static final int ISSUE_CLOSE_MINUTE = 0;
    private static final int CLOSE_HOUR = 22;
    private static final int CLOSE_MINUTE = 30;

    private GuestPassTimePolicy() {
    }

    /**
     * Test-only runtime override for bypassing issuance/entry time windows.
     */
    public static void setTestingBypassEnabled(boolean enabled) {
        testingBypassEnabled = enabled;
    }

    public static boolean canIssueNow() {
        return testingBypassEnabled || isIssueWindowOpenAt(System.currentTimeMillis());
    }

    public static boolean isIssueWindowOpenAt(long epochMillis) {
        Calendar calendar = campusCalendar(epochMillis);
        int minuteOfDay = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE);
        int openMinuteOfDay = OPEN_HOUR * 60 + OPEN_MINUTE;
        int issueCloseMinuteOfDay = ISSUE_CLOSE_HOUR * 60 + ISSUE_CLOSE_MINUTE;
        return minuteOfDay >= openMinuteOfDay && minuteOfDay < issueCloseMinuteOfDay;
    }

    public static boolean isEntryWindowOpenNow() {
        return testingBypassEnabled || isEntryWindowOpenAt(System.currentTimeMillis());
    }

    @NonNull
    public static Timestamp expiryAtToday2230() {
        return expiryAtToday2230For(System.currentTimeMillis());
    }

    public static boolean isActivePassOutOfPolicy(@NonNull GuestPass pass) {
        return isActivePassOutOfPolicy(pass, System.currentTimeMillis());
    }

    static boolean isEntryWindowOpenAt(long epochMillis) {
        return isWindowOpen(epochMillis);
    }

    @NonNull
    static Timestamp expiryAtToday2230For(long epochMillis) {
        Calendar calendar = campusCalendar(epochMillis);
        calendar.set(Calendar.HOUR_OF_DAY, CLOSE_HOUR);
        calendar.set(Calendar.MINUTE, CLOSE_MINUTE);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return new Timestamp(calendar.getTime());
    }

    static boolean isActivePassOutOfPolicy(@NonNull GuestPass pass, long nowMillis) {
        if (!"active".equalsIgnoreCase(pass.getStatus())) {
            return false;
        }
        Timestamp expiresAt = pass.getExpiresAt();
        if (testingBypassEnabled) {
            return expiresAt != null && expiresAt.toDate().getTime() <= nowMillis;
        }
        if (!isEntryWindowOpenAt(nowMillis)) {
            return true;
        }
        if (expiresAt == null) {
            return true;
        }
        long expiryMillis = expiresAt.toDate().getTime();
        if (expiryMillis <= nowMillis) {
            return true;
        }

        Calendar expiryCalendar = campusCalendar(expiryMillis);
        if (!isCloseTime(expiryCalendar)) {
            return true;
        }

        Calendar nowCalendar = campusCalendar(nowMillis);
        if (expiryCalendar.get(Calendar.YEAR) != nowCalendar.get(Calendar.YEAR)) {
            return true;
        }
        if (expiryCalendar.get(Calendar.DAY_OF_YEAR) != nowCalendar.get(Calendar.DAY_OF_YEAR)) {
            return true;
        }
        return false;
    }

    private static boolean isWindowOpen(long epochMillis) {
        Calendar calendar = campusCalendar(epochMillis);
        int minuteOfDay = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE);
        int openMinuteOfDay = OPEN_HOUR * 60 + OPEN_MINUTE;
        int closeMinuteOfDay = CLOSE_HOUR * 60 + CLOSE_MINUTE;
        return minuteOfDay >= openMinuteOfDay && minuteOfDay < closeMinuteOfDay;
    }

    private static boolean isCloseTime(@NonNull Calendar calendar) {
        return calendar.get(Calendar.HOUR_OF_DAY) == CLOSE_HOUR
                && calendar.get(Calendar.MINUTE) == CLOSE_MINUTE
                && calendar.get(Calendar.SECOND) == 0
                && calendar.get(Calendar.MILLISECOND) == 0;
    }

    @NonNull
    private static Calendar campusCalendar(long epochMillis) {
        TimeZone timeZone = TimeZone.getTimeZone(CAMPUS_TIME_ZONE_ID);
        Calendar calendar = Calendar.getInstance(timeZone, Locale.getDefault());
        calendar.setTimeInMillis(epochMillis);
        return calendar;
    }
}
