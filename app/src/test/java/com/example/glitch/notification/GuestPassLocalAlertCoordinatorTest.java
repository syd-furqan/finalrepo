package com.example.glitch.notification;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Unit tests for coordinator role eligibility checks.
 */
public class GuestPassLocalAlertCoordinatorTest {

    @Test
    public void supportedRoles_includeStudentAndFaculty() {
        assertTrue(GuestPassLocalAlertCoordinator.isSupportedSponsorRole("student"));
        assertTrue(GuestPassLocalAlertCoordinator.isSupportedSponsorRole("faculty"));
        assertTrue(GuestPassLocalAlertCoordinator.isSupportedSponsorRole("  STUDENT "));
        assertTrue(GuestPassLocalAlertCoordinator.isSupportedSponsorRole("FACULTY"));
    }

    @Test
    public void unsupportedRoles_areRejected() {
        assertFalse(GuestPassLocalAlertCoordinator.isSupportedSponsorRole("guard"));
        assertFalse(GuestPassLocalAlertCoordinator.isSupportedSponsorRole("admin"));
        assertFalse(GuestPassLocalAlertCoordinator.isSupportedSponsorRole("staff"));
        assertFalse(GuestPassLocalAlertCoordinator.isSupportedSponsorRole(""));
        assertFalse(GuestPassLocalAlertCoordinator.isSupportedSponsorRole(null));
    }
}
