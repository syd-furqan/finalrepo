package com.example.glitch.ui;

import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.glitch.R;
import com.example.glitch.auth.SessionManager;
import com.example.glitch.data.RepositoryProvider;
import com.example.glitch.model.UserProfile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Centralized role-aware router for bottom-nav and hub navigation actions.
 */
public final class RoleNavRouter {
    private RoleNavRouter() {
    }

    private static final class NavItem {
        private final RoleDestination destination;
        private final int labelResId;
        private final int iconResId;

        private NavItem(@NonNull RoleDestination destination, int labelResId, int iconResId) {
            this.destination = destination;
            this.labelResId = labelResId;
            this.iconResId = iconResId;
        }
    }

    public static void bindBottomNav(
            @NonNull View root,
            @NonNull Fragment owner,
            @NonNull RoleDestination active
    ) {
        LinearLayout[] slots = new LinearLayout[]{
                root.findViewById(R.id.nav_item_dashboard),
                root.findViewById(R.id.nav_item_passes),
                root.findViewById(R.id.nav_item_vehicles),
                root.findViewById(R.id.nav_item_directory),
                root.findViewById(R.id.nav_item_logout)
        };
        int[] iconIds = new int[]{
                R.id.nav_icon_dashboard,
                R.id.nav_icon_passes,
                R.id.nav_icon_vehicles,
                R.id.nav_icon_directory,
                R.id.nav_icon_logout
        };
        int[] labelIds = new int[]{
                R.id.nav_label_dashboard,
                R.id.nav_label_passes,
                R.id.nav_label_vehicles,
                R.id.nav_label_directory,
                R.id.nav_label_logout
        };

        for (LinearLayout slot : slots) {
            if (slot == null) {
                return;
            }
        }

        UserProfile profile = SessionManager.getCurrentProfile();
        String role = profile == null ? "" : profile.getRole();
        List<NavItem> items = navItemsForRole(role);
        RoleDestination activeDestination = resolveActiveDestinationForRole(active, role);

        for (int i = 0; i < slots.length; i++) {
            LinearLayout slot = slots[i];
            if (i >= items.size()) {
                slot.setVisibility(View.GONE);
                slot.setOnClickListener(null);
                continue;
            }
            NavItem item = items.get(i);
            slot.setVisibility(View.VISIBLE);
            slot.setOnClickListener(v -> route(owner, item.destination));

            ImageView icon = slot.findViewById(iconIds[i]);
            TextView label = slot.findViewById(labelIds[i]);
            if (icon != null) {
                icon.setImageResource(item.iconResId);
            }
            if (label != null) {
                label.setText(owner.getString(item.labelResId));
            }
            styleItem(owner, slot, iconIds[i], labelIds[i], item.destination == activeDestination);
        }
    }

    public static void route(@NonNull Fragment owner, @NonNull RoleDestination destination) {
        if (!owner.isAdded()) {
            return;
        }
        UserProfile profile = SessionManager.getCurrentProfile();
        String role = profile == null ? "" : profile.getRole();
        RoleDestination resolved = resolveDestinationForRole(destination, role);
        if (resolved == RoleDestination.LOGOUT) {
            RepositoryProvider.getAuthRepository().logout();
            SessionManager.clear();
            if (owner.requireActivity() instanceof NavigationHost) {
                ((NavigationHost) owner.requireActivity()).showLogin(true);
            }
            return;
        }
        if (profile == null) {
            if (owner.requireActivity() instanceof NavigationHost) {
                ((NavigationHost) owner.requireActivity()).showLogin(true);
            }
            return;
        }
        Fragment target = createFragmentForDestination(resolved, profile);
        if (target == null) {
            return;
        }
        if (owner.getClass().equals(target.getClass()) && !(owner instanceof AdminCategoryFragment)) {
            return;
        }
        if (owner.requireActivity() instanceof NavigationHost) {
            ((NavigationHost) owner.requireActivity()).showFragment(target, false);
        }
    }

