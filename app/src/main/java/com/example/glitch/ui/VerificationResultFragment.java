package com.example.glitch.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.glitch.R;
import com.example.glitch.data.EntryRequestRepository;
import com.example.glitch.data.RepositoryProvider;
import com.example.glitch.model.GatePolicy;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

/**
 * Guard verification profile screen for approve/deny decisions.
 * Pattern: Detail fragment with explicit action buttons and denial reason input.
 * Known issue: profile photo/map context sections are simplified for v1.
 */
public class VerificationResultFragment extends Fragment {
    private static final String ARG_REQUEST_ID = "arg_request_id";
    private static final String ARG_NAME = "arg_name";
    private static final String ARG_ROLE = "arg_role";
    private static final String ARG_HOST = "arg_host";
    private static final String ARG_GATE = "arg_gate";
    private static final String ARG_ENTERED = "arg_entered";

    private EntryRequestRepository repository;

    @NonNull
    public static VerificationResultFragment newInstance(
            @NonNull String requestId,
            @NonNull String name,
            @NonNull String role,
            @NonNull String host,
            @NonNull String gate,
            @NonNull String enteredTime
    ) {
        VerificationResultFragment fragment = new VerificationResultFragment();
        Bundle args = new Bundle();
        args.putString(ARG_REQUEST_ID, requestId);
        args.putString(ARG_NAME, name);
        args.putString(ARG_ROLE, role);
        args.putString(ARG_HOST, host);
        args.putString(ARG_GATE, gate);
        args.putString(ARG_ENTERED, enteredTime);
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
        return inflater.inflate(R.layout.fragment_verification_result, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = RepositoryProvider.getRepository();
        Bundle args = requireArguments();

        String requestId = safeArg(args, ARG_REQUEST_ID);
        String name = safeArg(args, ARG_NAME);
        String role = safeArg(args, ARG_ROLE);
        String host = safeArg(args, ARG_HOST);
        String gate = GatePolicy.toDisplayLabel(safeArg(args, ARG_GATE));
        String entered = safeArg(args, ARG_ENTERED);

        TextView textName = view.findViewById(R.id.text_subject_name);
        TextView textMeta = view.findViewById(R.id.text_subject_meta);
        TextView textGate = view.findViewById(R.id.text_gate_value);
        TextView textEntered = view.findViewById(R.id.text_entered_value);
        TextInputEditText inputReason = view.findViewById(R.id.input_denial_reason);
        MaterialButton buttonApprove = view.findViewById(R.id.button_approve);
        MaterialButton buttonDeny = view.findViewById(R.id.button_deny);

        RoleNavRouter.bindBottomNav(view, this, RoleDestination.PASSES);
        textName.setText(name);
        textMeta.setText(getString(R.string.verification_meta, role, host));
        textGate.setText(gate);
        textEntered.setText(entered);

        buttonApprove.setOnClickListener(v -> {
            if (!isAdded()) {
                return;
            }
            Snackbar.make(requireView(), R.string.route_to_dashboard_for_admission, Snackbar.LENGTH_LONG).show();
            if (requireActivity() instanceof NavigationHost) {
                ((NavigationHost) requireActivity()).showFragment(DashboardFragment.newInstance(), true);
            }
        });

        buttonDeny.setOnClickListener(v -> {
            String reason = inputReason.getText() == null ? "" : inputReason.getText().toString().trim();
            if (reason.isEmpty()) {
                Snackbar.make(requireView(), R.string.error_reason_required, Snackbar.LENGTH_SHORT).show();
                return;
            }
            repository.denyRequest(requestId, reason, (success, message, exception) -> {
                if (!isAdded()) {
                    return;
                }
                requireActivity().runOnUiThread(() -> {
                    Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG).show();
                    if (success && requireActivity() instanceof NavigationHost) {
                        requireActivity().getSupportFragmentManager().popBackStack();
                    }
                });
            });
        });
    }

    @NonNull
    private String safeArg(@NonNull Bundle args, @NonNull String key) {
        String value = args.getString(key);
        return value == null ? "" : value;
    }
}
