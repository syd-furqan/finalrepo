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
import com.example.glitch.model.UserProfile;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

/**
 * Faculty access-request form (US-06) for sponsoring guest entry.
 * Pattern: Form fragment that creates guard-visible entry requests.
 * Known issue: request scheduling fields are omitted in v1 for simpler checkpoint flow.
 */
public class FacultyAccessRequestFragment extends Fragment {
    private EntryRequestRepository repository;

    @NonNull
    public static FacultyAccessRequestFragment newInstance() {
        return new FacultyAccessRequestFragment();
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_faculty_access_request, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = RepositoryProvider.getRepository();

        TextInputEditText inputGuestName = view.findViewById(R.id.input_guest_name);
        TextInputEditText inputGuestId = view.findViewById(R.id.input_guest_id);
        TextInputEditText inputGate = view.findViewById(R.id.input_gate_label);
        MaterialButton buttonSubmit = view.findViewById(R.id.button_submit_request);
        RoleNavRouter.bindBottomNav(view, this, RoleDestination.DASHBOARD);

        buttonSubmit.setOnClickListener(v -> {
            String guestName = read(inputGuestName);
            String guestId = read(inputGuestId);
            String gate = read(inputGate);
            UserProfile profile = AuthUiGuard.requireProfile(this);

            if (guestName.isEmpty() || guestId.isEmpty() || gate.isEmpty() || profile == null) {
                Snackbar.make(requireView(), R.string.error_fill_required_fields, Snackbar.LENGTH_SHORT).show();
                return;
            }

            // UPDATE: Added 'null' as the 7th argument for the missing expiresAt timestamp
            repository.createEntryRequest(
                    profile.getUid(),
                    "faculty",
                    guestName,
                    guestId,
                    gate,
                    profile.getDisplayName(),
                    null, // <--- Add this 'null' right here!
                    (success, message, exception) -> {
                        if (!isAdded()) {
                            return;
                        }
                        requireActivity().runOnUiThread(() ->
                                Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG).show());
                    }
            );
        });
    }

    @NonNull
    private String read(@NonNull TextInputEditText input) {
        CharSequence value = input.getText();
        return value == null ? "" : value.toString().trim();
    }
}