    @NonNull
    public static RoleDestination resolveDestinationForRole(
            @NonNull RoleDestination destination,
            @Nullable String rawRole
    ) {
        String role = normalize(rawRole);
        switch (destination) {
            case DASHBOARD:
                if ("admin".equals(role)) return RoleDestination.DIRECTORY;
                if ("guard".equals(role)) return RoleDestination.DASHBOARD;
                if ("faculty".equals(role)) return RoleDestination.FACULTY_REQUEST;
                if ("monitor".equals(role)) return RoleDestination.MONITOR_REPORT;
                if ("student".equals(role)) return RoleDestination.STUDENT_PASSES;
                return RoleDestination.DIRECTORY;
            case PASSES:
                if ("admin".equals(role)) return RoleDestination.ADMIN_SECURITY;
                if ("guard".equals(role)) return RoleDestination.SCAN;
                if ("faculty".equals(role)) return RoleDestination.NOTIFICATIONS;
                if ("monitor".equals(role)) return RoleDestination.MONITOR_MY_REPORTS;
                if ("student".equals(role)) return RoleDestination.STUDENT_PASSES;
                return RoleDestination.DIRECTORY;
            case VEHICLES:
                if ("admin".equals(role)) return RoleDestination.ADMIN_ACCESS;
                if ("guard".equals(role)) return RoleDestination.SCAN;
                if ("faculty".equals(role)) return RoleDestination.SPONSOR_VEHICLES;
                if ("student".equals(role)) return RoleDestination.SPONSOR_VEHICLES;
                return RoleDestination.DIRECTORY;
            case DIRECTORY:
                return RoleDestination.DIRECTORY;
            case FACULTY_NOTIFICATIONS:
                return RoleDestination.NOTIFICATIONS;
            case ANNOUNCEMENTS:
                if ("guard".equals(role) || "monitor".equals(role)) return RoleDestination.ANNOUNCEMENTS;
                if ("faculty".equals(role) || "student".equals(role)) return RoleDestination.NOTIFICATIONS;
                return RoleDestination.DIRECTORY;
            case LOGOUT:
                return RoleDestination.LOGOUT;
            default:
                return destination;
        }
    }

    @NonNull
    public static List<RoleDestination> getPrimaryDestinationsForRole(@Nullable String rawRole) {
        List<RoleDestination> destinations = new ArrayList<>();
        for (NavItem item : navItemsForRole(rawRole)) {
            if (item.destination != RoleDestination.LOGOUT) {
                destinations.add(item.destination);
            }
        }
        return Collections.unmodifiableList(destinations);
    }

    @NonNull
    public static List<RoleDestination> getHubDestinationsForRole(@Nullable String rawRole) {
        String role = normalize(rawRole);
        List<RoleDestination> destinations = new ArrayList<>();
        if ("admin".equals(role)) {
            destinations.add(RoleDestination.USERS);
            destinations.addAll(getAdminCategoryDestinations(RoleDestination.ADMIN_SECURITY));
            destinations.addAll(getAdminCategoryDestinations(RoleDestination.ADMIN_ACCESS));
            destinations.add(RoleDestination.AUDIT);
            destinations.add(RoleDestination.ADMIN_ANALYTICS);
        } else {
            destinations.addAll(getPrimaryDestinationsForRole(role));
        }
        return Collections.unmodifiableList(destinations);
    }

    @NonNull
    public static List<RoleDestination> getAdminCategoryDestinations(@NonNull RoleDestination category) {
        List<RoleDestination> destinations = new ArrayList<>();
        switch (category) {
            case ADMIN_SECURITY:
                destinations.add(RoleDestination.ALERTS);
                destinations.add(RoleDestination.VIOLATION_DIRECTORY);
                destinations.add(RoleDestination.BANNED_LIST);
                destinations.add(RoleDestination.ADMIN_CHARGES);
                break;
            case ADMIN_ACCESS:
                destinations.add(RoleDestination.DASHBOARD);
                destinations.add(RoleDestination.SCAN);
                destinations.add(RoleDestination.ADMIN_VEHICLES);
                destinations.add(RoleDestination.ADMIN_TAKE_ACTION);
                break;
            case AUDIT:
                destinations.add(RoleDestination.AUDIT);
                destinations.add(RoleDestination.ADMIN_ANALYTICS);
                break;
            default:
                break;
        }
        return Collections.unmodifiableList(destinations);
    }

    @NonNull
    public static RoleDestination getDefaultDestinationForRole(@Nullable String rawRole) {
        List<RoleDestination> destinations = getPrimaryDestinationsForRole(rawRole);
        if (destinations.isEmpty()) {
            return RoleDestination.DIRECTORY;
        }
        return destinations.get(0);
    }

