package com.example.glitch.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.glitch.R;
import com.example.glitch.data.RepositoryProvider;
import com.google.android.material.button.MaterialButton;

import java.util.Locale;

/**
 * Role-based landing screen that routes users to story-specific feature screens.
 * Pattern: Hub fragment with dynamic action list by authenticated role.
 * Known issue: role hub uses button list instead of final polished bottom navigation flows.
 */
public class RoleHomeFragment extends Fragment {
    private static final String ARG_UID = "arg_uid";
    private static final String ARG_DISPLAY_NAME = "arg_display_name";
    private static final String ARG_EMAIL = "arg_email";
    private static final String ARG_ROLE = "arg_role";

    @NonNull
    public static RoleHomeFragment newInstance(
            @NonNull String uid,
            @NonNull String displayName,
            @NonNull String email,
            @NonNull String role
    ) {
        RoleHomeFragment fragment = new RoleHomeFragment();
        Bundle args = new Bundle();
        args.putString(ARG_UID, uid);
        args.putString(ARG_DISPLAY_NAME, displayName);
        args.putString(ARG_EMAIL, email);
        args.putString(ARG_ROLE, role);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_role_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (AuthUiGuard.requireProfile(this) == null) {
            return;
        }
        Bundle args = requireArguments();
        String uid = safeArg(args, ARG_UID);
        String displayName = safeArg(args, ARG_DISPLAY_NAME);
        String email = safeArg(args, ARG_EMAIL);
        String role = safeArg(args, ARG_ROLE).toLowerCase(Locale.getDefault());

        TextView textGreeting = view.findViewById(R.id.text_greeting);
        TextView textRole = view.findViewById(R.id.text_role_badge);
        LinearLayout featureContainer = view.findViewById(R.id.feature_container);
        MaterialButton buttonLogout = view.findViewById(R.id.button_logout);

        textGreeting.setText(getString(R.string.role_home_greeting, displayName.isEmpty() ? email : displayName));
        textRole.setText(role.toUpperCase(Locale.getDefault()));

        featureContainer.removeAllViews();
        bindRoleActions(featureContainer, role);
        RoleNavRouter.bindBottomNav(view, this, RoleDestination.DIRECTORY);

        buttonLogout.setOnClickListener(v -> {
            RepositoryProvider.getAuthRepository().logout();
            NavigationHost host = host();
            if (host != null) {
                host.showLogin(true);
            }
        });
    }

    private void bindRoleActions(@NonNull LinearLayout container, @NonNull String role) {
        if ("guard".equals(role) || "admin".equals(role)) {

            addAction(container, "View Guard Dashboard", () -> openFragment(DashboardFragment.newInstance()));
            addAction(container, "Search And Verify", () -> openFragment(GuardSearchFragment.newInstance()));
            addAction(container, "Scan Guest QR Code", () -> openFragment(GuardQrScanFragment.newInstance()));
            addAction(container, "Deny / Flag Access", () -> openFragment(GuardDenyFragment.newInstance()));
        }

        if ("student".equals(role) || "admin".equals(role)) {
            addAction(container, getString(R.string.feature_student_guest_pass), () -> openFragment(StudentGuestPassFragment.newInstance()));
        }

        if ("faculty".equals(role)) {
            addAction(container, getString(R.string.feature_faculty_submit_request), () -> openFragment(FacultyAccessRequestFragment.newInstance()));
            addAction(container, getString(R.string.feature_faculty_notifications), () -> openFragment(FacultyNotificationsFragment.newInstance()));
        }
        if ("staff".equals(role)) {
            addAction(container, getString(R.string.feature_staff_vehicle), () -> openFragment(StaffVehicleRequestFragment.newInstance()));
        }

        if ("admin".equals(role)) {
            addAction(container, "VIEW SYSTEM AUDIT LOGS", () -> openFragment(AdminAuditLogFragment.newInstance()));
            addAction(container, getString(R.string.feature_admin_users), () -> openFragment(AdminUserManagementFragment.newInstance()));
            addAction(container, getString(R.string.feature_admin_rules), () -> openFragment(AdminVerificationRulesFragment.newInstance()));
            addAction(container, getString(R.string.feature_admin_alerts), () -> openFragment(AdminAlertsFragment.newInstance()));
        }
    }

    private void addAction(@NonNull LinearLayout container, @NonNull String label, @NonNull Runnable action) {
        MaterialButton button = new MaterialButton(requireContext());
        button.setText(label);
        button.setTextColor(requireContext().getColor(R.color.white));
        button.setBackgroundResource(R.drawable.bg_button_primary);
        button.setAllCaps(false);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                (int) (getResources().getDisplayMetrics().density * 52)
        );
        params.bottomMargin = (int) (getResources().getDisplayMetrics().density * 10);
        button.setLayoutParams(params);
        button.setOnClickListener(v -> action.run());
        container.addView(button);
    }

    private void openFragment(@NonNull Fragment fragment) {
        NavigationHost host = host();
        if (host != null) {
            host.showFragment(fragment, true);
        }
    }

    @Nullable
    private NavigationHost host() {
        if (requireActivity() instanceof NavigationHost) {
            return (NavigationHost) requireActivity();
        }
        return null;
    }

    @NonNull
    private String safeArg(@NonNull Bundle args, @NonNull String key) {
        String value = args.getString(key);
        return value == null ? "" : value;
    }
}
