package com.example.glitch.ui;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.glitch.R;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

/**
 * Admin category hub used by the simplified admin bottom navigation.
 */
public class AdminCategoryFragment extends Fragment {
    private static final String ARG_CATEGORY = "arg_category";

    @NonNull
    public static AdminCategoryFragment newInstance(@NonNull RoleDestination category) {
        AdminCategoryFragment fragment = new AdminCategoryFragment();
        Bundle args = new Bundle();
        args.putString(ARG_CATEGORY, category.name());
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

        RoleDestination category = readCategory();
        String title = RoleNavRouter.getLabelForDestination(category, "admin");

        TextView textGreeting = view.findViewById(R.id.text_greeting);
        TextView textRole = view.findViewById(R.id.text_role_badge);
        LinearLayout featureContainer = view.findViewById(R.id.feature_container);
        TextView avatarView = view.findViewById(R.id.text_avatar_initials);

        textGreeting.setText(title);
        textRole.setText("ADMIN");
        if (avatarView != null) {
            avatarView.setText("A");
        }

        featureContainer.removeAllViews();
        pendingCards.clear();
        for (RoleDestination destination : RoleNavRouter.getAdminCategoryDestinations(category)) {
            if (RoleNavRouter.createFragmentForDestination(destination, null) == null) {
                continue;
            }
            addAction(
                    featureContainer,
                    destination,
                    RoleNavRouter.getLabelForDestination(destination, "admin"),
                    () -> openDestination(destination)
            );
        }
        flushPendingCards(featureContainer);

        RoleNavRouter.bindBottomNav(view, this, category);
    }

    @NonNull
    private RoleDestination readCategory() {
        Bundle args = getArguments();
        if (args == null) {
            return RoleDestination.ADMIN_SECURITY;
        }
        String value = args.getString(ARG_CATEGORY);
        if (value == null) {
            return RoleDestination.ADMIN_SECURITY;
        }
        try {
            return RoleDestination.valueOf(value);
        } catch (IllegalArgumentException ignored) {
            return RoleDestination.ADMIN_SECURITY;
        }
    }

    private final List<View> pendingCards = new ArrayList<>();

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
        if (pendingCards.isEmpty()) {
            return;
        }
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
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            if (i > 0) {
                params.leftMargin = dp * 10;
            }
            card.setLayoutParams(params);
            row.addView(card);
        }

        if (pendingCards.size() == 1) {
            View spacer = new View(requireContext());
            LinearLayout.LayoutParams spacerParams = new LinearLayout.LayoutParams(0, 1, 1f);
            spacerParams.leftMargin = dp * 10;
            spacer.setLayoutParams(spacerParams);
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
        card.setRadius(dp * 22);
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

        LinearLayout iconBox = new LinearLayout(requireContext());
        iconBox.setGravity(Gravity.CENTER);
        iconBox.setBackgroundResource(R.drawable.bg_icon_container);
        LinearLayout.LayoutParams iconBoxParams = new LinearLayout.LayoutParams(dp * 36, dp * 36);
        iconBox.setLayoutParams(iconBoxParams);

        ImageView icon = new ImageView(requireContext());
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp * 18, dp * 18);
        icon.setLayoutParams(iconParams);
        icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        icon.setImageResource(iconForDestination(destination));
        icon.setColorFilter(requireContext().getColor(R.color.md_primary));
        iconBox.addView(icon);
        inner.addView(iconBox);

        TextView text = new TextView(requireContext());
        text.setText(label);
        text.setTextColor(requireContext().getColor(R.color.gf_text_primary));
        text.setTextSize(15);
        text.setTypeface(text.getTypeface(), android.graphics.Typeface.BOLD);
        text.setMaxLines(2);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        textParams.leftMargin = dp * 12;
        text.setLayoutParams(textParams);
        inner.addView(text);

        card.addView(inner);
        return card;
    }

    @NonNull
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

    private void openDestination(@NonNull RoleDestination destination) {
        Fragment fragment = RoleNavRouter.createFragmentForDestination(destination, null);
        if (fragment == null || !(requireActivity() instanceof NavigationHost)) {
            return;
        }
        ((NavigationHost) requireActivity()).showFragment(fragment, true);
    }
}
