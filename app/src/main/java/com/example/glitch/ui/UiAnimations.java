package com.example.glitch.ui;

import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.NonNull;

public final class UiAnimations {

    private UiAnimations() {}

    public static void animateFallIn(@NonNull ViewGroup container) {
        animateFallIn(container, 0);
    }

    public static void animateFallIn(@NonNull ViewGroup container, int startDelayMs) {
        int delay = startDelayMs;
        for (int i = 0; i < container.getChildCount(); i++) {
            View child = container.getChildAt(i);
            child.setAlpha(0f);
            child.setTranslationY(36f);
            child.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(300)
                    .setStartDelay(delay)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
            delay += 60;
        }
    }
}
