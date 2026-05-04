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

/**
 * Bottom sheet for charge details and resolution actions.
 */
public class AdminChargeDetailsBottomSheetFragment extends BottomSheetDialogFragment {
    public static final String TAG = "AdminChargeDetailsBottomSheet";
    public static final String RESULT_KEY = "admin_charge_details_result";
    public static final String RESULT_ACTION = "result_action";
    public static final String RESULT_CHARGE_ID = "result_charge_id";

    public static final String ACTION_MARK_PAID = "mark_paid";
    public static final String ACTION_REMOVE_CHARGE = "remove_charge";

    private static final String ARG_CHARGE_ID = "arg_charge_id";
    private static final String ARG_STATUS = "arg_status";
    private static final String ARG_REQUEST_ID = "arg_request_id";
    private static final String ARG_ALERT_ID = "arg_alert_id";
    private static final String ARG_GUEST = "arg_guest";
    private static final String ARG_SPONSOR_UID = "arg_sponsor_uid";

    @NonNull
    public static AdminChargeDetailsBottomSheetFragment newInstance(
            @NonNull String chargeId,
            @NonNull String status,
            @NonNull String requestId,
            @NonNull String alertId,
            @NonNull String guest,
            @NonNull String sponsorUid
    ) {
        AdminChargeDetailsBottomSheetFragment fragment = new AdminChargeDetailsBottomSheetFragment();
        Bundle args = new Bundle();
        args.putString(ARG_CHARGE_ID, chargeId);
        args.putString(ARG_STATUS, status);
        args.putString(ARG_REQUEST_ID, requestId);
        args.putString(ARG_ALERT_ID, alertId);
        args.putString(ARG_GUEST, guest);
        args.putString(ARG_SPONSOR_UID, sponsorUid);
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
        return inflater.inflate(R.layout.bottom_sheet_admin_charge_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Bundle args = requireArguments();

        String chargeId = safe(args, ARG_CHARGE_ID);
        String status = safe(args, ARG_STATUS);
        String requestId = safe(args, ARG_REQUEST_ID);
        String alertId = safe(args, ARG_ALERT_ID);
        String guest = safe(args, ARG_GUEST);
        String sponsorUid = safe(args, ARG_SPONSOR_UID);

        TextView textStatus = view.findViewById(R.id.text_charge_detail_status);
        TextView textChargeId = view.findViewById(R.id.text_charge_detail_id);
        TextView textRequestId = view.findViewById(R.id.text_charge_detail_request_id);
        TextView textAlertId = view.findViewById(R.id.text_charge_detail_alert_id);
        TextView textGuest = view.findViewById(R.id.text_charge_detail_guest);
        TextView textSponsorUid = view.findViewById(R.id.text_charge_detail_sponsor_uid);
        View actionContainer = view.findViewById(R.id.container_charge_actions);
        MaterialButton buttonPaid = view.findViewById(R.id.button_charge_mark_paid);
        MaterialButton buttonRemove = view.findViewById(R.id.button_charge_remove);
        MaterialButton buttonClose = view.findViewById(R.id.button_charge_close);

        textStatus.setText("Status: " + status);
        textChargeId.setText("Charge ID: " + chargeId);
        textRequestId.setText("Request ID: " + requestId);
        textAlertId.setText("Alert ID: " + alertId);
        textGuest.setText("Visitor: " + guest);
        textSponsorUid.setText("Sponsor UID: " + sponsorUid);

        boolean actionable = "charged".equalsIgnoreCase(status.trim());
        actionContainer.setVisibility(actionable ? View.VISIBLE : View.GONE);

        buttonPaid.setOnClickListener(v -> dispatch(ACTION_MARK_PAID, chargeId));
        buttonRemove.setOnClickListener(v -> dispatch(ACTION_REMOVE_CHARGE, chargeId));
        buttonClose.setOnClickListener(v -> dismiss());
    }

    private void dispatch(@NonNull String action, @NonNull String chargeId) {
        Bundle result = new Bundle();
        result.putString(RESULT_ACTION, action);
        result.putString(RESULT_CHARGE_ID, chargeId);
        getParentFragmentManager().setFragmentResult(RESULT_KEY, result);
        dismiss();
    }

    @NonNull
    private String safe(@NonNull Bundle args, @NonNull String key) {
        String value = args.getString(key);
        return value == null ? "" : value;
    }
}
