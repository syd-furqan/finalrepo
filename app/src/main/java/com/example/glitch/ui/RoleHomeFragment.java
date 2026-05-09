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
 * Role hub used as a compact overflow page for role-specific primary and secondary actions.
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
        if ("admin".equals(role)) {
            bindAdminDashboardActions(container, role);
            return;
        }
        for (RoleDestination destination : RoleNavRouter.getHubDestinationsForRole(role)) {
            if (destination == RoleDestination.DIRECTORY
                    || destination == RoleDestination.LOGOUT
                    || RoleNavRouter.createFragmentForDestination(destination, null) == null) {
                continue;
            }
            addAction(
                    container,
                    RoleNavRouter.getLabelForDestination(destination, role),
                    () -> openDestination(destination)
            );
        }
    }

    private void bindAdminDashboardActions(@NonNull LinearLayout container, @NonNull String role) {
        addSectionHeader(container, "Admin Setup");
        addDestinationAction(container, RoleDestination.USERS, role);

        addSectionHeader(container, "Security");
        for (RoleDestination destination : RoleNavRouter.getAdminCategoryDestinations(RoleDestination.ADMIN_SECURITY)) {
            addDestinationAction(container, destination, role);
        }

        addSectionHeader(container, "Access");
        for (RoleDestination destination : RoleNavRouter.getAdminCategoryDestinations(RoleDestination.ADMIN_ACCESS)) {
            addDestinationAction(container, destination, role);
        }

        addSectionHeader(container, "Audit");
        for (RoleDestination destination : RoleNavRouter.getAdminCategoryDestinations(RoleDestination.AUDIT)) {
            addDestinationAction(container, destination, role);
        }
    }

    private void addDestinationAction(
            @NonNull LinearLayout container,
            @NonNull RoleDestination destination,
            @NonNull String role
    ) {
        if (RoleNavRouter.createFragmentForDestination(destination, null) == null) {
            return;
        }
        addAction(
                container,
                RoleNavRouter.getLabelForDestination(destination, role),
                () -> openDestination(destination)
        );
    }

    private void addSectionHeader(@NonNull LinearLayout container, @NonNull String label) {
        TextView header = new TextView(requireContext());
        header.setText(label);
        header.setTextColor(requireContext().getColor(R.color.text_dark));
        header.setTextSize(13);
        header.setTypeface(header.getTypeface(), android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = container.getChildCount() == 0
                ? 0
                : (int) (getResources().getDisplayMetrics().density * 14);
        params.bottomMargin = (int) (getResources().getDisplayMetrics().density * 8);
        header.setLayoutParams(params);
        container.addView(header);
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

    private void openDestination(@NonNull RoleDestination destination) {
        Fragment fragment = RoleNavRouter.createFragmentForDestination(destination, null);
        if (fragment != null) {
            openFragment(fragment);
        }
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
