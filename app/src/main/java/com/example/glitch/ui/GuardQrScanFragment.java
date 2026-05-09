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
import com.example.glitch.data.AdminAlertPayloadFactory;
import com.example.glitch.data.AlertRepository;
import com.example.glitch.data.AuditEventLogger;
import com.example.glitch.data.EntryRequestRepository;
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
    private EntryRequestRepository entryRequestRepository;
    private com.example.glitch.data.InterventionRepository interventionRepository;
    private AlertRepository alertRepository;
    private GuardPendingDecisionStore pendingDecisionStore;
    private AuditEventLogger auditEventLogger;
    private ActivityResultLauncher<ScanOptions> barcodeLauncher;
    private MaterialButton buttonLogExitFromScan;
    private GuestPass passReadyForExit;

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
        entryRequestRepository = RepositoryProvider.getRepository();
        interventionRepository = RepositoryProvider.getInterventionRepository();
        alertRepository = RepositoryProvider.getAlertRepository();
        pendingDecisionStore = new GuardPendingDecisionStore(requireContext());
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
        buttonLogExitFromScan = view.findViewById(R.id.button_log_exit_from_scan);
        buttonLogExitFromScan.setVisibility(View.GONE);
        buttonLogExitFromScan.setOnClickListener(v -> logExitForScannedPass(textResult));
        inputPassCode.setFilters(new InputFilter[]{
                new InputFilter.AllCaps(),
                new InputFilter.LengthFilter(8)
        });
        RoleNavRouter.bindBottomNav(view, this, RoleDestination.SCAN);

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

    private void verifyPassOnly(
            @NonNull String rawCode,
            @NonNull String verificationMethod,
            @NonNull TextView textResult
    ) {
        clearExitAction();
        if (reopenPendingDecisionIfAny(textResult)) {
            return;
        }
        verifyPassAfterShiftCheck(rawCode, verificationMethod, textResult);
    }

    private void verifyPassAfterShiftCheck(
            @NonNull String rawCode,
            @NonNull String verificationMethod,
            @NonNull TextView textResult
    ) {
        String passCode = rawCode.trim().toUpperCase();
        guestPassRepository.findPassByCode(passCode, new GuestPassRepository.PassLookupListener() {
            @Override
            public void onData(@Nullable GuestPass pass) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() ->
                        handlePassLookupResult(pass, verificationMethod, textResult));
            }

            @Override
            public void onError(@NonNull Exception exception) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> textResult.setText(R.string.error_verify_credential));
            }
        });
    }

    private void handlePassLookupResult(
            @Nullable GuestPass pass,
            @NonNull String verificationMethod,
            @NonNull TextView textResult
    ) {
        if (pass == null) {
            textResult.setText(R.string.pass_not_found);
            return;
        }
        if ("expired".equalsIgnoreCase(pass.getStatus())) {
            textResult.setText(R.string.pass_expired);
            return;
        }

        if (pass.getEntryRequestId().trim().isEmpty()) {
            textResult.setText(R.string.pass_orphan_invalid);
            return;
        }

        String status = pass.getStatus().trim().toLowerCase();
        if ("reported".equals(status)) {
            textResult.setText("This pass has been reported. Contact the security office.");
            return;
        }
        if ("used".equals(status) || "overdue".equals(status)) {
            showExitAction(pass, textResult);
            return;
        }
        if ("exited".equals(status)) {
            textResult.setText("This guest pass is already marked exited.");
            return;
        }
        if (!"active".equals(status)) {
            textResult.setText(R.string.pass_not_active);
            return;
        }

        if (!GuestPassTimePolicy.isEntryWindowOpenNow()) {
            textResult.setText(R.string.pass_not_valid_time_window);
            return;
        }

        if (GuestPassStatusRules.isTimeExpiredActive(pass)) {
            textResult.setText(R.string.pass_expired);
            return;
        }

        interventionRepository.isGuestBanned(pass.getGuestIdNumber(), (banned, record, message) -> {
            if (!isAdded()) {
                return;
            }
            requireActivity().runOnUiThread(() -> {
                if (banned) {
                    createBannedGuestScanAlert(pass);
                    textResult.setText(R.string.pass_guest_banned);
                    return;
                }
                if (message.startsWith("ERROR_")) {
                    textResult.setText(R.string.error_banned_check_unavailable);
                    return;
                }
                persistPendingDecision(pass, verificationMethod, textResult);
            });
        });
    }

    private void showExitAction(@NonNull GuestPass pass, @NonNull TextView textResult) {
        passReadyForExit = pass;
        if (buttonLogExitFromScan != null) {
            buttonLogExitFromScan.setVisibility(View.VISIBLE);
            buttonLogExitFromScan.setEnabled(true);
            buttonLogExitFromScan.setAlpha(1.0f);
        }
        String phoneText = pass.getGuestPhone().trim().isEmpty() ? "" : "\nPhone: " + pass.getGuestPhone();
        textResult.setText(
                "Ready to log exit for "
                        + pass.getGuestName()
                        + "\nCNIC: "
                        + pass.getGuestIdNumber()
                        + phoneText
                        + "\nStatus: "
                        + pass.getStatus().toUpperCase()
        );
    }

    private void clearExitAction() {
        passReadyForExit = null;
        if (buttonLogExitFromScan != null) {
            buttonLogExitFromScan.setVisibility(View.GONE);
            buttonLogExitFromScan.setEnabled(true);
            buttonLogExitFromScan.setAlpha(1.0f);
        }
    }

    private void logExitForScannedPass(@NonNull TextView textResult) {
        GuestPass pass = passReadyForExit;
        if (pass == null) {
            textResult.setText(R.string.error_verify_credential);
            clearExitAction();
            return;
        }
        String entryRequestId = pass.getEntryRequestId().trim();
        if (entryRequestId.isEmpty()) {
            textResult.setText(R.string.pass_orphan_invalid);
            clearExitAction();
            return;
        }
        buttonLogExitFromScan.setEnabled(false);
        buttonLogExitFromScan.setAlpha(0.5f);
        entryRequestRepository.logExit(entryRequestId, (entrySuccess, entryMessage, entryError) -> {
            if (!isAdded()) {
                return;
            }
            if (!entrySuccess) {
                requireActivity().runOnUiThread(() -> {
                    buttonLogExitFromScan.setEnabled(true);
                    buttonLogExitFromScan.setAlpha(1.0f);
                    textResult.setText(entryMessage);
                });
                return;
            }
            guestPassRepository.markPassExitedByEntryRequestId(entryRequestId, (passSuccess, passMessage, passError) -> {
                if (!isAdded()) {
                    return;
                }
                requireActivity().runOnUiThread(() -> {
                    if (passSuccess) {
                        textResult.setText("Guest exit logged for " + pass.getGuestName() + ".");
                        clearExitAction();
                    } else {
                        buttonLogExitFromScan.setEnabled(true);
                        buttonLogExitFromScan.setAlpha(1.0f);
                        textResult.setText("Exit logged, but pass update had an issue: " + passMessage);
                    }
                });
            });
        });
    }

    private void persistPendingDecision(
            @NonNull GuestPass pass,
            @NonNull String verificationMethod,
            @NonNull TextView textResult
    ) {

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
        metadata.put("guestPhone", pass.getGuestPhone());
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

    private void createBannedGuestScanAlert(@NonNull GuestPass pass) {
        if (alertRepository == null) {
            return;
        }
        String guardUid = currentGuardUid();
        String guardName = "";
        if (SessionManager.getCurrentProfile() != null) {
            guardName = SessionManager.getCurrentProfile().getDisplayName();
        }
        alertRepository.createAlert(
                null,
                AdminAlertPayloadFactory.bannedGuestScan(pass, guardUid, guardName),
                (success, message, exception) -> {
                    // Best-effort admin alert; the guard-facing scan result remains the primary UX.
                }
        );
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }
}
