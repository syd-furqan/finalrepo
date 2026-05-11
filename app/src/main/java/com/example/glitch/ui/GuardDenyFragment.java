package com.example.glitch.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.glitch.R;
import com.example.glitch.data.EntryRequestRepository;
import com.example.glitch.data.RepositoryProvider;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

/**
 * Dedicated guard incident screen for denying/flagging a request with reason.
 * Pattern: Simple action form fragment bound to repository deny operation.
 * Known issue: request lookup assistance is currently manual by request id.
 */
public class GuardDenyFragment extends Fragment {
    private EntryRequestRepository repository;

    @NonNull
    public static GuardDenyFragment newInstance() {
        return new GuardDenyFragment();
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_guard_deny, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = RepositoryProvider.getRepository();
        TextInputEditText inputRequestId = view.findViewById(R.id.input_request_id);
        TextInputEditText inputReason = view.findViewById(R.id.input_denial_reason);
        MaterialButton buttonSubmit = view.findViewById(R.id.button_submit_denial);
        RoleNavRouter.bindBottomNav(view, this, RoleDestination.GUARD_DENY);
        GuardLanguageUiBinder.bind(view, this);
        buttonSubmit.setOnClickListener(v -> {
            String requestId = read(inputRequestId);
            String reason = read(inputReason);
            if (requestId.isEmpty() || reason.isEmpty()) {
                Snackbar.make(requireView(), R.string.error_reason_required, Snackbar.LENGTH_SHORT).show();
                return;
            }
            repository.denyRequest(requestId, reason, (success, message, exception) -> {
                if (!isAdded()) {
                    return;
                }
                requireActivity().runOnUiThread(() ->
                        Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG).show());
            });
        });
    }

    @NonNull
    private String read(@NonNull TextInputEditText editText) {
        CharSequence value = editText.getText();
        return value == null ? "" : value.toString().trim();
    }
}
