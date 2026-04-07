package com.example.glitch.ui;

import android.app.Dialog;
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/**
 * Bottom sheet that shows request details and confirms exit logging.
 * Pattern: Dialog Fragment + FragmentResult API for parent communication.
 * Known issue: request values are passed via Bundle strings for simplicity in v1.
 */
public class EntryDetailsBottomSheetDialogFragment extends BottomSheetDialogFragment {
    public static final String TAG = "EntryDetailsBottomSheet";
    public static final String RESULT_KEY = "entry_details_result";
    public static final String RESULT_REQUEST_ID = "result_request_id";

    private static final String ARG_REQUEST_ID = "arg_request_id";
    private static final String ARG_FULL_NAME = "arg_full_name";
    private static final String ARG_ROLE = "arg_role";
    private static final String ARG_HOST = "arg_host";
    private static final String ARG_GATE = "arg_gate";
    private static final String ARG_ENTERED = "arg_entered";
    private static final String ARG_PROMPT_EXIT = "arg_prompt_exit";

    /**
     * Creates bottom sheet instance for a selected request.
     */
    @NonNull
    public static EntryDetailsBottomSheetDialogFragment newInstance(
            @NonNull String requestId,
            @NonNull String fullName,
            @NonNull String roleTag,
            @NonNull String hostName,
            @NonNull String guestIdNumber,
            @NonNull String gateLabel,
            @NonNull String enteredText,
            @NonNull String expiryText,
            boolean promptExit
    ) {
        EntryDetailsBottomSheetDialogFragment fragment = new EntryDetailsBottomSheetDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_REQUEST_ID, requestId);
        args.putString(ARG_FULL_NAME, fullName);
        args.putString(ARG_ROLE, roleTag);
        args.putString(ARG_HOST, hostName);
        args.putString(ARG_GATE, gateLabel);
        args.putString("arg_guest_id", guestIdNumber);
        args.putString(ARG_ENTERED, enteredText);
        args.putString("arg_expiry", expiryText);
        args.putBoolean(ARG_PROMPT_EXIT, promptExit);
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
        return inflater.inflate(R.layout.bottom_sheet_entry_details, container, false);
    }

    private void confirmLogEntry(@NonNull String requestId, @NonNull String fullName) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Confirm Entry")
                .setMessage("Grant campus access to " + fullName + "?")
                .setNegativeButton(R.string.cancel_action, (dialogInterface, i) -> dialogInterface.dismiss())
                .setPositiveButton("Grant", (dialogInterface, i) -> {
                    Bundle result = new Bundle();
                    result.putString(RESULT_REQUEST_ID, requestId);
                    result.putBoolean("is_entry_action", true);
                    getParentFragmentManager().setFragmentResult(RESULT_KEY, result);
                    dismiss();
                })
                .show();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Bundle args = requireArguments();

        String requestId = safeArg(args, ARG_REQUEST_ID);
        String fullName = safeArg(args, ARG_FULL_NAME);
        String roleTag = safeArg(args, ARG_ROLE);
        String hostName = safeArg(args, ARG_HOST);
        String guestId = safeArg(args, "arg_guest_id");
        String gateLabel = safeArg(args, ARG_GATE);
        String enteredText = safeArg(args, ARG_ENTERED);
        String expiryText = safeArg(args, "arg_expiry"); // The Supposed Exit
        boolean promptExit = args.getBoolean(ARG_PROMPT_EXIT, false);

        TextView textDetailsMessage = view.findViewById(R.id.text_details_message);
        TextView textVisitorId = view.findViewById(R.id.text_visitor_id);
        TextView textRequestName = view.findViewById(R.id.text_request_name);
        TextView textRequestRole = view.findViewById(R.id.text_request_role);
        TextView textRequestHost = view.findViewById(R.id.text_request_host);
        TextView textRequestGate = view.findViewById(R.id.text_request_gate);
        TextView textRequestEntered = view.findViewById(R.id.text_request_entered);
        TextView textExpiry = view.findViewById(R.id.text_request_expiry);
        TextView textRequestId = view.findViewById(R.id.text_request_id);
        MaterialButton buttonClose = view.findViewById(R.id.button_close);
        MaterialButton buttonLogExit = view.findViewById(R.id.button_sheet_log_exit);

        if ("--:--".equals(enteredText)) {
            buttonLogExit.setText("Grant Entry");
            buttonLogExit.setOnClickListener(v -> confirmLogEntry(requestId, fullName));
        } else {
            buttonLogExit.setText("Log Exit");
            buttonLogExit.setOnClickListener(v -> confirmLogExit(requestId, fullName));
        }

        buttonClose.setOnClickListener(v -> dismiss());


        textRequestName.setText(fullName);
        textRequestRole.setText(roleTag);
        textRequestHost.setText(getString(R.string.unknown_host_prefix, hostName));
        textRequestGate.setText(getString(R.string.gate_label) + ": " + gateLabel);
        textRequestEntered.setText(getString(R.string.entered_label) + ": " + enteredText);
        textRequestId.setText(getString(R.string.request_id_label) + ": " + requestId);
        if (promptExit) {
            textDetailsMessage.setText(getString(R.string.confirm_exit_message, fullName));
        }
        textExpiry.setText("Expiry: " + expiryText);
        textVisitorId.setText("Visitor ID: " + guestId);

        buttonClose.setOnClickListener(v -> dismiss());
    }

    private void confirmLogExit(@NonNull String requestId, @NonNull String fullName) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.confirm_exit_title)
                .setMessage(getString(R.string.confirm_exit_message, fullName))
                .setNegativeButton(R.string.cancel_action, (dialogInterface, i) -> dialogInterface.dismiss())
                .setPositiveButton(R.string.confirm_action, (dialogInterface, i) -> {
                    Bundle result = new Bundle();
                    result.putString(RESULT_REQUEST_ID, requestId);
                    getParentFragmentManager().setFragmentResult(RESULT_KEY, result);
                    dismiss();
                })
                .show();
    }

    @NonNull
    private String safeArg(@NonNull Bundle args, @NonNull String key) {
        String value = args.getString(key);
        return value == null ? "" : value;
    }
}
