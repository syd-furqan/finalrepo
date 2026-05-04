package com.example.glitch.ui;

/**
 * Canonical destination keys for role-aware in-app navigation.
 * Pattern: Enum-based route contract used by RoleNavRouter and UI shells.
 * Known issue: some destinations can resolve to the same screen for selected roles.
 */
public enum RoleDestination {
    DASHBOARD,
    PASSES,
    VEHICLES,
    DIRECTORY,
    SEARCH,
    SCAN,
    AUDIT,
    USERS,
    RULES,
    ALERTS,
    FACULTY_REQUEST,
    FACULTY_NOTIFICATIONS,
    STUDENT_PASSES,
    SPONSOR_VEHICLES,
    GUARD_DENY,
    ADMIN_VEHICLES,
    LOGOUT
}
