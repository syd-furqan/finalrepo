package com.example.glitch.ui;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Arrays;

/**
 * Unit tests for simplified role-aware navigation.
 */
public class RoleNavRouterTest {

    @Test
    public void guardPrimaryPagesAreDashboardAndQrScan() {
        assertEquals(
                Arrays.asList(RoleDestination.DASHBOARD, RoleDestination.SCAN),
                RoleNavRouter.getPrimaryDestinationsForRole("guard")
        );
        assertEquals(RoleDestination.DASHBOARD, RoleNavRouter.getDefaultDestinationForRole("guard"));
        assertEquals(RoleDestination.SCAN, RoleNavRouter.resolveDestinationForRole(RoleDestination.PASSES, "guard"));
        assertEquals(RoleDestination.SCAN, RoleNavRouter.resolveDestinationForRole(RoleDestination.VEHICLES, "guard"));
    }

    @Test
    public void studentPrimaryPagesAreGuestPassesAndVehicles() {
        assertEquals(
                Arrays.asList(RoleDestination.STUDENT_PASSES, RoleDestination.SPONSOR_VEHICLES),
                RoleNavRouter.getPrimaryDestinationsForRole("student")
        );
        assertEquals(RoleDestination.STUDENT_PASSES, RoleNavRouter.getDefaultDestinationForRole("student"));
        assertEquals(RoleDestination.STUDENT_PASSES, RoleNavRouter.resolveDestinationForRole(RoleDestination.DASHBOARD, "student"));
        assertEquals(RoleDestination.SPONSOR_VEHICLES, RoleNavRouter.resolveDestinationForRole(RoleDestination.VEHICLES, "student"));
    }

    @Test
    public void facultyPrimaryPagesAreRequestsNotificationsAndVehicles() {
        assertEquals(
                Arrays.asList(
                        RoleDestination.FACULTY_REQUEST,
                        RoleDestination.FACULTY_NOTIFICATIONS,
                        RoleDestination.SPONSOR_VEHICLES
                ),
                RoleNavRouter.getPrimaryDestinationsForRole("faculty")
        );
        assertEquals(RoleDestination.FACULTY_REQUEST, RoleNavRouter.getDefaultDestinationForRole("faculty"));
        assertEquals(RoleDestination.FACULTY_REQUEST, RoleNavRouter.resolveDestinationForRole(RoleDestination.DASHBOARD, "faculty"));
        assertEquals(RoleDestination.FACULTY_NOTIFICATIONS, RoleNavRouter.resolveDestinationForRole(RoleDestination.PASSES, "faculty"));
    }

    @Test
    public void adminPrimaryPagesAreAuditAlertsViolationsAndMore() {
        assertEquals(
                Arrays.asList(
                        RoleDestination.AUDIT,
                        RoleDestination.ALERTS,
                        RoleDestination.VIOLATION_DIRECTORY,
                        RoleDestination.DIRECTORY
                ),
                RoleNavRouter.getPrimaryDestinationsForRole("admin")
        );
        assertEquals(RoleDestination.AUDIT, RoleNavRouter.getDefaultDestinationForRole("admin"));
        assertEquals(RoleDestination.AUDIT, RoleNavRouter.resolveDestinationForRole(RoleDestination.DASHBOARD, "admin"));
        assertEquals(RoleDestination.ALERTS, RoleNavRouter.resolveDestinationForRole(RoleDestination.PASSES, "admin"));
    }

    @Test
    public void monitorPrimaryPagesAreReportAndMyReports() {
        assertEquals(
                Arrays.asList(RoleDestination.MONITOR_REPORT, RoleDestination.MONITOR_MY_REPORTS),
                RoleNavRouter.getPrimaryDestinationsForRole("monitor")
        );
        assertEquals(RoleDestination.MONITOR_REPORT, RoleNavRouter.getDefaultDestinationForRole("monitor"));
        assertEquals(RoleDestination.MONITOR_REPORT, RoleNavRouter.resolveDestinationForRole(RoleDestination.DASHBOARD, "monitor"));
        assertEquals(RoleDestination.MONITOR_MY_REPORTS, RoleNavRouter.resolveDestinationForRole(RoleDestination.PASSES, "monitor"));
    }

    @Test
    public void logoutDestinationAlwaysResolvesToLogout() {
        assertEquals(RoleDestination.LOGOUT, RoleNavRouter.resolveDestinationForRole(RoleDestination.LOGOUT, "admin"));
        assertEquals(RoleDestination.LOGOUT, RoleNavRouter.resolveDestinationForRole(RoleDestination.LOGOUT, "guard"));
        assertEquals(RoleDestination.LOGOUT, RoleNavRouter.resolveDestinationForRole(RoleDestination.LOGOUT, "faculty"));
        assertEquals(RoleDestination.LOGOUT, RoleNavRouter.resolveDestinationForRole(RoleDestination.LOGOUT, "student"));
    }
}
