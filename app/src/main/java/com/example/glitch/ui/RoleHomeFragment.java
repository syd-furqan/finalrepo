package com.example.glitch.ui;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.widget.ImageView;

import com.example.glitch.R;
import com.example.glitch.data.RepositoryProvider;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Role hub used as a compact overflow page for role-specific primary and secondary actions.
 */
public class RoleHomeFragment extends Fragment {
    private android.widget.LinearLayout featureContainerRef;
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

        textGreeting.setText(getString(R.string.role_home_greeting, displayName.isEmpty() ? email : displayName));
        textRole.setText(buildRoleSubtitle(role, email));

        // Avatar initials
        TextView avatarView = view.findViewById(R.id.text_avatar_initials);
        if (avatarView != null) {
            String name = displayName.isEmpty() ? email : displayName;
            avatarView.setText(initials(name));
        }

        featureContainerRef = featureContainer;
        featureContainer.removeAllViews();
        bindRoleActions(featureContainer, role);
        animateCards(featureContainer);
        RoleNavRouter.bindBottomNav(view, this, RoleDestination.DIRECTORY);
    }

    private void bindRoleActions(@NonNull LinearLayout container, @NonNull String role) {
        pendingCards.clear();
        if ("admin".equals(role)) {
            bindAdminDashboardActions(container, role);
            flushPendingCards(container);
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
                    destination,
                    RoleNavRouter.getLabelForDestination(destination, role),
                    () -> openDestination(destination)
            );
        }
        flushPendingCards(container);
    }

    private void bindAdminDashboardActions(@NonNull LinearLayout container, @NonNull String role) {
        addSectionHeader(container, "Admin Setup");
        addDestinationAction(container, RoleDestination.USERS, role);
        flushPendingCards(container);

        addSectionHeader(container, "Security");
        for (RoleDestination destination : RoleNavRouter.getAdminCategoryDestinations(RoleDestination.ADMIN_SECURITY)) {
            addDestinationAction(container, destination, role);
        }
        flushPendingCards(container);

        addSectionHeader(container, "Access");
        for (RoleDestination destination : RoleNavRouter.getAdminCategoryDestinations(RoleDestination.ADMIN_ACCESS)) {
            addDestinationAction(container, destination, role);
        }
        flushPendingCards(container);

        addSectionHeader(container, "Audit");
        for (RoleDestination destination : RoleNavRouter.getAdminCategoryDestinations(RoleDestination.AUDIT)) {
            addDestinationAction(container, destination, role);
        }
        flushPendingCards(container);
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
            destination,
                RoleNavRouter.getLabelForDestination(destination, role),
                () -> openDestination(destination)
        );
    }

    // Pending cards buffered until we can emit a full row of two
    private final List<View> pendingCards = new ArrayList<>();

    private void addSectionHeader(@NonNull LinearLayout container, @NonNull String label) {
        flushPendingCards(container);
        int dp = (int) getResources().getDisplayMetrics().density;
        TextView header = new TextView(requireContext());
        header.setText(label.toUpperCase(Locale.getDefault()));
        header.setTextColor(requireContext().getColor(R.color.gf_text_hint));
        header.setTextSize(11);
        header.setLetterSpacing(0.08f);
        header.setTypeface(header.getTypeface(), android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = container.getChildCount() == 0 ? 0 : dp * 16;
        params.bottomMargin = dp * 8;
        header.setLayoutParams(params);
        container.addView(header);
    }

    private void addAction(
            @NonNull LinearLayout container,
            @NonNull RoleDestination destination,
            @NonNull String label,
            @NonNull Runnable action
    ) {
        pendingCards.add(buildFeatureCard(destination, label, action));
        if (pendingCards.size() == 2) {
            flushPendingCards(container);
        }
    }

    private void flushPendingCards(@NonNull LinearLayout container) {
        if (pendingCards.isEmpty()) return;
        int dp = (int) getResources().getDisplayMetrics().density;
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_HORIZONTAL);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowParams.topMargin = dp * 10;
        row.setLayoutParams(rowParams);

        for (int i = 0; i < pendingCards.size(); i++) {
            View card = pendingCards.get(i);
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            if (i > 0) cp.leftMargin = dp * 10;
            card.setLayoutParams(cp);
            row.addView(card);
        }
        // If odd card, add invisible placeholder so layout is consistent
        if (pendingCards.size() == 1) {
            View spacer = new View(requireContext());
            LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(0, 1, 1f);
            sp.leftMargin = dp * 10;
            spacer.setLayoutParams(sp);
            row.addView(spacer);
        }
        pendingCards.clear();
        container.addView(row);
    }

    @NonNull
    private View buildFeatureCard(@NonNull RoleDestination destination, @NonNull String label, @NonNull Runnable action) {
        int dp = (int) getResources().getDisplayMetrics().density;

        MaterialCardView card = new MaterialCardView(requireContext());
        card.setCardBackgroundColor(requireContext().getColor(R.color.md_surface));
        card.setRadius(dp * 18);
        card.setCardElevation(0f);
        card.setMaxCardElevation(0f);
        card.setStrokeColor(requireContext().getColor(R.color.md_outline_variant));
        card.setStrokeWidth(dp);
        card.setClickable(true);
        card.setFocusable(true);
        card.setOnClickListener(v -> action.run());

        LinearLayout inner = new LinearLayout(requireContext());
        inner.setOrientation(LinearLayout.HORIZONTAL);
        inner.setGravity(Gravity.CENTER_VERTICAL);
        inner.setPadding(dp * 14, dp * 12, dp * 14, dp * 12);

        // Icon container
        LinearLayout iconBox = new LinearLayout(requireContext());
        iconBox.setGravity(Gravity.CENTER);
        iconBox.setBackgroundResource(R.drawable.bg_icon_container);
        LinearLayout.LayoutParams ibParams = new LinearLayout.LayoutParams(dp * 36, dp * 36);
        iconBox.setLayoutParams(ibParams);

        ImageView icon = new ImageView(requireContext());
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp * 18, dp * 18);
        icon.setLayoutParams(iconParams);
        icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        icon.setImageResource(iconForDestination(destination));
        icon.setColorFilter(requireContext().getColor(R.color.md_primary));
        iconBox.addView(icon);
        inner.addView(iconBox);

        // Label
        TextView text = new TextView(requireContext());
        text.setText(label);
        text.setTextColor(requireContext().getColor(R.color.gf_text_primary));
        text.setTextSize(15);
        text.setTypeface(text.getTypeface(), android.graphics.Typeface.BOLD);
        text.setMaxLines(2);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        tp.leftMargin = dp * 12;
        text.setLayoutParams(tp);
        inner.addView(text);

        card.addView(inner);
        return card;
    }

    @DrawableRes
    private int iconForDestination(@NonNull RoleDestination destination) {
        switch (destination) {
            case DASHBOARD:
                return android.R.drawable.ic_menu_view;
            case SEARCH:
                return android.R.drawable.ic_menu_search;
            case SCAN:
                return android.R.drawable.ic_menu_camera;
            case AUDIT:
                return android.R.drawable.ic_menu_recent_history;
            case USERS:
                return android.R.drawable.ic_menu_manage;
            case RULES:
                return android.R.drawable.ic_menu_view;
            case ALERTS:
                return android.R.drawable.ic_dialog_alert;
            case FACULTY_REQUEST:
                return android.R.drawable.ic_menu_agenda;
            case FACULTY_NOTIFICATIONS:
            case NOTIFICATIONS:
                return android.R.drawable.ic_dialog_email;
            case STUDENT_PASSES:
                return android.R.drawable.ic_menu_agenda;
            case SPONSOR_VEHICLES:
            case ADMIN_VEHICLES:
                return android.R.drawable.ic_menu_directions;
            case MONITOR_REPORT:
                return android.R.drawable.ic_menu_delete;
            case MONITOR_MY_REPORTS:
                return android.R.drawable.ic_menu_info_details;
            case VIOLATION_DIRECTORY:
                return android.R.drawable.ic_delete;
            case BANNED_LIST:
                return android.R.drawable.ic_delete;
            case STUDENT_CHARGES:
            case ADMIN_CHARGES:
                return android.R.drawable.ic_menu_agenda;
            case STUDENT_WARNINGS:
                return android.R.drawable.ic_dialog_alert;
            case ADMIN_ANALYTICS:
                return android.R.drawable.ic_menu_sort_by_size;
            case ADMIN_SECURITY:
                return android.R.drawable.ic_secure;
            case ADMIN_ACCESS:
                return android.R.drawable.ic_menu_directions;
            case ADMIN_TAKE_ACTION:
                return android.R.drawable.ic_menu_edit;
            case ANNOUNCEMENTS:
                return android.R.drawable.ic_dialog_info;
            case LOGOUT:
                return android.R.drawable.ic_lock_power_off;
            case DIRECTORY:
            default:
                return android.R.drawable.ic_menu_compass;
        }
    }

    private void animateCards(@NonNull LinearLayout container) {
        int delay = 0;
        for (int i = 0; i < container.getChildCount(); i++) {
            View child = container.getChildAt(i);
            if (!(child instanceof LinearLayout)) continue;
            LinearLayout row = (LinearLayout) child;
            for (int j = 0; j < row.getChildCount(); j++) {
                View card = row.getChildAt(j);
                if (!(card instanceof MaterialCardView)) continue;
                card.setAlpha(0f);
                card.setTranslationY(24f);
                card.animate()
                        .alpha(1f)
                        .translationY(0f)
                        .setDuration(280)
                        .setStartDelay(delay)
                        .setInterpolator(new DecelerateInterpolator())
                        .start();
                delay += 55;
            }
        }
    }

    @NonNull
    private String buildRoleSubtitle(@NonNull String role, @NonNull String email) {
        String r = role.toLowerCase(Locale.getDefault());
        if (r.contains("guard")) return "Security Guard · On Duty";
        if (r.contains("admin")) return "Administrator · LUMS";
        if (r.contains("faculty")) return "Faculty · LUMS";
        return "Student · LUMS";
    }

    @NonNull
    private String initials(@NonNull String name) {
        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2) return String.valueOf(parts[0].charAt(0)).toUpperCase(Locale.getDefault())
                + String.valueOf(parts[1].charAt(0)).toUpperCase(Locale.getDefault());
        if (!name.isEmpty()) return String.valueOf(name.charAt(0)).toUpperCase(Locale.getDefault());
        return "?";
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

    @Override
    public void onResume() {
        super.onResume();
        if (featureContainerRef != null) animateCards(featureContainerRef);
    }
}
