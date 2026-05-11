package com.example.glitch.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.glitch.R;
import com.example.glitch.model.NotificationItem;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Full-screen, disruptive announcement interrupt for student/faculty roles.
 */
public class AnnouncementInterruptDialogFragment extends DialogFragment {
    public static final String TAG = "AnnouncementInterruptDialog";
    public static final String RESULT_KEY = "announcement_interrupt_result";
    public static final String RESULT_NOTIFICATION_ID = "notification_id";

    private static final String ARG_NOTIFICATION_ID = "arg_notification_id";
    private static final String ARG_TITLE = "arg_title";
    private static final String ARG_MESSAGE = "arg_message";
    private static final String ARG_CREATED_AT = "arg_created_at";
    private static final long TS_UNSET = -1L;

    @NonNull
    public static AnnouncementInterruptDialogFragment newInstance(@NonNull NotificationItem item) {
        AnnouncementInterruptDialogFragment fragment = new AnnouncementInterruptDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_NOTIFICATION_ID, item.getId());
        args.putString(ARG_TITLE, item.getTitle());
        args.putString(ARG_MESSAGE, item.getMessage());
        args.putLong(ARG_CREATED_AT, asMillis(item.getCreatedAt()));
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NORMAL, R.style.ThemeOverlay_Glitch_AnnouncementInterrupt);
        setCancelable(false);
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_announcement_interrupt, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            );
            getDialog().getWindow().setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
            );
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Bundle args = requireArguments();
        String notificationId = safe(args.getString(ARG_NOTIFICATION_ID));
        String title = valueOr(safe(args.getString(ARG_TITLE)), getString(R.string.announcements_banner_title));
        String message = valueOr(safe(args.getString(ARG_MESSAGE)), getString(R.string.no_notifications));
        long createdAt = args.getLong(ARG_CREATED_AT, TS_UNSET);

        TextView textChannel = view.findViewById(R.id.text_interrupt_channel);
        TextView textTitle = view.findViewById(R.id.text_interrupt_title);
        TextView textMessage = view.findViewById(R.id.text_interrupt_message);
        TextView textTimestamp = view.findViewById(R.id.text_interrupt_timestamp);
        MaterialButton buttonAcknowledge = view.findViewById(R.id.button_interrupt_acknowledge);

        textChannel.setText(R.string.announcements_banner_channel);
        textTitle.setText(title);
        textMessage.setText(message);
        textTimestamp.setText(getString(
                R.string.announcement_interrupt_timestamp_template,
                formatTimestamp(createdAt)
        ));

        buttonAcknowledge.setOnClickListener(v -> {
            Bundle result = new Bundle();
            result.putString(RESULT_NOTIFICATION_ID, notificationId);
            getParentFragmentManager().setFragmentResult(RESULT_KEY, result);
            dismissAllowingStateLoss();
        });
    }

    @NonNull
    private String formatTimestamp(long millis) {
        if (millis <= TS_UNSET) {
            return getString(R.string.announcement_interrupt_timestamp_unknown);
        }
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        return format.format(new Date(millis));
    }

    @NonNull
    private String safe(@Nullable String value) {
        return value == null ? "" : value.trim();
    }

    @NonNull
    private String valueOr(@NonNull String value, @NonNull String fallback) {
        return value.isEmpty() ? fallback : value;
    }

    private static long asMillis(@Nullable Timestamp timestamp) {
        return timestamp == null ? TS_UNSET : timestamp.toDate().getTime();
    }
}
