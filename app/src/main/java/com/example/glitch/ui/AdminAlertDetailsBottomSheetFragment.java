package com.example.glitch.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.glitch.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;

import java.util.Locale;

/**
 * Bottom sheet for admin security alert details and decision actions.
 */
public class AdminAlertDetailsBottomSheetFragment extends BottomSheetDialogFragment {
    public static final String TAG = "AdminAlertDetailsBottomSheet";
    public static final String RESULT_KEY = "admin_alert_details_result";
    public static final String RESULT_ACTION = "result_action";
    public static final String RESULT_ALERT_ID = "result_alert_id";
    public static final String RESULT_REQUEST_ID = "result_request_id";
    public static final String RESULT_SPONSOR_UID = "result_sponsor_uid";
    public static final String RESULT_GUEST_NAME = "result_guest_name";
    public static final String RESULT_GUEST_ID = "result_guest_id";

    public static final String ACTION_LOG_EXIT = "log_exit";
    public static final String ACTION_CHARGE = "charge";
    public static final String ACTION_MARK_REVIEWED = "mark_reviewed";

    private static final String ARG_ALERT_ID = "arg_alert_id";
    private static final String ARG_ALERT_TYPE = "arg_alert_type";
    private static final String ARG_REQUEST_ID = "arg_request_id";
    private static final String ARG_INCIDENT_STATUS = "arg_incident_status";
    private static final String ARG_INTERVENTION_SUMMARY = "arg_intervention_summary";
    private static final String ARG_GUARD = "arg_guard";
    private static final String ARG_VISITOR = "arg_visitor";
    private static final String ARG_SPONSOR = "arg_sponsor";
    private static final String ARG_GATE = "arg_gate";
    private static final String ARG_REASON = "arg_reason";
    private static final String ARG_SOURCE = "arg_source";
    private static final String ARG_MESSAGE = "arg_message";
    private static final String ARG_SPONSOR_UID = "arg_sponsor_uid";
    private static final String ARG_GUEST_NAME = "arg_guest_name";
    private static final String ARG_GUEST_ID = "arg_guest_id";

