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
import com.google.android.material.button.MaterialButton;

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
        MaterialButton buttonLogout = view.findViewById(R.id.button_logout);

        textGreeting.setText(title);
        textRole.setText("ADMIN");
        buttonLogout.setVisibility(View.GONE);

        featureContainer.removeAllViews();
        for (RoleDestination destination : RoleNavRouter.getAdminCategoryDestinations(category)) {
            if (RoleNavRouter.createFragmentForDestination(destination, null) == null) {
                continue;
            }
            addAction(
                    featureContainer,
                    RoleNavRouter.getLabelForDestination(destination, "admin"),
                    () -> openDestination(destination)
            );
        }

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
        if (fragment == null || !(requireActivity() instanceof NavigationHost)) {
            return;
        }
        ((NavigationHost) requireActivity()).showFragment(fragment, true);
    }
}
