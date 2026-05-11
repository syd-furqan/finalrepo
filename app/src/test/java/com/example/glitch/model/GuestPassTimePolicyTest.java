package com.example.glitch.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.firebase.Timestamp;

import org.junit.Test;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Unit tests for daytime guest-pass policy.
 */
public class GuestPassTimePolicyTest {

    @Test
    public void issueWindow_enforcesInclusiveOpenAndExclusiveCloseBoundaries() {
        assertFalse(GuestPassTimePolicy.isIssueWindowOpenAt(millisAt(8, 29, 59)));
        assertTrue(GuestPassTimePolicy.isIssueWindowOpenAt(millisAt(8, 30, 0)));
        assertTrue(GuestPassTimePolicy.isIssueWindowOpenAt(millisAt(21, 59, 59)));
        assertFalse(GuestPassTimePolicy.isIssueWindowOpenAt(millisAt(22, 0, 0)));
    }

    @Test
    public void entryWindow_matchesIssueWindowBoundaries() {
        assertFalse(GuestPassTimePolicy.isEntryWindowOpenAt(millisAt(8, 29, 59)));
        assertTrue(GuestPassTimePolicy.isEntryWindowOpenAt(millisAt(8, 30, 0)));
        assertTrue(GuestPassTimePolicy.isEntryWindowOpenAt(millisAt(22, 29, 59)));
        assertFalse(GuestPassTimePolicy.isEntryWindowOpenAt(millisAt(22, 30, 0)));
    }

    @Test
    public void expiryAtToday2230For_alwaysReturnsSameDayCloseTimestamp() {
        long now = millisAt(11, 5, 12);
        Timestamp expiresAt = GuestPassTimePolicy.expiryAtToday2230For(now);

        Calendar nowCal = campusCalendar(now);
        Calendar expiryCal = campusCalendar(expiresAt.toDate().getTime());
        assertEquals(nowCal.get(Calendar.YEAR), expiryCal.get(Calendar.YEAR));
        assertEquals(nowCal.get(Calendar.DAY_OF_YEAR), expiryCal.get(Calendar.DAY_OF_YEAR));
        assertEquals(22, expiryCal.get(Calendar.HOUR_OF_DAY));
        assertEquals(30, expiryCal.get(Calendar.MINUTE));
        assertEquals(0, expiryCal.get(Calendar.SECOND));
        assertEquals(0, expiryCal.get(Calendar.MILLISECOND));
    }

    @Test
    public void activePassOutOfPolicy_trueOutsideRulesAndFalseForValidActivePass() {
        long validNow = millisAt(10, 0, 0);
        GuestPass validPass = passWith("active", millisAt(22, 30, 0), millisAt(9, 0, 0));
        assertFalse(GuestPassTimePolicy.isActivePassOutOfPolicy(validPass, validNow));

        long beforeOpen = millisAt(8, 0, 0);
        assertTrue(GuestPassTimePolicy.isActivePassOutOfPolicy(validPass, beforeOpen));

        GuestPass wrongCloseTimePass = passWith("active", millisAt(21, 15, 0), millisAt(9, 0, 0));
        assertTrue(GuestPassTimePolicy.isActivePassOutOfPolicy(wrongCloseTimePass, validNow));

        GuestPass nextDayPass = passWith("active", millisAtNextDay(22, 30, 0), millisAt(9, 0, 0));
        assertTrue(GuestPassTimePolicy.isActivePassOutOfPolicy(nextDayPass, validNow));

        GuestPass legacyCarryOverPass = passWith("active", millisAt(22, 30, 0), millisAtPreviousDay(23, 0, 0));
        assertFalse(GuestPassTimePolicy.isActivePassOutOfPolicy(legacyCarryOverPass, validNow));

        GuestPass usedPass = passWith("used", millisAt(22, 30, 0), millisAtPreviousDay(23, 0, 0));
        assertFalse(GuestPassTimePolicy.isActivePassOutOfPolicy(usedPass, validNow));
    }

    private GuestPass passWith(String status, long expiryMillis, long createdAtMillis) {
        return new GuestPass(
                "pass-1",
                "student-1",
                "student",
                "Ali",
                "ali@example.com",
                "Hamza",
                "12345",
                false,
                "",
                "non_vehicle",
                "CODE1234",
                "req-1",
                GatePolicy.STORED_VALUE,
                status,
                new Timestamp(new Date(expiryMillis)),
                null,
                "",
                "",
                new Timestamp(new Date(createdAtMillis))
        );
    }

    private long millisAt(int hour, int minute, int second) {
        Calendar calendar = campusCalendar(System.currentTimeMillis());
        calendar.set(2026, Calendar.MAY, 3, hour, minute, second);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    private long millisAtNextDay(int hour, int minute, int second) {
        Calendar calendar = campusCalendar(millisAt(hour, minute, second));
        calendar.add(Calendar.DAY_OF_YEAR, 1);
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, second);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    private long millisAtPreviousDay(int hour, int minute, int second) {
        Calendar calendar = campusCalendar(millisAt(hour, minute, second));
        calendar.add(Calendar.DAY_OF_YEAR, -1);
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, second);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    private Calendar campusCalendar(long millis) {
        Calendar calendar = Calendar.getInstance(
                TimeZone.getTimeZone(GuestPassTimePolicy.CAMPUS_TIME_ZONE_ID),
                Locale.getDefault()
        );
        calendar.setTimeInMillis(millis);
        return calendar;
    }
}
