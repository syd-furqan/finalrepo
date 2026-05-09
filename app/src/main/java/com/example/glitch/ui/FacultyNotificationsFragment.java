package com.example.glitch.ui;

import androidx.annotation.NonNull;

/**
 * Legacy entry point retained for old references; notifications now use the shared center.
 */
public class FacultyNotificationsFragment extends NotificationCenterFragment {
    @NonNull
    public static FacultyNotificationsFragment newInstance() {
        return new FacultyNotificationsFragment();
    }
}
