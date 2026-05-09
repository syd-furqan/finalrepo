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
    public void adminPrimaryPagesAreDashboardSecurityAccessAndAudit() {
        assertEquals(
                Arrays.asList(
                        RoleDestination.DIRECTORY,
                        RoleDestination.ADMIN_SECURITY,
                        RoleDestination.ADMIN_ACCESS,
                        RoleDestination.AUDIT
                ),
                RoleNavRouter.getPrimaryDestinationsForRole("admin")
        );
        assertEquals(RoleDestination.DIRECTORY, RoleNavRouter.getDefaultDestinationForRole("admin"));
        assertEquals(RoleDestination.DIRECTORY, RoleNavRouter.resolveDestinationForRole(RoleDestination.DASHBOARD, "admin"));
        assertEquals(RoleDestination.ADMIN_SECURITY, RoleNavRouter.resolveDestinationForRole(RoleDestination.PASSES, "admin"));
        assertEquals(RoleDestination.ADMIN_ACCESS, RoleNavRouter.resolveDestinationForRole(RoleDestination.VEHICLES, "admin"));
    }

    @Test
    public void adminCategoriesContainInternalPages() {
        assertEquals(
                Arrays.asList(
                        RoleDestination.ALERTS,
                        RoleDestination.VIOLATION_DIRECTORY,
                        RoleDestination.BANNED_LIST,
                        RoleDestination.ADMIN_CHARGES
                ),
                RoleNavRouter.getAdminCategoryDestinations(RoleDestination.ADMIN_SECURITY)
        );
        assertEquals(
                Arrays.asList(
                        RoleDestination.DASHBOARD,
                        RoleDestination.SCAN,
                        RoleDestination.ADMIN_VEHICLES
                ),
                RoleNavRouter.getAdminCategoryDestinations(RoleDestination.ADMIN_ACCESS)
        );
        assertEquals(
                Arrays.asList(RoleDestination.AUDIT, RoleDestination.ADMIN_ANALYTICS),
                RoleNavRouter.getAdminCategoryDestinations(RoleDestination.AUDIT)
        );
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
