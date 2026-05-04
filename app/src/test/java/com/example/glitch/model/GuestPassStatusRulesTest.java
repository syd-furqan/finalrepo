package com.example.glitch.model;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.firebase.Timestamp;

import org.junit.Test;

import java.util.Date;

/**
 * Unit tests for shared guest-pass status lifecycle rules.
 */
public class GuestPassStatusRulesTest {

    @Test
    public void isArchivedStatus_matchesExpiredCancelledDeniedAndExited() {
        assertTrue(GuestPassStatusRules.isArchivedStatus("expired"));
        assertTrue(GuestPassStatusRules.isArchivedStatus("cancelled"));
        assertTrue(GuestPassStatusRules.isArchivedStatus("denied"));
        assertTrue(GuestPassStatusRules.isArchivedStatus("exited"));
        assertFalse(GuestPassStatusRules.isArchivedStatus("reported"));
        assertFalse(GuestPassStatusRules.isArchivedStatus("active"));
        assertFalse(GuestPassStatusRules.isArchivedStatus("used"));
    }

    @Test
    public void isTimeExpiredActive_requiresActiveAndPastExpiry() {
        GuestPass expiredActive = passWith("active", new Timestamp(new Date(System.currentTimeMillis() - 1000L)));
        GuestPass futureActive = passWith("active", new Timestamp(new Date(System.currentTimeMillis() + 3600_000L)));
        GuestPass expiredUsed = passWith("used", new Timestamp(new Date(System.currentTimeMillis() - 1000L)));

        assertTrue(GuestPassStatusRules.isTimeExpiredActive(expiredActive));
        assertFalse(GuestPassStatusRules.isTimeExpiredActive(futureActive));
        assertFalse(GuestPassStatusRules.isTimeExpiredActive(expiredUsed));
    }

    @Test
    public void isShareable_blocksArchivedPasses() {
        assertFalse(GuestPassStatusRules.isShareable(passWith("expired", null)));
        assertFalse(GuestPassStatusRules.isShareable(passWith("cancelled", null)));
        assertFalse(GuestPassStatusRules.isShareable(passWith("denied", null)));
        assertFalse(GuestPassStatusRules.isShareable(passWith("exited", null)));
        assertTrue(GuestPassStatusRules.isShareable(passWith("active", null)));
        assertFalse(GuestPassStatusRules.isShareable(passWith("used", null)));
    }

    @Test
    public void blocksStudentIssuance_forInProgressStatuses() {
        assertTrue(GuestPassStatusRules.blocksStudentIssuance("active"));
        assertTrue(GuestPassStatusRules.blocksStudentIssuance("used"));
        assertTrue(GuestPassStatusRules.blocksStudentIssuance("overdue"));
        assertTrue(GuestPassStatusRules.blocksStudentIssuance("reported"));
        assertFalse(GuestPassStatusRules.blocksStudentIssuance("expired"));
        assertFalse(GuestPassStatusRules.blocksStudentIssuance("cancelled"));
        assertFalse(GuestPassStatusRules.blocksStudentIssuance("denied"));
        assertFalse(GuestPassStatusRules.blocksStudentIssuance("exited"));
    }

    private GuestPass passWith(String status, Timestamp expiresAt) {
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
                expiresAt,
                null,
                "",
                "",
                null
        );
    }
}
