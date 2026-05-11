package com.example.glitch.ui;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.glitch.R;
import com.example.glitch.data.RepositoryProvider;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;
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
        textRole.setText(buildRoleSubtitle(role, email));

        // Avatar initials
        TextView avatarView = view.findViewById(R.id.text_avatar_initials);
        if (avatarView != null) {
            String name = displayName.isEmpty() ? email : displayName;
            avatarView.setText(initials(name));
        }

        featureContainer.removeAllViews();
        bindRoleActions(featureContainer, role);
        animateCards(featureContainer);
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

    private void addAction(@NonNull LinearLayout container, @NonNull String label, @NonNull Runnable action) {
        pendingCards.add(buildFeatureCard(label, action));
        if (pendingCards.size() == 2) {
            flushPendingCards(container);
        }
    }

    private void flushPendingCards(@NonNull LinearLayout container) {
        if (pendingCards.isEmpty()) return;
        int dp = (int) getResources().getDisplayMetrics().density;
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowParams.topMargin = dp * 12;
        row.setLayoutParams(rowParams);

        for (int i = 0; i < pendingCards.size(); i++) {
            View card = pendingCards.get(i);
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            if (i > 0) cp.leftMargin = dp * 12;
            card.setLayoutParams(cp);
            row.addView(card);
        }
        // If odd card, add invisible placeholder so layout is consistent
        if (pendingCards.size() == 1) {
            View spacer = new View(requireContext());
            LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(0, 1, 1f);
            sp.leftMargin = dp * 12;
            spacer.setLayoutParams(sp);
            row.addView(spacer);
        }
        pendingCards.clear();
        container.addView(row);
    }

    @NonNull
    private View buildFeatureCard(@NonNull String label, @NonNull Runnable action) {
        int dp = (int) getResources().getDisplayMetrics().density;

        MaterialCardView card = new MaterialCardView(requireContext());
        card.setCardBackgroundColor(requireContext().getColor(R.color.gf_bg_surface));
        card.setRadius(dp * 20);
        card.setCardElevation(0);
        card.setStrokeColor(requireContext().getColor(R.color.gf_border));
        card.setStrokeWidth(dp);
        card.setClickable(true);
        card.setFocusable(true);
        card.setOnClickListener(v -> action.run());

        LinearLayout inner = new LinearLayout(requireContext());
        inner.setOrientation(LinearLayout.VERTICAL);
        inner.setGravity(Gravity.CENTER);
        inner.setPadding(dp * 20, dp * 24, dp * 20, dp * 24);

        // Icon container
        LinearLayout iconBox = new LinearLayout(requireContext());
        iconBox.setGravity(Gravity.CENTER);
        iconBox.setBackgroundResource(R.drawable.bg_icon_container);
        LinearLayout.LayoutParams ibParams = new LinearLayout.LayoutParams(dp * 56, dp * 56);
        iconBox.setLayoutParams(ibParams);

        ImageView icon = new ImageView(requireContext());
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp * 28, dp * 28);
        icon.setLayoutParams(iconParams);
        icon.setImageResource(iconForLabel(label));
        icon.setColorFilter(requireContext().getColor(R.color.gf_accent));
        iconBox.addView(icon);
        inner.addView(iconBox);

        // Label
        TextView text = new TextView(requireContext());
        text.setText(label);
        text.setTextColor(requireContext().getColor(R.color.gf_text_primary));
        text.setTextSize(13);
        text.setTypeface(text.getTypeface(), android.graphics.Typeface.BOLD);
        text.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tp.topMargin = dp * 12;
        text.setLayoutParams(tp);
        inner.addView(text);

        card.addView(inner);
        return card;
    }

    @DrawableRes
    private int iconForLabel(@NonNull String label) {
        String l = label.toLowerCase(Locale.getDefault());
        if (l.contains("scan") || l.contains("qr")) return android.R.drawable.ic_menu_camera;
        if (l.contains("pass") || l.contains("guest")) return android.R.drawable.ic_menu_agenda;
        if (l.contains("log") || l.contains("audit")) return android.R.drawable.ic_menu_edit;
        if (l.contains("user") || l.contains("manage")) return android.R.drawable.ic_menu_manage;
        if (l.contains("ban") || l.contains("block")) return android.R.drawable.ic_delete;
        if (l.contains("alert") || l.contains("notif")) return android.R.drawable.stat_notify_more;
        if (l.contains("analytic") || l.contains("traffic")) return android.R.drawable.ic_menu_sort_by_size;
        if (l.contains("vehicle") || l.contains("car")) return android.R.drawable.ic_menu_directions;
        if (l.contains("rule") || l.contains("verif")) return android.R.drawable.ic_menu_view;
        if (l.contains("search")) return android.R.drawable.ic_menu_search;
        return android.R.drawable.ic_menu_compass;
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
}
