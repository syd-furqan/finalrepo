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

import java.util.Locale;

/**
 * Centralized role-aware router for bottom-nav and CTA navigation actions.
 * Updated to handle backstack more intelligently and fix student routing loops.
 */
public final class RoleNavRouter {
    private RoleNavRouter() {
    }

    /**
     * Binds shared bottom navigation clicks and active-state styling for a fragment.
     */
    public static void bindBottomNav(
            @NonNull View root,
            @NonNull Fragment owner,
            @NonNull RoleDestination active
    ) {
        LinearLayout dashboard = root.findViewById(R.id.nav_item_dashboard);
        LinearLayout passes = root.findViewById(R.id.nav_item_passes);
        LinearLayout vehicles = root.findViewById(R.id.nav_item_vehicles);
        LinearLayout directory = root.findViewById(R.id.nav_item_directory);
        LinearLayout logout = root.findViewById(R.id.nav_item_logout);

        if (dashboard == null || passes == null || vehicles == null || directory == null || logout == null) {
            return;
        }

        dashboard.setOnClickListener(v -> route(owner, RoleDestination.DASHBOARD));
        passes.setOnClickListener(v -> route(owner, RoleDestination.PASSES));
        vehicles.setOnClickListener(v -> route(owner, RoleDestination.VEHICLES));
        directory.setOnClickListener(v -> route(owner, RoleDestination.DIRECTORY));
        logout.setOnClickListener(v -> route(owner, RoleDestination.LOGOUT));

        styleItem(
                owner,
                dashboard,
                R.id.nav_icon_dashboard,
                R.id.nav_label_dashboard,
                active == RoleDestination.DASHBOARD
        );
        styleItem(
                owner,
                passes,
                R.id.nav_icon_passes,
                R.id.nav_label_passes,
                active == RoleDestination.PASSES
        );
        styleItem(
                owner,
                vehicles,
                R.id.nav_icon_vehicles,
                R.id.nav_label_vehicles,
                active == RoleDestination.VEHICLES
        );
        styleItem(
                owner,
                directory,
                R.id.nav_icon_directory,
                R.id.nav_label_directory,
                active == RoleDestination.DIRECTORY
        );
        styleItem(
                owner,
                logout,
                R.id.nav_icon_logout,
                R.id.nav_label_logout,
                active == RoleDestination.LOGOUT
        );
    }

    /**
     * Routes to a destination according to the authenticated role.
     */
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
        if (owner.getClass().equals(target.getClass())) {
            return;
        }
        if (owner.requireActivity() instanceof NavigationHost) {
            // For Bottom Nav primary destinations, we do NOT add to backstack to avoid bloat.
            // Sub-screens (like Details) should still use host.showFragment(f, true).
            ((NavigationHost) owner.requireActivity()).showFragment(target, false);
        }
    }

    /**
     * Resolves bottom-nav routes into concrete role destinations.
     */
    @NonNull
    public static RoleDestination resolveDestinationForRole(
            @NonNull RoleDestination destination,
            @Nullable String rawRole
    ) {
        String role = normalize(rawRole);
        switch (destination) {
            case DASHBOARD:
                if ("admin".equals(role)) return RoleDestination.AUDIT;
                if ("guard".equals(role)) return RoleDestination.DASHBOARD;
                if ("faculty".equals(role)) return RoleDestination.FACULTY_REQUEST;
                // Student dashboard should be the Hub (Directory) or a summary, not creation.
                if ("student".equals(role)) return RoleDestination.DIRECTORY;
                return RoleDestination.DIRECTORY;
            case PASSES:
                if ("admin".equals(role)) return RoleDestination.RULES;
                if ("guard".equals(role)) return RoleDestination.SEARCH;
                if ("faculty".equals(role)) return RoleDestination.FACULTY_NOTIFICATIONS;
                if ("student".equals(role)) return RoleDestination.STUDENT_PASSES;
                return RoleDestination.DASHBOARD;
            case VEHICLES:
                if ("admin".equals(role)) return RoleDestination.ADMIN_VEHICLES;
                if ("guard".equals(role)) return RoleDestination.SCAN;
                if ("faculty".equals(role)) return RoleDestination.SPONSOR_VEHICLES;
                if ("student".equals(role)) return RoleDestination.SPONSOR_VEHICLES;
                return RoleDestination.DASHBOARD;
            case DIRECTORY:
                return RoleDestination.DIRECTORY;
            case LOGOUT:
                return RoleDestination.LOGOUT;
            default:
                return destination;
        }
    }

    @Nullable
    private static Fragment createFragmentForDestination(
            @NonNull RoleDestination destination,
            @Nullable UserProfile profile
    ) {
        switch (destination) {
            case DASHBOARD:
                return DashboardFragment.newInstance();
            case PASSES:
                return DashboardFragment.newInstance();
            case VEHICLES:
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
                return FacultyNotificationsFragment.newInstance();
            case STUDENT_PASSES:
                return StudentGuestPassFragment.newInstance();
            case GUARD_DENY:
                return GuardDenyFragment.newInstance();
            case ADMIN_VEHICLES:
                return AdminVehicleReviewFragment.newInstance();
            case SPONSOR_VEHICLES:
                return SponsorVehicleRegistrationFragment.newInstance();
            default:
                return null;
        }
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
        int tint = ContextCompat.getColor(owner.requireContext(), selected ? R.color.primary_navy : R.color.nav_unselected);
        if (icon != null) {
            icon.setColorFilter(tint);
        }
        if (label != null) {
            label.setTextColor(tint);
        }
    }
}