    @NonNull
    public static String getLabelForDestination(
            @NonNull RoleDestination destination,
            @Nullable String rawRole
    ) {
        switch (destination) {
            case DASHBOARD:
                return "admin".equals(normalize(rawRole)) ? "Gate Dashboard" : "Dashboard";
            case SEARCH:
                return "Search & Verify";
            case SCAN:
                return "QR Scan";
            case AUDIT:
                return "Audit Logs";
            case USERS:
                return "Users";
            case RULES:
                return "Verification Rules";
            case ALERTS:
                return "Alerts";
            case FACULTY_REQUEST:
                return "Access Requests";
            case FACULTY_NOTIFICATIONS:
            case NOTIFICATIONS:
                return "Notifications";
            case STUDENT_PASSES:
                return "Guest Passes";
            case SPONSOR_VEHICLES:
                return "Vehicles";
            case ADMIN_VEHICLES:
                return "Vehicle Review";
            case MONITOR_REPORT:
                return "Report Violation";
            case MONITOR_MY_REPORTS:
                return "My Reports";
            case VIOLATION_DIRECTORY:
                return "Violations";
            case BANNED_LIST:
                return "Banned Guests";
            case STUDENT_CHARGES:
                return "My Charges";
            case STUDENT_WARNINGS:
                return "My Warnings";
            case ADMIN_CHARGES:
                return "Charges";
            case ADMIN_ANALYTICS:
                return "Traffic Analytics";
            case ADMIN_SECURITY:
                return "Security";
            case ADMIN_ACCESS:
                return "Access";
            case ADMIN_TAKE_ACTION:
                return "Take Action";
            case ANNOUNCEMENTS:
                return "Announcements";
            case DIRECTORY:
                return "admin".equals(normalize(rawRole)) ? "Dashboard" : "Home";
            case LOGOUT:
                return "Logout";
            default:
                return "Home";
        }
    }

    @Nullable
    public static Fragment createFragmentForDestination(
            @NonNull RoleDestination destination,
            @Nullable UserProfile profile
    ) {
        switch (destination) {
            case DASHBOARD:
                return DashboardFragment.newInstance();
            case DIRECTORY:
                return RoleHomeFragment.newInstance(
                        safeProfileValue(profile, true),
                        safeProfileValue(profile, false),
                        profile == null ? "" : profile.getEmail(),
                        profile == null ? "" : normalize(profile.getRole())
                );
            case SEARCH:
                return GuardSearchFragment.newInstance();
            case SCAN:
                return GuardQrScanFragment.newInstance();
            case AUDIT:
                return AdminAuditLogFragment.newInstance();
            case USERS:
                return AdminUserManagementFragment.newInstance();
            case RULES:
                return AdminVerificationRulesFragment.newInstance();
            case ALERTS:
                return AdminAlertsFragment.newInstance();
            case FACULTY_REQUEST:
                return FacultyAccessRequestFragment.newInstance();
            case FACULTY_NOTIFICATIONS:
            case NOTIFICATIONS:
                return NotificationCenterFragment.newInstance();
            case STUDENT_PASSES:
                return StudentGuestPassFragment.newInstance();
            case ADMIN_VEHICLES:
                return AdminVehicleReviewFragment.newInstance();
            case SPONSOR_VEHICLES:
                return SponsorVehicleRegistrationFragment.newInstance();
            case MONITOR_REPORT:
                return MonitorViolationReportFragment.newInstance();
            case MONITOR_MY_REPORTS:
                return MonitorMyReportsFragment.newInstance();
            case VIOLATION_DIRECTORY:
                return AdminViolationDirectoryFragment.newInstance();
            case BANNED_LIST:
                return AdminBannedListFragment.newInstance();
            case STUDENT_CHARGES:
                return StudentChargesFragment.newInstance();
            case STUDENT_WARNINGS:
                return StudentWarningsFragment.newInstance();
            case ADMIN_CHARGES:
                return AdminChargesFragment.newInstance();
            case ADMIN_ANALYTICS:
                return AdminTrafficAnalyticsFragment.newInstance();
            case ADMIN_SECURITY:
                return AdminCategoryFragment.newInstance(RoleDestination.ADMIN_SECURITY);
            case ADMIN_ACCESS:
                return AdminCategoryFragment.newInstance(RoleDestination.ADMIN_ACCESS);
            case ADMIN_TAKE_ACTION:
                return AdminTakeActionFragment.newInstance();
            case ANNOUNCEMENTS:
                return SecurityAnnouncementsFragment.newInstance();
            case GUARD_DENY:
                return GuardDenyFragment.newInstance();
            default:
                return null;
        }
    }

