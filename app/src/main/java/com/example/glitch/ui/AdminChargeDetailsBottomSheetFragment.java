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

public class AdminChargeDetailsBottomSheetFragment extends BottomSheetDialogFragment {
    public static final String TAG = "AdminChargeDetailsBottomSheet";
    public static final String RESULT_KEY = "admin_charge_details_result";
    public static final String RESULT_ACTION = "result_action";
    public static final String RESULT_CHARGE_ID = "result_charge_id";

    public static final String ACTION_APPROVE_REMOVAL = "approve_removal";
    public static final String ACTION_REJECT_REMOVAL = "reject_removal";

    private static final String ARG_CHARGE_ID = "arg_charge_id";
    private static final String ARG_STATUS = "arg_status";
    private static final String ARG_REASON = "arg_reason";
    private static final String ARG_AMOUNT = "arg_amount";
    private static final String ARG_GUEST = "arg_guest";
    private static final String ARG_SPONSOR = "arg_sponsor";
    private static final String ARG_NOTE = "arg_note";

    @NonNull
    public static AdminChargeDetailsBottomSheetFragment newInstance(
            @NonNull String chargeId,
            @NonNull String status,
            @NonNull String reason,
            @NonNull String amount,
            @NonNull String guest,
            @NonNull String sponsor,
            @NonNull String note
    ) {
        AdminChargeDetailsBottomSheetFragment fragment = new AdminChargeDetailsBottomSheetFragment();
        Bundle args = new Bundle();
        args.putString(ARG_CHARGE_ID, chargeId);
        args.putString(ARG_STATUS, status);
        args.putString(ARG_REASON, reason);
        args.putString(ARG_AMOUNT, amount);
        args.putString(ARG_GUEST, guest);
        args.putString(ARG_SPONSOR, sponsor);
        args.putString(ARG_NOTE, note);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_admin_charge_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Bundle args = requireArguments();

        String chargeId = safe(args, ARG_CHARGE_ID);
        String status = safe(args, ARG_STATUS);
        String reason = safe(args, ARG_REASON);
        String amount = safe(args, ARG_AMOUNT);
        String guest = safe(args, ARG_GUEST);
        String sponsor = safe(args, ARG_SPONSOR);
        String note = safe(args, ARG_NOTE);

        TextView textStatus = view.findViewById(R.id.text_charge_detail_status);
        TextView textReason = view.findViewById(R.id.text_charge_detail_reason);
        TextView textAmount = view.findViewById(R.id.text_charge_detail_amount);
        TextView textGuest = view.findViewById(R.id.text_charge_detail_guest);
        TextView textSponsor = view.findViewById(R.id.text_charge_detail_sponsor);
        TextView textNote = view.findViewById(R.id.text_charge_detail_note);
        View containerActions = view.findViewById(R.id.container_charge_actions);
        MaterialButton buttonApprove = view.findViewById(R.id.button_charge_mark_paid);
        MaterialButton buttonReject = view.findViewById(R.id.button_charge_remove);
        MaterialButton buttonClose = view.findViewById(R.id.button_charge_close);

        textStatus.setText("Status: " + status);
        textReason.setText("Reason: " + reason);
        textAmount.setText("Amount: " + amount);
        textGuest.setText("Visitor: " + guest);
        textSponsor.setText(sponsor.isEmpty() ? "" : "Sponsor: " + sponsor);
        textNote.setText(note.isEmpty() ? "" : "Note: " + note);

        boolean removalRequested = "removal requested".equalsIgnoreCase(status.trim());
        containerActions.setVisibility(removalRequested ? View.VISIBLE : View.GONE);

        if (removalRequested) {
            buttonApprove.setText("Approve Removal");
            buttonReject.setText("Reject Removal");
        }

        buttonApprove.setOnClickListener(v -> dispatch(ACTION_APPROVE_REMOVAL, chargeId));
        buttonReject.setOnClickListener(v -> dispatch(ACTION_REJECT_REMOVAL, chargeId));
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
