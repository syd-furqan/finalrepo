package com.example.glitch.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.glitch.R;
import com.example.glitch.model.AccessEvent;
import com.example.glitch.model.GatePolicy;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Bottom sheet detail view for a single audit event row.
 */
public class AuditEventDetailBottomSheetFragment extends BottomSheetDialogFragment {
    private static final String ARG_EVENT_TYPE = "arg_event_type";
    private static final String ARG_OUTCOME = "arg_outcome";
    private static final String ARG_ACTOR_UID = "arg_actor_uid";
    private static final String ARG_ACTOR_ROLE = "arg_actor_role";
    private static final String ARG_ENTITY_TYPE = "arg_entity_type";
    private static final String ARG_ENTITY_ID = "arg_entity_id";
    private static final String ARG_REQUEST_ID = "arg_request_id";
    private static final String ARG_GATE = "arg_gate";
    private static final String ARG_SOURCE = "arg_source";
    private static final String ARG_REASON = "arg_reason";
    private static final String ARG_DESCRIPTION = "arg_description";
    private static final String ARG_METADATA = "arg_metadata";
    private static final String ARG_SCHEMA = "arg_schema";
    private static final String ARG_CREATED_MS = "arg_created_ms";

    @NonNull
    public static AuditEventDetailBottomSheetFragment newInstance(@NonNull AccessEvent event) {
        AuditEventDetailBottomSheetFragment fragment = new AuditEventDetailBottomSheetFragment();
        Bundle args = new Bundle();
        args.putString(ARG_EVENT_TYPE, event.getEventType());
        args.putString(ARG_OUTCOME, event.getOutcome());
        args.putString(ARG_ACTOR_UID, event.getActorUid());
        args.putString(ARG_ACTOR_ROLE, event.getActorRole());
        args.putString(ARG_ENTITY_TYPE, event.getEntityType());
        args.putString(ARG_ENTITY_ID, event.getEntityId());
        args.putString(ARG_REQUEST_ID, event.getRequestId());
        args.putString(ARG_GATE, event.getGateLabel());
        args.putString(ARG_SOURCE, event.getSource());
        args.putString(ARG_REASON, event.getReasonCode());
        args.putString(ARG_DESCRIPTION, event.getDescription());
        args.putString(ARG_METADATA, event.getMetadata().toString());
        args.putInt(ARG_SCHEMA, event.getSchemaVersion());
        long createdMs = event.getCreatedAt() == null ? 0L : event.getCreatedAt().toDate().getTime();
        args.putLong(ARG_CREATED_MS, createdMs);
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
        return inflater.inflate(R.layout.bottom_sheet_audit_event_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Bundle args = getArguments();
        if (args == null) {
            dismiss();
            return;
        }

        String rawType = safe(args.getString(ARG_EVENT_TYPE));
        ((TextView) view.findViewById(R.id.text_detail_event_type)).setText(
                "Event: " + friendlyEventType(rawType)
        );
        ((TextView) view.findViewById(R.id.text_detail_outcome)).setText(
                "Outcome: " + safe(args.getString(ARG_OUTCOME))
        );
        ((TextView) view.findViewById(R.id.text_detail_actor)).setText(
                "Actor Role: " + safe(args.getString(ARG_ACTOR_ROLE))
        );
        ((TextView) view.findViewById(R.id.text_detail_entity)).setText(
                "Entity: " + safe(args.getString(ARG_ENTITY_TYPE))
        );
        view.findViewById(R.id.text_detail_request).setVisibility(View.GONE);
        ((TextView) view.findViewById(R.id.text_detail_gate)).setText(
                "Gate: " + GatePolicy.toDisplayLabel(args.getString(ARG_GATE))
        );
        ((TextView) view.findViewById(R.id.text_detail_source)).setText(
                "Source: " + safe(args.getString(ARG_SOURCE))
        );
        ((TextView) view.findViewById(R.id.text_detail_reason)).setText(
                "Reason Code: " + safe(args.getString(ARG_REASON))
        );
        ((TextView) view.findViewById(R.id.text_detail_description)).setText(
                "Description: " + safe(args.getString(ARG_DESCRIPTION))
        );
        ((TextView) view.findViewById(R.id.text_detail_metadata)).setText(
                "Metadata: " + safe(args.getString(ARG_METADATA))
        );
        ((TextView) view.findViewById(R.id.text_detail_created)).setText(
                "Created At: " + formatMillis(args.getLong(ARG_CREATED_MS, 0L))
        );
    }

    @NonNull
    private String friendlyEventType(@NonNull String raw) {
        switch (raw.trim().toLowerCase(Locale.getDefault())) {
            case "entry_allowed":          return "Entry Allowed";
            case "entry_denied":           return "Entry Denied";
            case "exit_logged":            return "Exit Logged";
            case "request_overdue":        return "Overdue";
            case "entry_reported_manual":  return "Reported (Manual)";
            case "entry_reported_overdue": return "Auto-Reported";
            case "pass_used":              return "Pass Used";
            case "pass_denied":            return "Pass Denied";
            case "pass_reported":          return "Pass Reported";
            case "pass_exited":            return "Pass Exited";
            case "entry_invalidated_ban":          return "Banned";
            case "violation_report_submitted":     return "Violation Reported";
            case "alert_created":                  return "Alert Created";
            case "alert_resolved":                 return "Alert Resolved";
            default:
                if (raw.trim().isEmpty()) return "Event";
                return raw.trim().replace('_', ' ');
        }
    }

    @NonNull
    private String formatMillis(long millis) {
        if (millis <= 0L) {
            return "N/A";
        }
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return formatter.format(new Date(millis));
    }

    @NonNull
    private String safe(@Nullable String value) {
        if (value == null || value.trim().isEmpty()) {
            return "N/A";
        }
        return value;
    }
}
