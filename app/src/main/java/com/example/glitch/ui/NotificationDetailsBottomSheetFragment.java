package com.example.glitch.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.glitch.R;
import com.example.glitch.model.NotificationItem;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class NotificationDetailsBottomSheetFragment extends BottomSheetDialogFragment {
    public static final String TAG = "NotificationDetailsBottomSheet";
    public static final String RESULT_KEY = "notification_details_result";
    public static final String RESULT_SOURCE_COLLECTION = "source_collection";
    public static final String RESULT_SOURCE_ID = "source_id";

    private static final String ARG_TITLE = "title";
    private static final String ARG_MESSAGE = "message";
    private static final String ARG_TYPE = "type";
    private static final String ARG_SOURCE_ID = "source_id";
    private static final String ARG_SOURCE_COLLECTION = "source_collection";
    private static final String ARG_READ = "read";
    private static final String ARG_CREATED_AT = "created_at";
    private static final long TS_UNSET = -1L;

    @NonNull
    public static NotificationDetailsBottomSheetFragment newInstance(@NonNull NotificationItem item) {
        NotificationDetailsBottomSheetFragment fragment = new NotificationDetailsBottomSheetFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, item.getTitle());
        args.putString(ARG_MESSAGE, item.getMessage());
        args.putString(ARG_TYPE, item.getType());
        args.putString(ARG_SOURCE_ID, item.getSourceId());
        args.putString(ARG_SOURCE_COLLECTION, item.getSourceCollection());
        args.putBoolean(ARG_READ, item.isRead());
        args.putLong(ARG_CREATED_AT, asMillis(item.getCreatedAt()));
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_notification_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Bundle args = requireArguments();
        String title = safe(args, ARG_TITLE);
        String message = safe(args, ARG_MESSAGE);
        String type = safe(args, ARG_TYPE);
        String sourceId = safe(args, ARG_SOURCE_ID);
        String sourceCollection = safe(args, ARG_SOURCE_COLLECTION);
        boolean read = args.getBoolean(ARG_READ, false);
        long createdAt = args.getLong(ARG_CREATED_AT, TS_UNSET);

        TextView textType = view.findViewById(R.id.text_notification_detail_type);
        TextView textTitle = view.findViewById(R.id.text_notification_detail_title);
        TextView textMessage = view.findViewById(R.id.text_notification_detail_message);
        TextView textMeta = view.findViewById(R.id.text_notification_detail_meta);
        MaterialButton buttonOpen = view.findViewById(R.id.button_open_notification_source);
        MaterialButton buttonClose = view.findViewById(R.id.button_close_notification_details);

        textType.setText(type.trim().isEmpty() ? "NOTIFICATION" : type.trim().toUpperCase(Locale.US));
        textTitle.setText(valueOr(title, "Notification"));
        textMessage.setText(valueOr(message, "No message provided."));
        textMeta.setText(
                "Status: " + (read ? "Read" : "Unread")
                        + "\nCreated: " + formatMillis(createdAt)
                        + "\nSource: " + valueOr(sourceCollection, "Not available")
                        + "\nSource ID: " + valueOr(sourceId, "Not available")
        );

        boolean supported = isSupportedSource(sourceCollection) && !sourceId.trim().isEmpty();
        buttonOpen.setVisibility(supported ? View.VISIBLE : View.GONE);
        buttonOpen.setOnClickListener(v -> {
            Bundle result = new Bundle();
            result.putString(RESULT_SOURCE_COLLECTION, sourceCollection.trim());
            result.putString(RESULT_SOURCE_ID, sourceId.trim());
            getParentFragmentManager().setFragmentResult(RESULT_KEY, result);
            dismiss();
        });
        buttonClose.setOnClickListener(v -> dismiss());
    }

    public static boolean isSupportedSource(@NonNull String sourceCollection) {
        String normalized = sourceCollection.trim().toLowerCase(Locale.US);
        return "guest_passes".equals(normalized)
                || "vehicle_requests".equals(normalized)
                || "fine_cases".equals(normalized)
                || "violation_reports".equals(normalized);
    }

    @NonNull
    private String safe(@NonNull Bundle args, @NonNull String key) {
        String value = args.getString(key);
        return value == null ? "" : value;
    }

    @NonNull
    private String valueOr(@NonNull String value, @NonNull String fallback) {
        String trimmed = value.trim();
        return trimmed.isEmpty() ? fallback : trimmed;
    }

    @NonNull
    private String formatMillis(long millis) {
        if (millis <= TS_UNSET) {
            return "Not available";
        }
        return new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new java.util.Date(millis));
    }

    private static long asMillis(@Nullable Timestamp timestamp) {
        return timestamp == null ? TS_UNSET : timestamp.toDate().getTime();
    }
}
