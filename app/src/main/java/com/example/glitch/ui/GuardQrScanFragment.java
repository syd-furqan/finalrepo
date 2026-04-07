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
import com.example.glitch.data.GuestPassRepository;
import com.example.glitch.data.RepositoryProvider;
import com.example.glitch.model.GuestPass;
import com.example.glitch.model.UserProfile;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Date;

/**
 * Guard QR verification screen for single-use guest pass checks.
 * Pattern: Verification form fragment using pass-code lookup against Firestore.
 * Known issue: scanner camera integration is represented as pass-code input in v1.
 */
public class GuardQrScanFragment extends Fragment {
    private GuestPassRepository guestPassRepository;
    private EntryRequestRepository entryRequestRepository;

    @NonNull
    public static GuardQrScanFragment newInstance() {
        return new GuardQrScanFragment();
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_guard_qr_scan, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        guestPassRepository = RepositoryProvider.getGuestPassRepository();
        entryRequestRepository = RepositoryProvider.getRepository();

        TextInputEditText inputPassCode = view.findViewById(R.id.input_pass_code);
        MaterialButton buttonValidate = view.findViewById(R.id.button_validate_pass);
        TextView textResult = view.findViewById(R.id.text_pass_result);
        RoleNavRouter.bindBottomNav(view, this, RoleDestination.VEHICLES);

        buttonValidate.setOnClickListener(v -> {
            String passCode = read(inputPassCode);
            if (passCode.isEmpty()) {
                textResult.setText(R.string.error_pass_code_required);
                return;
            }
            guestPassRepository.findPassByCode(passCode, new GuestPassRepository.PassLookupListener() {
                @Override
                public void onData(@Nullable GuestPass pass) {
                    if (!isAdded()) {
                        return;
                    }
                    requireActivity().runOnUiThread(() -> handlePassLookupResult(pass, textResult));
                }

                @Override
                public void onError(@NonNull Exception exception) {
                    if (!isAdded()) {
                        return;
                    }
                    requireActivity().runOnUiThread(() ->
                            textResult.setText(R.string.error_verify_credential));
                }
            });
        });
    }

    private void handlePassLookupResult(@Nullable GuestPass pass, @NonNull TextView textResult) {
        if (pass == null) {
            textResult.setText(R.string.pass_not_found);
            return;
        }
        if (!"active".equalsIgnoreCase(pass.getStatus())) {
            textResult.setText(R.string.pass_not_active);
            return;
        }

        if (pass.getExpiresAt() != null && pass.getExpiresAt().toDate().before(new Date())) {
            textResult.setText(R.string.pass_expired);
            return;
        }

        UserProfile profile = AuthUiGuard.requireProfile(this);
        if (profile == null) {
            return;
        }
        String guardUid = profile.getUid();
        String hostInfo = pass.getSponsorName() + " (" + pass.getSponsorEmail() + ")";

        entryRequestRepository.createEntryRequest(
                guardUid,
                "guard",
                pass.getGuestName(),
                pass.getGuestIdNumber(),
                "QR Gate",
                hostInfo,
                pass.getExpiresAt(),
                (success, message, exception) -> {
                    if (!isAdded()) {
                        return;
                    }
                    requireActivity().runOnUiThread(() -> textResult.setText(message));
                }
        );
    }

    @NonNull
    private String read(@NonNull TextInputEditText input) {
        CharSequence value = input.getText();
        return value == null ? "" : value.toString().trim();
    }
}
