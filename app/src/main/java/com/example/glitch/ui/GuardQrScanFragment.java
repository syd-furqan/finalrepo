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
import com.example.glitch.data.GuestPassRepository;
import com.example.glitch.data.RepositoryProvider;
import com.example.glitch.model.GuestPass;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
import androidx.activity.result.ActivityResultLauncher;

import java.util.Date;

/**
 * Guard QR verification screen for single-use guest pass checks.
 */
public class GuardQrScanFragment extends Fragment {
    private GuestPassRepository guestPassRepository;
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
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_guard_qr_scan, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        guestPassRepository = RepositoryProvider.getGuestPassRepository();
        verificationRulesRepository = RepositoryProvider.getVerificationRulesRepository();
        firestore = FirebaseFirestore.getInstance();

        // 1. Register barcode scanner activity result
        barcodeLauncher = registerForActivityResult(new ScanContract(), result -> {
            if (result == null) return;
            String contents = result.getContents();
            TextView textResult = view.findViewById(R.id.text_pass_result);
            if (contents == null || contents.isEmpty()) {
                Snackbar.make(requireView(), R.string.error_verify_credential, Snackbar.LENGTH_SHORT).show();
                return;
            }
            verifyPassOnly(contents, textResult);
        });

        // 2. Setup Manual Validation
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
            verifyPassOnly(passCode, textResult);
        });

        // 3. Setup QR Scan Button
        MaterialButton buttonScan = view.findViewById(R.id.button_scan_qr);
        buttonScan.setOnClickListener(v -> {
            ScanOptions options = new ScanOptions();
            options.setBeepEnabled(true);
            options.setOrientationLocked(false);
            options.setPrompt("Point camera at QR code");
            barcodeLauncher.launch(options);
        });

        // 4. Listen to Verification Rules
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
    }

    private void recordFailedAttempt(@NonNull String identifier) {
        DocumentReference counterRef = firestore.collection("failed_counts").document(identifier);
        counterRef.set(new java.util.HashMap<String, Object>() {{
                    put("count", FieldValue.increment(1));
                    put("lastFailedAt", FieldValue.serverTimestamp());
                }}, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(v -> counterRef.get().addOnSuccessListener(snapshot -> {
                    long count = snapshot.getLong("count") == null ? 0L : snapshot.getLong("count");
                    int threshold = Math.max(1, currentRules.getAlertThreshold());
                    if (count >= threshold) {
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

    private void verifyPassOnly(@NonNull String rawCode, @NonNull TextView textResult) {
        String passCode = rawCode.trim().toUpperCase();
        guestPassRepository.findPassByCode(passCode, new GuestPassRepository.PassLookupListener() {
            @Override
            public void onData(@Nullable GuestPass pass) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() ->
                        handlePassLookupResult(pass, passCode, textResult));
            }

            @Override
            public void onError(@NonNull Exception exception) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> textResult.setText(R.string.error_verify_credential));
                recordFailedAttempt(passCode);
            }
        });
    }

    private void handlePassLookupResult(
            @Nullable GuestPass pass,
            @NonNull String scannedCode,
            @NonNull TextView textResult
    ) {
        if (pass == null) {
            textResult.setText(R.string.pass_not_found);
            recordFailedAttempt(scannedCode);
            return;
        }
        if ("expired".equalsIgnoreCase(pass.getStatus())) {
            textResult.setText(R.string.pass_expired);
            recordFailedAttempt(scannedCode);
            return;
        }
        if (!"active".equalsIgnoreCase(pass.getStatus())) {
            textResult.setText(R.string.pass_not_active);
            recordFailedAttempt(scannedCode);
            return;
        }

        if (pass.getExpiresAt() != null && pass.getExpiresAt().toDate().before(new Date())) {
            textResult.setText(R.string.pass_expired);
            recordFailedAttempt(scannedCode);
            return;
        }

        if (pass.getEntryRequestId().trim().isEmpty()) {
            textResult.setText(R.string.pass_orphan_invalid);
            recordFailedAttempt(scannedCode);
            return;
        }

        textResult.setText(getString(R.string.pass_verified_go_dashboard, pass.getEntryRequestId()));
        Snackbar.make(requireView(), R.string.route_to_dashboard_for_admission, Snackbar.LENGTH_SHORT).show();
        if (requireActivity() instanceof NavigationHost) {
            ((NavigationHost) requireActivity()).showFragment(DashboardFragment.newInstance(), true);
        }
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
