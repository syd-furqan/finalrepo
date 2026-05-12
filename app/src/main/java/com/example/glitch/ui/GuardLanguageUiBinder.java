package com.example.glitch.ui;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.core.widget.NestedScrollView;

import com.example.glitch.R;
import com.google.android.material.button.MaterialButton;

final class GuardLanguageUiBinder {
    private static final String TOGGLE_TAG = "guard_language_toggle_button";
    private static final String TOGGLE_ROW_TAG = "guard_language_toggle_row";

    private GuardLanguageUiBinder() {
    }

    static void bind(@NonNull View root, @NonNull Fragment owner) {
        if (!(root instanceof ViewGroup)) {
            return;
        }
        Context context = root.getContext();
        GuardLanguage current = GuardLanguageHelper.currentGuardLanguage(context);
        if (current == null) {
            return;
        }

        ViewGroup rootGroup = (ViewGroup) root;
        ViewGroup host = resolveHostContainer(rootGroup);
        LinearLayout topRow = findOrCreateTopRow(host, context);
        MaterialButton toggle = findOrCreateToggle(rootGroup, topRow, context);
        if (toggle == null) {
            return;
        }

        bindLabel(toggle, current);
        toggle.setEnabled(true);
        toggle.setClickable(true);
        toggle.setAlpha(1f);
        toggle.setOnClickListener(v -> {
            GuardLanguage before = GuardLanguageHelper.currentGuardLanguage(context);
            GuardLanguage next = before == GuardLanguage.UR ? GuardLanguage.EN : GuardLanguage.UR;
            GuardLanguageHelper.setCurrentGuardLanguage(context, next);
            bindLabel(toggle, next);
            if (owner.isAdded() && owner.getActivity() != null) {
                owner.requireActivity().recreate();
            }
        });
    }

    @NonNull
    private static ViewGroup resolveHostContainer(@NonNull ViewGroup root) {
        View nested = findFirstNestedScrollView(root);
        if (nested instanceof NestedScrollView) {
            NestedScrollView scroll = (NestedScrollView) nested;
            if (scroll.getChildCount() > 0 && scroll.getChildAt(0) instanceof ViewGroup) {
                return (ViewGroup) scroll.getChildAt(0);
            }
        }
        return root;
    }

    @NonNull
    private static LinearLayout findOrCreateTopRow(@NonNull ViewGroup host, @NonNull Context context) {
        View existing = host.findViewWithTag(TOGGLE_ROW_TAG);
        if (existing instanceof LinearLayout) {
            return (LinearLayout) existing;
        }

        LinearLayout row = new LinearLayout(context);
        row.setTag(TOGGLE_ROW_TAG);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(context, 10), 0, dp(context, 8));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        row.setLayoutParams(params);
        host.addView(row, 0);
        return row;
    }

    @NonNull
    private static MaterialButton findOrCreateToggle(
            @NonNull ViewGroup root,
            @NonNull LinearLayout topRow,
            @NonNull Context context
    ) {
        View existing = root.findViewWithTag(TOGGLE_TAG);
        if (existing instanceof MaterialButton) {
            MaterialButton toggle = (MaterialButton) existing;
            ViewParent parent = toggle.getParent();
            if (parent instanceof ViewGroup && parent != topRow) {
                ((ViewGroup) parent).removeView(toggle);
                topRow.addView(toggle);
            } else if (parent == null) {
                topRow.addView(toggle);
            }
            return toggle;
        }

        MaterialButton toggle = new MaterialButton(context);
        toggle.setTag(TOGGLE_TAG);
        toggle.setTextSize(12f);
        toggle.setAllCaps(false);
        toggle.setInsetTop(0);
        toggle.setInsetBottom(0);
        toggle.setMinHeight(dp(context, 36));
        toggle.setMinimumHeight(dp(context, 36));
        toggle.setPadding(dp(context, 12), dp(context, 4), dp(context, 12), dp(context, 4));
        toggle.setContentDescription(context.getString(R.string.guard_language_toggle_content_desc));
        toggle.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.primary_navy));
        toggle.setTextColor(ContextCompat.getColor(context, R.color.white));
        toggle.setStrokeWidth(0);

        ViewGroup.MarginLayoutParams params = new ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMarginEnd(dp(context, 2));
        toggle.setLayoutParams(params);
        topRow.addView(toggle);
        return toggle;
    }

    private static void bindLabel(@NonNull MaterialButton toggle, @NonNull GuardLanguage current) {
        if (current == GuardLanguage.UR) {
            toggle.setText("ار  |  EN");
            toggle.setBackgroundTintList(
                    ContextCompat.getColorStateList(toggle.getContext(), R.color.primary_navy)
            );
            toggle.setTextColor(ContextCompat.getColor(toggle.getContext(), R.color.white));
            return;
        }
        toggle.setText("EN  |  ار");
        toggle.setBackgroundTintList(
                ContextCompat.getColorStateList(toggle.getContext(), R.color.primary_navy)
        );
        toggle.setTextColor(ContextCompat.getColor(toggle.getContext(), R.color.white));
    }

    @Nullable
    private static View findFirstNestedScrollView(@NonNull ViewGroup root) {
        if (root instanceof NestedScrollView) {
            return root;
        }
        for (int i = 0; i < root.getChildCount(); i++) {
            View child = root.getChildAt(i);
            if (child instanceof NestedScrollView) {
                return child;
            }
            if (child instanceof ViewGroup) {
                View nested = findFirstNestedScrollView((ViewGroup) child);
                if (nested != null) {
                    return nested;
                }
            }
        }
        return null;
    }

    private static int dp(@NonNull Context context, int value) {
        return (int) (value * context.getResources().getDisplayMetrics().density);
    }
}
