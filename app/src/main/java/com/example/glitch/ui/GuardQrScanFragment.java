package com.example.glitch.ui;

import android.os.Bundle;
import android.text.InputFilter;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.glitch.R;
import com.example.glitch.auth.SessionManager;
import com.example.glitch.data.AuditEventLogger;
import com.example.glitch.data.GuestPassRepository;
import com.example.glitch.data.RepositoryProvider;
import com.example.glitch.model.AuditEventType;
import com.example.glitch.model.GatePolicy;
import com.example.glitch.model.GuestPass;
import com.example.glitch.model.GuestPassStatusRules;
import com.example.glitch.model.GuestPassTimePolicy;
import com.example.glitch.model.GuardPendingDecision;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import java.util.HashMap;
import java.util.Map;

/**
 * Guard QR verification screen for single-use guest pass checks.
 */
public class GuardQrScanFragment extends Fragment {
    private static final String ARG_STATUS_MESSAGE = "arg_status_message";

    private GuestPassRepository guestPassRepository;
    private com.example.glitch.data.VerificationRulesRepository verificationRulesRepository;
    private com.example.glitch.model.VerificationRules currentRules = com.example.glitch.model.VerificationRules.defaultRules();
    private FirebaseFirestore firestore;
    private GuardPendingDecisionStore pendingDecisionStore;
    private AuditEventLogger auditEventLogger;
    private ActivityResultLauncher<ScanOptions> barcodeLauncher;

    @NonNull
    public static GuardQrScanFragment newInstance() {
        return newInstance(null);
    }

