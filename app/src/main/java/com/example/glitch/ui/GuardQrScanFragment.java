package com.example.glitch.ui;

import android.net.Uri;
import android.os.Bundle;
import android.text.InputFilter;
import android.util.Log;
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
import com.example.glitch.data.GuestPassRepository;
import com.example.glitch.data.RepositoryProvider;
import com.example.glitch.model.AuditEventType;
import com.example.glitch.model.GatePolicy;
import com.example.glitch.model.GuestPass;
import com.example.glitch.model.GuestPassStatusRules;
import com.example.glitch.model.GuardPendingDecision;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Guard QR verification screen for single-use guest pass checks.
 */
public class GuardQrScanFragment extends Fragment {
    private static final String ARG_STATUS_MESSAGE = "arg_status_message";
    private static final Pattern PASS_CODE_PATTERN = Pattern.compile("\\b([A-Z0-9]{8})\\b");
    private static final String TAG = "GuardScanFlow";

    private GuestPassRepository guestPassRepository;
    private com.example.glitch.data.InterventionRepository interventionRepository;
    private AlertRepository alertRepository;
    private GuardPendingDecisionStore pendingDecisionStore;
    private AuditEventLogger auditEventLogger;
    private TextView textResultView;
    private final ActivityResultLauncher<ScanOptions> barcodeLauncher =
            registerForActivityResult(new ScanContract(), result -> {
                if (result == null) return;
                String contents = result.getContents();
                Log.d(TAG, "barcode result received; hasContents=" + (contents != null && !contents.isEmpty()));
                if (contents == null || contents.isEmpty()) {
                    View root = getView();
                    if (root != null) {
                        Snackbar.make(root, R.string.error_verify_credential, Snackbar.LENGTH_SHORT).show();
                    }
                    Log.d(TAG, "barcode contents empty; stopping");
                    return;
                }
                if (textResultView == null) {
                    Log.d(TAG, "barcode callback before textResultView bound; ignoring");
                    return;
                }
                verifyPassOnly(contents, "QR_SCAN", textResultView);
            });

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
        interventionRepository = RepositoryProvider.getInterventionRepository();
        alertRepository = RepositoryProvider.getAlertRepository();
        pendingDecisionStore = new GuardPendingDecisionStore(requireContext());
        auditEventLogger = new AuditEventLogger();

        textResultView = view.findViewById(R.id.text_pass_result);
        showStatusMessageFromArgs(textResultView);

        // 2. Setup Manual Validation
        TextInputEditText inputPassCode = view.findViewById(R.id.input_pass_code);
        MaterialButton buttonValidate = view.findViewById(R.id.button_validate_pass);
        inputPassCode.setFilters(new InputFilter[]{
                new InputFilter.AllCaps(),
                new InputFilter.LengthFilter(8)
        });
        RoleNavRouter.bindBottomNav(view, this, RoleDestination.SCAN);
        GuardLanguageUiBinder.bind(view, this);

        buttonValidate.setOnClickListener(v -> {
            String passCode = read(inputPassCode);
            if (passCode.isEmpty()) {
                textResultView.setText(R.string.error_pass_code_required);
                return;
            }
            verifyPassOnly(passCode, "PASS_CODE", textResultView);
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
            if (reopenPendingDecisionIfAny(textResultView)) {
                return;
            }
            ScanOptions options = new ScanOptions();
            options.setBeepEnabled(true);
            options.setOrientationLocked(false);
            options.setPrompt(getString(R.string.guard_qr_prompt_camera));
            barcodeLauncher.launch(options);
        });