    @NonNull
    public static AdminAlertDetailsBottomSheetFragment newInstance(
            @NonNull String alertId,
            @NonNull String alertType,
            @NonNull String requestId,
            @NonNull String incidentStatus,
            @NonNull String interventionSummary,
            @NonNull String guard,
            @NonNull String visitor,
            @NonNull String sponsor,
            @NonNull String gate,
            @NonNull String reason,
            @NonNull String source,
            @NonNull String message,
            @NonNull String sponsorUid,
            @NonNull String guestName,
            @NonNull String guestId
    ) {
        AdminAlertDetailsBottomSheetFragment fragment = new AdminAlertDetailsBottomSheetFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ALERT_ID, alertId);
        args.putString(ARG_ALERT_TYPE, alertType);
        args.putString(ARG_REQUEST_ID, requestId);
        args.putString(ARG_INCIDENT_STATUS, incidentStatus);
        args.putString(ARG_INTERVENTION_SUMMARY, interventionSummary);
        args.putString(ARG_GUARD, guard);
        args.putString(ARG_VISITOR, visitor);
        args.putString(ARG_SPONSOR, sponsor);
        args.putString(ARG_GATE, gate);
        args.putString(ARG_REASON, reason);
        args.putString(ARG_SOURCE, source);
        args.putString(ARG_MESSAGE, message);
        args.putString(ARG_SPONSOR_UID, sponsorUid);
        args.putString(ARG_GUEST_NAME, guestName);
        args.putString(ARG_GUEST_ID, guestId);
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
        return inflater.inflate(R.layout.bottom_sheet_admin_alert_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Bundle args = requireArguments();

        String alertId = safe(args, ARG_ALERT_ID);
        String alertType = safe(args, ARG_ALERT_TYPE);
        String requestId = safe(args, ARG_REQUEST_ID);
        String incidentStatus = safe(args, ARG_INCIDENT_STATUS);
        String interventionSummary = safe(args, ARG_INTERVENTION_SUMMARY);
        String guard = safe(args, ARG_GUARD);
        String visitor = safe(args, ARG_VISITOR);
        String sponsor = safe(args, ARG_SPONSOR);
        String gate = safe(args, ARG_GATE);
        String reason = safe(args, ARG_REASON);
        String source = safe(args, ARG_SOURCE);
        String message = safe(args, ARG_MESSAGE);
        String sponsorUid = safe(args, ARG_SPONSOR_UID);
        String guestName = safe(args, ARG_GUEST_NAME);
        String guestId = safe(args, ARG_GUEST_ID);

        TextView textIncidentStatus = view.findViewById(R.id.text_alert_incident_status);
        TextView textInterventionSummary = view.findViewById(R.id.text_alert_intervention_summary);
        TextView textGuard = view.findViewById(R.id.text_alert_guard);
        TextView textVisitor = view.findViewById(R.id.text_alert_visitor);
        TextView textSponsor = view.findViewById(R.id.text_alert_sponsor);
        TextView textGate = view.findViewById(R.id.text_alert_gate);
        TextView textReason = view.findViewById(R.id.text_alert_reason);
        TextView textSource = view.findViewById(R.id.text_alert_source);
        TextView textMessage = view.findViewById(R.id.text_alert_message);
        View actionsContainer = view.findViewById(R.id.container_alert_actions);
        MaterialButton buttonLogExit = view.findViewById(R.id.button_alert_log_exit);
        MaterialButton buttonCharge = view.findViewById(R.id.button_alert_charge);
        MaterialButton buttonClose = view.findViewById(R.id.button_alert_close);

        textIncidentStatus.setText(labelValue("Incident", valueOr(incidentStatus, "new")));
        textInterventionSummary.setText(labelValue("Intervention", valueOr(interventionSummary, "N/A")));
        textGuard.setText(labelValue(actorLabel(alertType), valueOr(guard, "Unknown")));
        textVisitor.setText(labelValue(subjectLabel(alertType), valueOr(visitor, "N/A")));
        textSponsor.setText(labelValue(ownerLabel(alertType), valueOr(sponsor, "N/A")));
        textGate.setText(labelValue("Gate", valueOr(gate, "In-Gate")));
        textReason.setText(labelValue("Reason", valueOr(reason, "N/A")));
        textSource.setText(labelValue("Source", valueOr(source, "N/A")));
        textMessage.setText(labelValue("Message", valueOr(message, "N/A")));

        boolean actionable = isEntryReport(alertType)
                && "new".equalsIgnoreCase(incidentStatus.trim())
                && !requestId.trim().isEmpty();
        boolean reviewOnly = "new".equalsIgnoreCase(incidentStatus.trim()) && !actionable;
        actionsContainer.setVisibility(actionable || reviewOnly ? View.VISIBLE : View.GONE);
        if (reviewOnly) {
            buttonLogExit.setText("Mark Reviewed");
            buttonLogExit.setOnClickListener(v -> dispatchAction(
                    ACTION_MARK_REVIEWED,
                    alertId,
                    requestId,
                    sponsorUid,
                    guestName,
                    guestId
            ));
            buttonCharge.setVisibility(View.GONE);
        } else {
            buttonLogExit.setOnClickListener(v -> dispatchAction(
                    ACTION_LOG_EXIT,
                    alertId,
                    requestId,
                    sponsorUid,
                    guestName,
                    guestId
            ));
            buttonCharge.setVisibility(View.VISIBLE);
            buttonCharge.setOnClickListener(v -> dispatchAction(
                    ACTION_CHARGE,
                    alertId,
                    requestId,
                    sponsorUid,
                    guestName,
                    guestId
            ));
        }
        buttonClose.setOnClickListener(v -> dismiss());
    }

    private void dispatchAction(
            @NonNull String action,
            @NonNull String alertId,
            @NonNull String requestId,
            @NonNull String sponsorUid,
            @NonNull String guestName,
            @NonNull String guestId
    ) {
        Bundle result = new Bundle();
        result.putString(RESULT_ACTION, action);
        result.putString(RESULT_ALERT_ID, alertId);
        result.putString(RESULT_REQUEST_ID, requestId);
        result.putString(RESULT_SPONSOR_UID, sponsorUid);
        result.putString(RESULT_GUEST_NAME, guestName);
        result.putString(RESULT_GUEST_ID, guestId);
        getParentFragmentManager().setFragmentResult(RESULT_KEY, result);
        dismiss();
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
    private String labelValue(@NonNull String label, @NonNull String value) {
        return label + ": " + value;
    }

    @NonNull
    private String actorLabel(@NonNull String alertType) {
        return isEntryReport(alertType) ? "Guard" : "Actor";
    }

    @NonNull
    private String subjectLabel(@NonNull String alertType) {
        String normalized = alertType.trim().toLowerCase(Locale.getDefault());
        if ("vehicle_review".equals(normalized)) {
            return "Vehicle";
        }
        if ("charge_review".equals(normalized)) {
            return "Charge";
        }
        return isEntryReport(alertType) ? "Visitor" : "Subject";
    }

    @NonNull
    private String ownerLabel(@NonNull String alertType) {
        String normalized = alertType.trim().toLowerCase(Locale.getDefault());
        if ("vehicle_review".equals(normalized) || "charge_review".equals(normalized)) {
            return "Owner";
        }
        return "Sponsor";
    }

    private boolean isEntryReport(@NonNull String alertType) {
        return "entry_report".equalsIgnoreCase(alertType.trim());
    }
}