    @NonNull
    public static GuardQrScanFragment newInstance(@Nullable String statusMessage) {
        GuardQrScanFragment fragment = new GuardQrScanFragment();
        Bundle args = new Bundle();
        args.putString(ARG_STATUS_MESSAGE, statusMessage == null ? "" : statusMessage);
        fragment.setArguments(args);
        return fragment;
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
        pendingDecisionStore = new GuardPendingDecisionStore(requireContext());
        firestore = FirebaseFirestore.getInstance();
        auditEventLogger = new AuditEventLogger();

        TextView textResult = view.findViewById(R.id.text_pass_result);
        showStatusMessageFromArgs(textResult);

        // 1. Register barcode scanner activity result
        barcodeLauncher = registerForActivityResult(new ScanContract(), result -> {
            if (result == null) return;
            String contents = result.getContents();
            if (contents == null || contents.isEmpty()) {
                Snackbar.make(requireView(), R.string.error_verify_credential, Snackbar.LENGTH_SHORT).show();
                return;
            }
            verifyPassOnly(contents, "QR_SCAN", textResult);
        });

        // 2. Setup Manual Validation
        TextInputEditText inputPassCode = view.findViewById(R.id.input_pass_code);
        MaterialButton buttonValidate = view.findViewById(R.id.button_validate_pass);
        inputPassCode.setFilters(new InputFilter[]{
                new InputFilter.AllCaps(),
                new InputFilter.LengthFilter(8)
        });
        RoleNavRouter.bindBottomNav(view, this, RoleDestination.VEHICLES);

        buttonValidate.setOnClickListener(v -> {
            String passCode = read(inputPassCode);
            if (passCode.isEmpty()) {
                textResult.setText(R.string.error_pass_code_required);
                return;
            }
            verifyPassOnly(passCode, "PASS_CODE", textResult);
        });
        inputPassCode.setOnEditorActionListener((textView, actionId, event) -> {
            boolean isDoneAction = actionId == EditorInfo.IME_ACTION_DONE
                    || actionId == EditorInfo.IME_ACTION_GO;
            boolean isEnterKey = event != null
                    && event.getAction() == KeyEvent.ACTION_DOWN
                    && event.getKeyCode() == KeyEvent.KEYCODE_ENTER;
            if (!isDoneAction && !isEnterKey) {
                return false;
            }
            buttonValidate.performClick();
            return true;
        });

        // 3. Setup QR Scan Button
        MaterialButton buttonScan = view.findViewById(R.id.button_scan_qr);
        buttonScan.setOnClickListener(v -> {
            if (reopenPendingDecisionIfAny(textResult)) {
                return;
            }
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

        reopenPendingDecisionIfAny(textResult);
    }

    @Override
    public void onResume() {
        super.onResume();
        View root = getView();
        if (root == null) {
            return;
        }
        TextView textResult = root.findViewById(R.id.text_pass_result);
        reopenPendingDecisionIfAny(textResult);
    }

    private void showStatusMessageFromArgs(@NonNull TextView textResult) {
        Bundle args = getArguments();
        if (args == null) {
            return;
        }
        String message = args.getString(ARG_STATUS_MESSAGE, "").trim();
        if (message.isEmpty()) {
            return;
        }
        textResult.setText(message);
    }

    private boolean reopenPendingDecisionIfAny(@Nullable TextView textResult) {
        String guardUid = currentGuardUid();
        if (guardUid.isEmpty()) {
            return false;
        }
        GuardPendingDecision pendingDecision = pendingDecisionStore.getForGuard(guardUid);
        if (pendingDecision == null) {
            return false;
        }
        if (textResult != null) {
            textResult.setText(R.string.guard_pending_existing_reopened);
        }
        navigateToPendingDecisionScreen();
        return true;
    }

    private void navigateToPendingDecisionScreen() {
        if (!isAdded() || !(requireActivity() instanceof NavigationHost)) {
            return;
        }
        ((NavigationHost) requireActivity()).showFragment(GuardPendingDecisionFragment.newInstance(), false);
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

    private void verifyPassOnly(
            @NonNull String rawCode,
            @NonNull String verificationMethod,
            @NonNull TextView textResult
    ) {
        if (reopenPendingDecisionIfAny(textResult)) {
            return;
        }
        if (!GuestPassTimePolicy.isEntryWindowOpenNow()) {
            textResult.setText(R.string.pass_not_valid_time_window);
            return;
        }
        String passCode = rawCode.trim().toUpperCase();
        guestPassRepository.findPassByCode(passCode, new GuestPassRepository.PassLookupListener() {
            @Override
            public void onData(@Nullable GuestPass pass) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() ->
                        handlePassLookupResult(pass, passCode, verificationMethod, textResult));
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
            @NonNull String verificationMethod,
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

        if (GuestPassStatusRules.isTimeExpiredActive(pass)) {
            textResult.setText(R.string.pass_expired);
            recordFailedAttempt(scannedCode);
            return;
        }

        if (pass.getEntryRequestId().trim().isEmpty()) {
            textResult.setText(R.string.pass_orphan_invalid);
            recordFailedAttempt(scannedCode);
            return;
        }

        String guardUid = currentGuardUid();
        if (guardUid.trim().isEmpty()) {
            textResult.setText(R.string.error_verify_credential);
            return;
        }
        GuardPendingDecision decision = GuardPendingDecision.fromPass(guardUid, pass, verificationMethod);
        pendingDecisionStore.save(decision);
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("passCode", pass.getPassCode());
        metadata.put("verificationMethod", verificationMethod);
        metadata.put("guestName", pass.getGuestName());
        metadata.put("hasVehicle", pass.hasVehicle());
        metadata.put("vehiclePlate", pass.getVehiclePlate());
        auditEventLogger.log(
                AuditEventType.PENDING_DECISION_CREATED,
                "guest_pass",
                pass.getId(),
                pass.getEntryRequestId(),
                guardUid,
                "guard",
                "Guard pending decision created from pass verification",
                "guard_scan",
                "pending",
                "awaiting_decision",
                pass.getGateLabel().trim().isEmpty() ? GatePolicy.STORED_VALUE : pass.getGateLabel(),
                metadata
        );
        navigateToPendingDecisionScreen();
    }

    @NonNull
    private String read(@NonNull TextInputEditText input) {
        CharSequence value = input.getText();
        return value == null ? "" : value.toString().trim();
    }

    @NonNull
    private String currentGuardUid() {
        if (SessionManager.getCurrentProfile() == null) {
            return "";
        }
        return SessionManager.getCurrentProfile().getUid();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (verificationRulesRepository != null) verificationRulesRepository.removeListeners();
    }
}
