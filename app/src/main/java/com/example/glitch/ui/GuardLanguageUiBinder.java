package com.example.glitch.ui;

import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.glitch.R;
import com.example.glitch.auth.SessionManager;
import com.example.glitch.model.UserProfile;

final class GuardLanguageUiBinder {
    private GuardLanguageUiBinder() {
    }

    static void bind(@NonNull View root, @NonNull Fragment owner) {
        View toggle = root.findViewById(R.id.button_guard_language_toggle);
        TextView clock = root.findViewById(R.id.text_status_clock);
        if (toggle == null) {
            return;
        }
        UserProfile profile = SessionManager.getCurrentProfile();
        boolean isGuard = profile != null && "guard".equalsIgnoreCase(profile.getRole());
        toggle.setVisibility(isGuard ? View.VISIBLE : View.GONE);
        if (clock != null) {
            clock.setVisibility(isGuard ? View.GONE : View.VISIBLE);
        }
        if (!isGuard) {
            return;
        }
        GuardLanguage resolved = GuardLanguageHelper.currentGuardLanguage(owner.requireContext());
        final GuardLanguage current = resolved == null ? GuardLanguage.EN : resolved;
        if (toggle instanceof TextView) {
            ((TextView) toggle).setText(buildToggleLabel(current, owner));
        }
        toggle.setOnClickListener(v -> {
            if (!owner.isAdded()) {
                return;
            }
            GuardLanguage next = current == GuardLanguage.EN ? GuardLanguage.UR : GuardLanguage.EN;
            GuardLanguageHelper.setCurrentGuardLanguage(owner.requireContext(), next);
        });
    }

    @NonNull
    private static CharSequence buildToggleLabel(@NonNull GuardLanguage current, @NonNull Fragment owner) {
        String label = "EN | ار";
        SpannableString spannable = new SpannableString(label);
        int activeStart = current == GuardLanguage.EN ? 0 : 5;
        int activeEnd = current == GuardLanguage.EN ? 2 : label.length();
        int inactiveStart = current == GuardLanguage.EN ? 5 : 0;
        int inactiveEnd = current == GuardLanguage.EN ? label.length() : 2;
        int activeColor = ContextCompat.getColor(owner.requireContext(), R.color.white);
        int inactiveColor = ContextCompat.getColor(owner.requireContext(), R.color.md_primary_container);

        spannable.setSpan(new StyleSpan(Typeface.BOLD), activeStart, activeEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannable.setSpan(new ForegroundColorSpan(activeColor), activeStart, activeEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannable.setSpan(new ForegroundColorSpan(inactiveColor), inactiveStart, inactiveEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return spannable;
    }
}
