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
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
import com.journeyapps.barcodescanner.ScanIntentResult;
import androidx.activity.result.ActivityResultLauncher;

import java.util.Date;

/**
 * Guard QR verification screen for single-use guest pass checks.
 * Pattern: Verification form fragment using pass-code lookup against Firestore.
 * Known issue: scanner camera integration is represented as pass-code input in v1.
 */
public class GuardQrScanFragment extends Fragment {
    private GuestPassRepository guestPassRepository;
    private EntryRequestRepository entryRequestRepository;
    private com.example.glitch.data.VerificationRulesRepository verificationRulesRepository;
    private com.example.glitch.model.VerificationRules currentRules = com.example.glitch.model.VerificationRules.defaultRules();
    private FirebaseFirestore firestore;
    private ActivityResultLauncher<ScanOptions> barcodeLauncher;

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
        verificationRulesRepository = RepositoryProvider.getVerificationRulesRepository();
        firestore = FirebaseFirestore.getInstance();

        // Register barcode scanner activity result
        barcodeLauncher = registerForActivityResult(new ScanContract(), result -> {
            if (result == null) return;
            String contents = result.getContents();
            if (contents == null || contents.isEmpty()) {
                Snackbar.make(requireView(), R.string.error_verify_credential, Snackbar.LENGTH_SHORT).show();
                return;
            }
            // Treat scanned contents as pass code and run the same lookup flow
            guestPassRepository.findPassByCode(contents, new GuestPassRepository.PassLookupListener() {
                @Override
                public void onData(@Nullable GuestPass pass) {
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() -> {
                        TextView textResult = view.findViewById(R.id.text_pass_result);
                        handlePassLookupResult(pass, textResult);
                    });
                }

                @Override
                public void onError(@NonNull Exception exception) {
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() -> {
                        TextView textResult = view.findViewById(R.id.text_pass_result);
                        textResult.setText(R.string.error_verify_credential);
                    });
                }
            });
        });

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
                    // record a failed attempt for this identifier
                    recordFailedAttempt(passCode);
            });
        });
    }

        MaterialButton buttonScan = view.findViewById(R.id.button_scan_qr);
        buttonScan.setOnClickListener(v -> {
            ScanOptions options = new ScanOptions();
            options.setBeepEnabled(true);
            options.setOrientationLocked(false);
            options.setPrompt("Point camera at QR code");
            barcodeLauncher.launch(options);
        });

        // Start listening to verification rules so we can use alert threshold
        verificationRulesRepository.listenRules(new com.example.glitch.data.VerificationRulesRepository.RulesListener() {
            @Override
            public void onData(@NonNull com.example.glitch.model.VerificationRules rules) {
                currentRules = rules;
            }

            @Override
            public void onError(@NonNull Exception exception) {
                // ignore; keep defaults
            }
        });


    private void recordFailedAttempt(@NonNull String identifier) {
        // Increment a counter document and create/update an alert when threshold reached
        DocumentReference counterRef = firestore.collection("failed_counts").document(identifier);
        counterRef.set(new java.util.HashMap<String, Object>() {{ put("count", FieldValue.increment(1)); put("lastFailedAt", FieldValue.serverTimestamp()); }}, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(v -> counterRef.get().addOnSuccessListener(snapshot -> {
                    long count = snapshot.getLong("count") == null ? 0L : snapshot.getLong("count");
                    int threshold = Math.max(1, currentRules.getAlertThreshold());
                    if (count >= threshold) {
                        // create or update alert document for this identifier
                        DocumentReference alertRef = firestore.collection("alerts").document(identifier);
                        java.util.Map<String, Object> payload = new java.util.HashMap<>();
                        payload.put("identifier", identifier);
                        payload.put("failCount", (int) count);
                        payload.put("severity", count >= (threshold * 2) ? "HIGH" : "MEDIUM");
                        payload.put("message", "Repeated failed verification attempts");
                        payload.put("lastFailedAt", FieldValue.serverTimestamp());
                        payload.put("createdAt", FieldValue.serverTimestamp());
                        alertRef.set(payload, com.google.firebase.firestore.SetOptions.merge());
                    }
                }));
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (verificationRulesRepository != null) verificationRulesRepository.removeListeners();
    }
}