    @NonNull
    private static List<NavItem> navItemsForRole(@Nullable String rawRole) {
        String role = normalize(rawRole);
        List<NavItem> items = new ArrayList<>();
        if ("guard".equals(role)) {
            items.add(new NavItem(RoleDestination.DASHBOARD, R.string.nav_dashboard, android.R.drawable.ic_dialog_dialer));
            items.add(new NavItem(RoleDestination.SCAN, R.string.nav_qr_scan, android.R.drawable.ic_menu_camera));
            items.add(new NavItem(RoleDestination.ANNOUNCEMENTS, R.string.nav_announcements, android.R.drawable.ic_dialog_email));
        } else if ("student".equals(role)) {
            items.add(new NavItem(RoleDestination.STUDENT_PASSES, R.string.nav_guest_passes, android.R.drawable.ic_menu_agenda));
            items.add(new NavItem(RoleDestination.SPONSOR_VEHICLES, R.string.nav_vehicles, android.R.drawable.ic_menu_directions));
            items.add(new NavItem(RoleDestination.NOTIFICATIONS, R.string.nav_notifications, android.R.drawable.ic_dialog_email));
        } else if ("faculty".equals(role)) {
            items.add(new NavItem(RoleDestination.FACULTY_REQUEST, R.string.nav_access_requests, android.R.drawable.ic_menu_send));
            items.add(new NavItem(RoleDestination.NOTIFICATIONS, R.string.nav_notifications, android.R.drawable.ic_dialog_email));
            items.add(new NavItem(RoleDestination.SPONSOR_VEHICLES, R.string.nav_vehicles, android.R.drawable.ic_menu_directions));
        } else if ("monitor".equals(role)) {
            items.add(new NavItem(RoleDestination.MONITOR_REPORT, R.string.nav_report, android.R.drawable.ic_menu_upload));
            items.add(new NavItem(RoleDestination.MONITOR_MY_REPORTS, R.string.nav_my_reports, android.R.drawable.ic_menu_agenda));
            items.add(new NavItem(RoleDestination.ANNOUNCEMENTS, R.string.nav_announcements, android.R.drawable.ic_dialog_email));
        } else if ("admin".equals(role)) {
            items.add(new NavItem(RoleDestination.DIRECTORY, R.string.nav_dashboard, android.R.drawable.ic_menu_myplaces));
            items.add(new NavItem(RoleDestination.ADMIN_SECURITY, R.string.nav_security, android.R.drawable.ic_dialog_alert));
            items.add(new NavItem(RoleDestination.ADMIN_ACCESS, R.string.nav_access, android.R.drawable.ic_menu_manage));
            items.add(new NavItem(RoleDestination.AUDIT, R.string.nav_audit, android.R.drawable.ic_menu_recent_history));
        } else {
            items.add(new NavItem(RoleDestination.DIRECTORY, R.string.nav_home, android.R.drawable.ic_menu_myplaces));
        }
        items.add(new NavItem(RoleDestination.LOGOUT, R.string.nav_logout, android.R.drawable.ic_lock_power_off));
        return items;
    }

    @NonNull
    private static RoleDestination resolveActiveDestinationForRole(
            @NonNull RoleDestination active,
            @Nullable String rawRole
    ) {
        String role = normalize(rawRole);
        if ("admin".equals(role)) {
            if (active == RoleDestination.ALERTS
                    || active == RoleDestination.VIOLATION_DIRECTORY
                    || active == RoleDestination.BANNED_LIST
                    || active == RoleDestination.ADMIN_CHARGES
                    || active == RoleDestination.ADMIN_SECURITY) {
                return RoleDestination.ADMIN_SECURITY;
            }
            if (active == RoleDestination.ADMIN_VEHICLES
                    || active == RoleDestination.RULES
                    || active == RoleDestination.DASHBOARD
                    || active == RoleDestination.SEARCH
                    || active == RoleDestination.SCAN
                    || active == RoleDestination.ADMIN_TAKE_ACTION
                    || active == RoleDestination.ADMIN_ACCESS) {
                return RoleDestination.ADMIN_ACCESS;
            }
            if (active == RoleDestination.ADMIN_ANALYTICS || active == RoleDestination.AUDIT) {
                return RoleDestination.AUDIT;
            }
            if (active == RoleDestination.USERS || active == RoleDestination.DIRECTORY) {
                return RoleDestination.DIRECTORY;
            }
        }
        RoleDestination resolved = resolveDestinationForRole(active, role);
        return resolved;
    }

    @NonNull
    private static String safeProfileValue(@Nullable UserProfile profile, boolean uid) {
        if (profile == null) {
            return "";
        }
        return uid ? profile.getUid() : profile.getDisplayName();
    }

    @NonNull
    private static String normalize(@Nullable String role) {
        if (role == null) {
            return "";
        }
        return role.toLowerCase(Locale.getDefault());
    }

    private static void styleItem(
            @NonNull Fragment owner,
            @NonNull LinearLayout container,
            int iconId,
            int labelId,
            boolean selected
    ) {
        ImageView icon = container.findViewById(iconId);
        TextView label = container.findViewById(labelId);
        container.setBackgroundResource(selected ? R.drawable.bg_bottom_nav_selected : android.R.color.transparent);
        int tint = ContextCompat.getColor(
                owner.requireContext(),
                selected ? R.color.md_primary : R.color.nav_unselected
        );
        if (icon != null) {
            icon.setColorFilter(tint);
        }
        if (label != null) {
            label.setTextColor(tint);
        }
    }
}
