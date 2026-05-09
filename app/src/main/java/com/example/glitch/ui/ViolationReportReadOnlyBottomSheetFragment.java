package com.example.glitch.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.glitch.R;
import com.example.glitch.model.ViolationReport;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class ViolationReportReadOnlyBottomSheetFragment extends BottomSheetDialogFragment {
    public static final String TAG = "ViolationReportReadOnlyBottomSheet";

    private static final String ARG_ID = "id";
    private static final String ARG_LEVEL = "level";
    private static final String ARG_STATUS = "status";
    private static final String ARG_SUBJECT_TYPE = "subject_type";
    private static final String ARG_GUEST_NAME = "guest_name";
    private static final String ARG_GUEST_CNIC = "guest_cnic";
    private static final String ARG_GUEST_PHONE = "guest_phone";
    private static final String ARG_STUDENT_NAME = "student_name";
    private static final String ARG_STUDENT_ID = "student_id";
    private static final String ARG_DETAIL = "detail";
    private static final String ARG_CREATED_AT = "created_at";
    private static final long TS_UNSET = -1L;

    @NonNull
    public static ViolationReportReadOnlyBottomSheetFragment newInstance(@NonNull ViolationReport report) {
        ViolationReportReadOnlyBottomSheetFragment fragment = new ViolationReportReadOnlyBottomSheetFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ID, report.getId());
        args.putString(ARG_LEVEL, report.getViolationLevel());
        args.putString(ARG_STATUS, report.getStatus());
        args.putString(ARG_SUBJECT_TYPE, report.getSubjectType());
        args.putString(ARG_GUEST_NAME, report.getGuestName());
        args.putString(ARG_GUEST_CNIC, report.getGuestCnic());
        args.putString(ARG_GUEST_PHONE, report.getGuestPhone());
        args.putString(ARG_STUDENT_NAME, report.getSubjectStudentName());
        args.putString(ARG_STUDENT_ID, report.getSubjectStudentId());
        args.putString(ARG_DETAIL, report.getDetail());
        args.putLong(ARG_CREATED_AT, asMillis(report.getCreatedAt()));
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_violation_report_read_only, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Bundle args = requireArguments();
        String subjectType = safe(args, ARG_SUBJECT_TYPE);
        String subject = ViolationReport.SUBJECT_STUDENT.equalsIgnoreCase(subjectType)
                ? valueOr(safe(args, ARG_STUDENT_NAME), "Student") + " | Student ID: " + valueOr(safe(args, ARG_STUDENT_ID), "N/A")
                : valueOr(safe(args, ARG_GUEST_NAME), "Guest")
                + " | CNIC: " + valueOr(safe(args, ARG_GUEST_CNIC), "N/A")
                + " | Phone: " + valueOr(safe(args, ARG_GUEST_PHONE), "N/A");

        TextView textLevel = view.findViewById(R.id.text_violation_readonly_level);
        TextView textBody = view.findViewById(R.id.text_violation_readonly_body);
        MaterialButton buttonClose = view.findViewById(R.id.button_close_violation_readonly);

        textLevel.setText(valueOr(safe(args, ARG_LEVEL), "Violation").toUpperCase(Locale.US));
        textBody.setText(
                "Report ID: " + valueOr(safe(args, ARG_ID), "N/A")
                        + "\nStatus: " + valueOr(safe(args, ARG_STATUS), "N/A")
                        + "\nSubject: " + subject
                        + "\nCreated: " + formatMillis(args.getLong(ARG_CREATED_AT, TS_UNSET))
                        + "\n\n" + valueOr(safe(args, ARG_DETAIL), "No detail provided.")
        );
        buttonClose.setOnClickListener(v -> dismiss());
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
