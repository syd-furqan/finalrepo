package com.example.glitch.ui;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Unit tests for role-aware destination mapping in the shared nav router.
 */
public class RoleNavRouterTest {

    @Test
    public void adminBottomNavMapsToAdminScreens() {
        assertEquals(RoleDestination.AUDIT, RoleNavRouter.resolveDestinationForRole(RoleDestination.DASHBOARD, "admin"));
        assertEquals(RoleDestination.RULES, RoleNavRouter.resolveDestinationForRole(RoleDestination.PASSES, "admin"));
        assertEquals(RoleDestination.ADMIN_VEHICLES, RoleNavRouter.resolveDestinationForRole(RoleDestination.VEHICLES, "admin"));
        assertEquals(RoleDestination.DIRECTORY, RoleNavRouter.resolveDestinationForRole(RoleDestination.DIRECTORY, "admin"));
    }

    @Test
    public void guardBottomNavIncludesDenyFlow() {
        assertEquals(RoleDestination.DASHBOARD, RoleNavRouter.resolveDestinationForRole(RoleDestination.DASHBOARD, "guard"));
        assertEquals(RoleDestination.SEARCH, RoleNavRouter.resolveDestinationForRole(RoleDestination.PASSES, "guard"));
        assertEquals(RoleDestination.SCAN, RoleNavRouter.resolveDestinationForRole(RoleDestination.VEHICLES, "guard"));
        assertEquals(RoleDestination.DIRECTORY, RoleNavRouter.resolveDestinationForRole(RoleDestination.DIRECTORY, "guard"));
    }

    @Test
    public void roleSpecificFallbacksRemainStable() {
        assertEquals(RoleDestination.FACULTY_REQUEST, RoleNavRouter.resolveDestinationForRole(RoleDestination.DASHBOARD, "faculty"));
        assertEquals(RoleDestination.DASHBOARD, RoleNavRouter.resolveDestinationForRole(RoleDestination.VEHICLES, "staff"));
        assertEquals(RoleDestination.STUDENT_PASSES, RoleNavRouter.resolveDestinationForRole(RoleDestination.PASSES, "student"));
        assertEquals(RoleDestination.DIRECTORY, RoleNavRouter.resolveDestinationForRole(RoleDestination.DASHBOARD, "unknown"));
    }

    @Test
    public void logoutDestinationAlwaysResolvesToLogout() {
        assertEquals(RoleDestination.LOGOUT, RoleNavRouter.resolveDestinationForRole(RoleDestination.LOGOUT, "admin"));
        assertEquals(RoleDestination.LOGOUT, RoleNavRouter.resolveDestinationForRole(RoleDestination.LOGOUT, "guard"));
        assertEquals(RoleDestination.LOGOUT, RoleNavRouter.resolveDestinationForRole(RoleDestination.LOGOUT, "faculty"));
    }
}