        reopenPendingDecisionIfAny(textResultView);
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
            Log.d(TAG, "reopenPendingDecisionIfAny: guard uid empty");
            return false;
        }
        GuardPendingDecision pendingDecision = pendingDecisionStore.getForGuard(guardUid);
        if (pendingDecision == null) {
            Log.d(TAG, "reopenPendingDecisionIfAny: no saved pending decision for guard=" + guardUid);
            return false;
        }
        Log.d(TAG, "reopenPendingDecisionIfAny: reopening pending for pass=" + pendingDecision.getPassCode());
        if (textResult != null) {
            textResult.setText(R.string.guard_pending_existing_reopened);
        }
        navigateToPendingDecisionScreen(pendingDecision);
        return true;
    }

    private void navigateToPendingDecisionScreen(@Nullable GuardPendingDecision decision) {
        if (!isAdded() || !(requireActivity() instanceof NavigationHost)) {
            Log.d(TAG, "navigateToPendingDecisionScreen skipped: fragment not added or host missing");
            return;
        }
        Log.d(TAG, "navigateToPendingDecisionScreen: pass=" + (decision == null ? "<store>" : decision.getPassCode()));
        if (decision == null) {
            ((NavigationHost) requireActivity()).showFragment(GuardPendingDecisionFragment.newInstance(), false);
            return;
        }
        ((NavigationHost) requireActivity()).showFragment(
                GuardPendingDecisionFragment.newInstance(decision),
                false
        );
    }

    private void verifyPassOnly(
            @NonNull String rawCode,
            @NonNull String verificationMethod,
            @NonNull TextView textResult
    ) {
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
        String passCode = normalizeScannedPassCode(rawCode, verificationMethod);
        Log.d(TAG, "verifyPassAfterShiftCheck: method=" + verificationMethod + " raw=" + rawCode + " normalized=" + passCode);
        if (passCode.isEmpty()) {
            textResult.setText(R.string.pass_not_found);
            Log.d(TAG, "verifyPassAfterShiftCheck: normalized pass code empty");
            return;
        }
        guestPassRepository.findPassByCode(passCode, new GuestPassRepository.PassLookupListener() {
            @Override
            public void onData(@Nullable GuestPass pass) {
                Log.d(TAG, "findPassByCode.onData: isAdded=" + isAdded()
                        + " activityNull=" + (getActivity() == null)
                        + " passFound=" + (pass != null));
                if (getActivity() == null) {
                    Log.d(TAG, "findPassByCode.onData: dropping result because activity is null");
                    return;
                }
                getActivity().runOnUiThread(() -> handlePassLookupResult(pass, verificationMethod, textResult));
            }

            @Override
            public void onError(@NonNull Exception exception) {
                Log.e(TAG, "findPassByCode.onError: isAdded=" + isAdded()
                        + " activityNull=" + (getActivity() == null), exception);
                if (getActivity() == null) {
                    return;
                }
                getActivity().runOnUiThread(() -> textResult.setText(R.string.error_verify_credential));
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
            Log.d(TAG, "handlePassLookupResult: pass is null");
            return;
        }
        Log.d(TAG, "handlePassLookupResult: id=" + pass.getId()
                + " passCode=" + pass.getPassCode()
                + " status=" + pass.getStatus()
                + " entryRequestId=" + pass.getEntryRequestId());
        if ("expired".equalsIgnoreCase(pass.getStatus())) {
            textResult.setText(R.string.pass_expired);
            Log.d(TAG, "handlePassLookupResult: blocked expired");
            return;
        }

        if (pass.getEntryRequestId().trim().isEmpty()) {
            textResult.setText(R.string.pass_orphan_invalid);
            Log.d(TAG, "handlePassLookupResult: blocked missing entryRequestId");
            return;
        }

        String status = pass.getStatus().trim().toLowerCase();
        if ("reported".equals(status)) {
            textResult.setText(R.string.guard_pass_reported_contact_office);
            Log.d(TAG, "handlePassLookupResult: blocked reported");
            return;
        }
        if ("used".equals(status) || "overdue".equals(status)) {
            Log.d(TAG, "handlePassLookupResult: routing to exit decision");
            openExitDecision(pass);
            return;
        }
        if ("exited".equals(status)) {
            textResult.setText(R.string.guard_pass_already_exited);
            Log.d(TAG, "handlePassLookupResult: blocked exited");
            return;
        }
        if (!"active".equals(status)) {
            textResult.setText(R.string.pass_not_active);
            Log.d(TAG, "handlePassLookupResult: blocked non-active status=" + status);
            return;
        }

        // if (!GuestPassTimePolicy.isEntryWindowOpenNow()) {
        //     textResult.setText(R.string.pass_not_valid_time_window);
        //     return;
        // }

        if (GuestPassStatusRules.isTimeExpiredActive(pass)) {
            textResult.setText(R.string.pass_expired);
            Log.d(TAG, "handlePassLookupResult: blocked time-expired active");
            return;
        }

        interventionRepository.isGuestBanned(pass.getGuestIdNumber(), (banned, record, message) -> {
            if (!isAdded()) {
                return;
            }
            Log.d(TAG, "banCheck: banned=" + banned + " message=" + message);
            requireActivity().runOnUiThread(() -> {
                if (banned) {
                    createBannedGuestScanAlert(pass);
                    textResult.setText(R.string.pass_guest_banned);
                    Log.d(TAG, "banCheck: blocked banned guest");
                    return;
                }
                if (message.startsWith("ERROR_")) {
                    // Do not block guard flow on transient banned-list lookup failures.
                    Snackbar.make(requireView(), R.string.error_banned_check_unavailable, Snackbar.LENGTH_SHORT).show();
                    Log.d(TAG, "banCheck: error path, continuing anyway");
                }
                persistPendingDecision(pass, verificationMethod, textResult);
            });
        });
    }

    private void openExitDecision(@NonNull GuestPass pass) {
        if (!isAdded() || !(requireActivity() instanceof NavigationHost)) {
            return;
        }
        ((NavigationHost) requireActivity()).showFragment(
                GuardExitDecisionFragment.newInstance(
                        pass.getEntryRequestId(),
                        pass.getGuestName(),
                        pass.getGuestIdNumber(),
                        pass.getGuestPhone(),
                        pass.getStatus(),
                        pass.getPassCode()
                ),
                true
        );
    }

    private void persistPendingDecision(
            @NonNull GuestPass pass,
            @NonNull String verificationMethod,
            @NonNull TextView textResult
    ) {

        String guardUid = currentGuardUid();
        if (guardUid.trim().isEmpty()) {
            textResult.setText(R.string.error_verify_credential);
            Log.d(TAG, "persistPendingDecision: guard uid empty");
            return;
        }
        GuardPendingDecision decision = GuardPendingDecision.fromPass(guardUid, pass, verificationMethod);
        pendingDecisionStore.save(decision);
        Log.d(TAG, "persistPendingDecision: saved decision pass=" + decision.getPassCode()
                + " entryRequestId=" + decision.getEntryRequestId());
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
        if (!pendingDecisionStore.hasForGuard(guardUid)) {
            textResult.setText(R.string.error_verify_credential);
            Log.d(TAG, "persistPendingDecision: store check failed after save");
            return;
        }
        Log.d(TAG, "persistPendingDecision: navigating to pending screen");
        navigateToPendingDecisionScreen(decision);
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

    @NonNull
    private String normalizeScannedPassCode(@NonNull String rawCode, @NonNull String verificationMethod) {
        String trimmed = rawCode.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        if (!"QR_SCAN".equalsIgnoreCase(verificationMethod)) {
            return trimmed.toUpperCase(Locale.US);
        }

        String upper = trimmed.toUpperCase(Locale.US);

        // 1) Direct raw pass code.
        Matcher direct = PASS_CODE_PATTERN.matcher(upper);
        if (direct.matches()) {
            return direct.group(1);
        }

        // 2) URI payloads: ...?passCode=ABCDEFGH or path/.../ABCDEFGH
        try {
            Uri uri = Uri.parse(trimmed);
            String queryPassCode = firstNonEmpty(
                    uri.getQueryParameter("passCode"),
                    uri.getQueryParameter("pass_code"),
                    uri.getQueryParameter("code")
            );
            if (!queryPassCode.isEmpty()) {
                Matcher queryMatcher = PASS_CODE_PATTERN.matcher(queryPassCode.toUpperCase(Locale.US).trim());
                if (queryMatcher.matches()) {
                    return queryMatcher.group(1);
                }
            }
            String lastSegment = uri.getLastPathSegment();
            if (lastSegment != null) {
                Matcher pathMatcher = PASS_CODE_PATTERN.matcher(lastSegment.toUpperCase(Locale.US).trim());
                if (pathMatcher.matches()) {
                    return pathMatcher.group(1);
                }
            }
        } catch (Exception ignored) {
            // fall through to generic token extraction.
        }

        // 3) Prefix payloads: PASS_CODE: XXXXXXXX / PASSCODE=XXXXXXXX
        String normalized = upper
                .replace("PASS_CODE", " ")
                .replace("PASSCODE", " ")
                .replace("PASS CODE", " ")
                .replace("CODE", " ")
                .replace(":", " ")
                .replace("=", " ")
                .replace("/", " ")
                .replace("\\", " ");
        Matcher tokenMatcher = PASS_CODE_PATTERN.matcher(normalized);
        if (tokenMatcher.find()) {
            return tokenMatcher.group(1);
        }

        Log.d(TAG, "normalizeScannedPassCode: unable to extract pass code from QR payload");
        return "";
    }

    @NonNull
    private String firstNonEmpty(@Nullable String first, @Nullable String second, @Nullable String third) {
        if (first != null && !first.trim().isEmpty()) return first.trim();
        if (second != null && !second.trim().isEmpty()) return second.trim();
        if (third != null && !third.trim().isEmpty()) return third.trim();
        return "";
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
        textResultView = null;
        super.onDestroyView();
    }
}
