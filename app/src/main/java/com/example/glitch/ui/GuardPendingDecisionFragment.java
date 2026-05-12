package com.example.glitch.ui;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.glitch.R;
import com.example.glitch.auth.SessionManager;
import com.example.glitch.data.AuditEventLogger;
import com.example.glitch.data.EntryRequestRepository;
import com.example.glitch.data.GuestPassRepository;
import com.example.glitch.data.RepositoryProvider;
import com.example.glitch.model.AuditEventType;
import com.example.glitch.model.CnicScanResult;
import com.example.glitch.model.GatePolicy;
import com.example.glitch.model.GuestIdentityPolicy;
import com.example.glitch.model.GuestPass;
import com.example.glitch.model.GuestPassStatusRules;
import com.example.glitch.model.GuardPendingDecision;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Full-screen, non-cancelable guard decision screen for unresolved scanned passes.
 */
public class GuardPendingDecisionFragment extends Fragment {
    private static final String ARG_PENDING_DECISION_JSON = "arg_pending_decision_json";
    private static final String TAG = "GuardPendingFlow";
    private EntryRequestRepository entryRequestRepository;
    private GuestPassRepository guestPassRepository;
    private GuardPendingDecisionStore pendingDecisionStore;
    private AuditEventLogger auditEventLogger;
    private GuardPendingDecision pendingDecision;
    private MaterialButton buttonAllow;
    private MaterialButton buttonDeny;
    private MaterialButton buttonClose;
    private MaterialCheckBox checkCnicVerified;
    private MaterialCheckBox checkVehicleVerified;
    private TextView textCnicResult;
    private View layoutManualOverride;
    private TextInputEditText inputCnicManual;
    private TextInputLayout layoutCnicManualInput;
    private boolean decisionActionable;
    private boolean cnicVerifiedEvidence;
    private CnicOcrHelper cnicOcrHelper;
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

    @NonNull
    public static GuardPendingDecisionFragment newInstance() {
        return new GuardPendingDecisionFragment();
    }

    @NonNull
    public static GuardPendingDecisionFragment newInstance(@NonNull GuardPendingDecision decision) {
        GuardPendingDecisionFragment fragment = new GuardPendingDecisionFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PENDING_DECISION_JSON, decision.toJson());
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        cnicOcrHelper = new CnicOcrHelper(this, new CnicOcrHelper.Callback() {
            @Override
            public void onScanStarted() {
                if (isAdded()) {
                    Snackbar.make(requireView(), R.string.guard_scanning_cnic, Snackbar.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onResult(@NonNull CnicScanResult result) {
                if (!isAdded()) return;
                handleCnicScanResult(result);
            }
        });
        cnicOcrHelper.register();
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_guard_pending_decision, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        entryRequestRepository = RepositoryProvider.getRepository();
        guestPassRepository = RepositoryProvider.getGuestPassRepository();
        pendingDecisionStore = new GuardPendingDecisionStore(requireContext());
        auditEventLogger = new AuditEventLogger();

        buttonAllow = view.findViewById(R.id.button_pending_allow);
        buttonDeny = view.findViewById(R.id.button_pending_deny);
        buttonClose = view.findViewById(R.id.button_pending_close);
        checkCnicVerified = view.findViewById(R.id.check_pending_cnic_verified);
        checkVehicleVerified = view.findViewById(R.id.check_pending_vehicle_verified);
        textCnicResult = view.findViewById(R.id.text_cnic_verify_result);
        layoutManualOverride = view.findViewById(R.id.layout_cnic_manual_override);
        inputCnicManual = view.findViewById(R.id.input_cnic_manual);
        layoutCnicManualInput = view.findViewById(R.id.layout_cnic_manual_input);
        buttonClose.setOnClickListener(v -> closeDecisionWithoutAction());

        ImageButton buttonVerifyCamera = view.findViewById(R.id.button_verify_cnic_camera);
        buttonVerifyCamera.setOnClickListener(v -> cnicOcrHelper.launchCameraOrRequestPermission());

        // Manual override: attach formatter and verify button
        GuestIdentityInputSupport.attachCnicFormatter(inputCnicManual);
        view.findViewById(R.id.button_cnic_manual_verify).setOnClickListener(v -> verifyManualCnic());

        RoleNavRouter.bindBottomNav(view, this, RoleDestination.SCAN);
        GuardLanguageUiBinder.bind(view, this);

        String guardUid = currentGuardUid();
        pendingDecision = readPendingDecisionFromArgs(guardUid);
        if (pendingDecision == null) {
            pendingDecision = pendingDecisionStore.getForGuard(guardUid);
            Log.d(TAG, "onViewCreated: loaded pending from store=" + (pendingDecision != null));
        } else {
            Log.d(TAG, "onViewCreated: loaded pending from args pass=" + pendingDecision.getPassCode());
        }
        if (pendingDecision == null) {
            Log.d(TAG, "onViewCreated: pending decision missing, routing back to scan");
            routeToScanAndShowMessage(getString(R.string.guard_pending_missing));
            return;
        }

        bindDecision(view, pendingDecision);
        setDecisionActionable(false);
        revalidatePendingDecision();
    }

    @Nullable
    private GuardPendingDecision readPendingDecisionFromArgs(@NonNull String guardUid) {
        Bundle args = getArguments();
        if (args == null) {
            return null;
        }
        String raw = args.getString(ARG_PENDING_DECISION_JSON, "");
        GuardPendingDecision fromArgs = GuardPendingDecision.fromJson(raw);
        if (fromArgs == null || !fromArgs.isValid()) {
            return null;
        }
        String expectedUid = guardUid.trim();
        String decisionUid = fromArgs.getGuardUid().trim();
        if (!expectedUid.isEmpty() && !expectedUid.equals(decisionUid)) {
            return null;
        }
        if (!expectedUid.isEmpty()) {
            pendingDecisionStore.save(fromArgs);
        }
        Log.d(TAG, "readPendingDecisionFromArgs: accepted args decision for pass=" + fromArgs.getPassCode());
        return fromArgs;
    }

    private void bindDecision(@NonNull View view, @NonNull GuardPendingDecision decision) {
        TextView textGuestName = view.findViewById(R.id.text_pending_guest_name);
        TextView textPassCode = view.findViewById(R.id.text_pending_pass_code);
        TextView textGuestPhone = view.findViewById(R.id.text_pending_guest_phone);
        TextView textHasVehicle = view.findViewById(R.id.text_pending_has_vehicle);
        TextView textSponsor = view.findViewById(R.id.text_pending_sponsor);
        TextView textSponsorEmail = view.findViewById(R.id.text_pending_sponsor_email);
        TextView textSponsorRole = view.findViewById(R.id.text_pending_sponsor_role);
        TextView textCreatedAt = view.findViewById(R.id.text_pending_created_at);
        TextView textMethod = view.findViewById(R.id.text_pending_method);

        textGuestName.setText(decision.getGuestName());
        textPassCode.setText(getString(R.string.pass_code_label, decision.getPassCode()));
        textGuestPhone.setText(getString(R.string.guard_pending_guest_phone_label, valueOrUnavailable(decision.getGuestPhone())));
        textHasVehicle.setText(getString(R.string.guard_pending_has_vehicle_label, decision.hasVehicle() ? "Yes" : "No"));
        textSponsor.setText(getString(
                R.string.guard_pending_sponsor_name_label,
                valueOrUnavailable(decision.getSponsorName())
        ));
        textSponsorEmail.setText(getString(R.string.guard_pending_sponsor_email_label, valueOrUnavailable(decision.getSponsorEmail())));
        textSponsorRole.setText(getString(
                R.string.guard_pending_sponsor_role_label,
                formatRole(decision.getSponsorRole())
        ));
        textCreatedAt.setText(getString(
                R.string.guard_pending_created_label,
                formatMillis(decision.getCreatedAtMillis())
        ));
        textMethod.setText(getString(R.string.guard_pending_method_label, humanizeVerificationMethod(decision.getVerificationMethod())));
    }

    private void revalidatePendingDecision() {
        // if (!GuestPassTimePolicy.isEntryWindowOpenNow()) {
        //     clearAndReturnToScan(getString(R.string.pass_not_valid_time_window));
        //     return;
        // }
        guestPassRepository.findPassByCode(pendingDecision.getPassCode(), new GuestPassRepository.PassLookupListener() {
            @Override
            public void onData(@Nullable GuestPass pass) {
                if (!isAdded()) {
                    return;
                }
                requireActivity().runOnUiThread(() -> {
                    if (!isPendingPassActionable(pass, pendingDecision)) {
                        Log.d(TAG, "revalidatePendingDecision: not actionable; status="
                                + (pass == null ? "<null>" : pass.getStatus())
                                + " entryRequestId="
                                + (pass == null ? "<null>" : pass.getEntryRequestId()));
                        recordPendingInvalidated(pass);
                        clearAndReturnToScan(getString(R.string.guard_pending_not_actionable));
                        return;
                    }
                    if (pendingDecision.hasVehicle() && pendingDecision.getVehiclePlate().trim().isEmpty()) {
                        Log.d(TAG, "revalidatePendingDecision: invalid vehicle plate state");
                        recordPendingInvalidated(pass);
                        clearAndReturnToScan(getString(R.string.guard_pending_not_actionable));
                        return;
                    }
                    Log.d(TAG, "revalidatePendingDecision: actionable, enabling decision controls");
                    buttonAllow.setOnClickListener(v -> allowDecision());
                    buttonDeny.setOnClickListener(v -> denyDecision());
                    bindCheckpoints();
                    setDecisionActionable(true);
                });
            }

            @Override
            public void onError(@NonNull Exception exception) {
                if (!isAdded()) {
                    return;
                }
                Log.e(TAG, "revalidatePendingDecision onError", exception);
                requireActivity().runOnUiThread(() ->
                        Snackbar.make(requireView(), R.string.error_verify_credential, Snackbar.LENGTH_LONG).show());
            }
        });
    }

    private boolean isPendingPassActionable(
            @Nullable GuestPass pass,
            @NonNull GuardPendingDecision decision
    ) {
        if (pass == null) {
            return false;
        }
        if (!"active".equalsIgnoreCase(pass.getStatus())) {
            return false;
        }
        if (GuestPassStatusRules.isTimeExpiredActive(pass)) {
            return false;
        }
        String entryRequestId = pass.getEntryRequestId().trim();
        return !entryRequestId.isEmpty() && entryRequestId.equals(decision.getEntryRequestId().trim());
    }

    private void allowDecision() {
        if (!isAllowEligible()) {
            Snackbar.make(requireView(), R.string.guard_checkpoint_required, Snackbar.LENGTH_SHORT).show();
            return;
        }
        setButtonsEnabled(false);
        entryRequestRepository.logEntry(pendingDecision.getEntryRequestId(), (entrySuccess, entryMessage, entryError) -> {
            if (!isAdded()) {
                return;
            }
            if (!entrySuccess) {
                requireActivity().runOnUiThread(() -> {
                    setDecisionActionable(true);
                    Snackbar.make(requireView(), entryMessage, Snackbar.LENGTH_LONG).show();
                });
                return;
            }
            guestPassRepository.markPassAdmittedByEntryRequestId(
                    pendingDecision.getEntryRequestId(),
                    currentGuardUid(),
                    pendingDecision.getVerificationMethod(),
                    (passSuccess, passMessage, passError) -> {
                        if (!isAdded()) {
                            return;
                        }
                        requireActivity().runOnUiThread(() -> {
                            if (!passSuccess) {
                                setDecisionActionable(true);
                                Snackbar.make(requireView(), passMessage, Snackbar.LENGTH_LONG).show();
                                return;
                            }
                            recordPendingResolvedAllow();
                            clearPendingDecision();
                            routeToDashboard();
                        });
                    }
            );
        });
    }

    private void denyDecision() {
        setButtonsEnabled(false);
        String reason = getString(
                R.string.guard_scan_default_deny_reason,
                pendingDecision.getVerificationMethod()
        );
        entryRequestRepository.denyRequest(pendingDecision.getEntryRequestId(), reason, (denySuccess, denyMessage, denyError) -> {
            if (!isAdded()) {
                return;
            }
            if (!denySuccess) {
                requireActivity().runOnUiThread(() -> {
                    setDecisionActionable(true);
                    Snackbar.make(requireView(), denyMessage, Snackbar.LENGTH_LONG).show();
                });
                return;
            }
            guestPassRepository.markPassDeniedByEntryRequestId(
                    pendingDecision.getEntryRequestId(),
                    currentGuardUid(),
                    (passSuccess, passMessage, passError) -> {
                        if (!isAdded()) {
                            return;
                        }
                        requireActivity().runOnUiThread(() -> {
                            if (!passSuccess) {
                                setDecisionActionable(true);
                                Snackbar.make(requireView(), passMessage, Snackbar.LENGTH_LONG).show();
                                return;
                            }
                            recordPendingResolvedDeny();
                            clearPendingDecision();
                            routeToDashboard();
                        });
                    }
            );
        });
    }

    private void clearAndReturnToScan(@NonNull String message) {
        clearPendingDecision();
        routeToScanAndShowMessage(message);
    }

    private void clearPendingDecision() {
        pendingDecisionStore.clearForGuard(currentGuardUid());
    }

    private void setButtonsEnabled(boolean enabled) {
        buttonAllow.setEnabled(enabled);
        buttonDeny.setEnabled(enabled);
        buttonAllow.setAlpha(enabled ? 1f : 0.6f);
        buttonDeny.setAlpha(enabled ? 1f : 0.6f);
    }

    private void bindCheckpoints() {
        cnicVerifiedEvidence = false;
        checkCnicVerified.setChecked(false);
        checkCnicVerified.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked && !cnicVerifiedEvidence) {
                buttonView.setChecked(false);
                if (isAdded()) {
                    Snackbar.make(requireView(), R.string.guard_cnic_verify_first, Snackbar.LENGTH_SHORT).show();
                }
                return;
            }
            updateAllowButtonState();
        });
        textCnicResult.setVisibility(View.GONE);
        layoutManualOverride.setVisibility(View.VISIBLE);
        layoutCnicManualInput.setError(null);
        if (pendingDecision.hasVehicle()) {
            checkVehicleVerified.setVisibility(View.VISIBLE);
            String label = getString(
                    R.string.guard_checkpoint_vehicle_verified_with_plate,
                    valueOrUnavailable(pendingDecision.getVehiclePlate())
            );
            checkVehicleVerified.setText(label);
            checkVehicleVerified.setChecked(false);
            checkVehicleVerified.setOnCheckedChangeListener((buttonView, isChecked) -> updateAllowButtonState());
        } else {
            checkVehicleVerified.setVisibility(View.GONE);
            checkVehicleVerified.setChecked(false);
            checkVehicleVerified.setOnCheckedChangeListener(null);
        }
        updateAllowButtonState();
    }

    private void setDecisionActionable(boolean actionable) {
        decisionActionable = actionable;
        if (!actionable) {
            setButtonsEnabled(false);
            return;
        }
        buttonDeny.setEnabled(true);
        buttonDeny.setAlpha(1f);
        updateAllowButtonState();
    }

    private void updateAllowButtonState() {
        boolean enabled = decisionActionable && isAllowEligible();
        buttonAllow.setEnabled(enabled);
        buttonAllow.setAlpha(enabled ? 1f : 0.6f);
    }

    private boolean isAllowEligible() {
        if (!cnicVerifiedEvidence) return false;
        if (!checkCnicVerified.isChecked()) return false;
        if (pendingDecision != null && pendingDecision.hasVehicle()) {
            return checkVehicleVerified.isChecked();
        }
        return true;
    }

    private void handleCnicScanResult(@NonNull CnicScanResult result) {
        textCnicResult.setVisibility(View.VISIBLE);
        layoutCnicManualInput.setError(null);

        if (!result.isSuccess()) {
            setCnicVerified(false);
            logCnicVerification(false, "CNIC_SCAN", "", result.getFailureReason());
            textCnicResult.setText(getString(R.string.guard_cnic_scan_warning, result.getFailureReason()));
            textCnicResult.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.semantic_warning_container));
            textCnicResult.setTextColor(ContextCompat.getColor(requireContext(), R.color.semantic_warning_on_container));
            return;
        }

        String scanned = result.getNormalizedCnic();
        String expected = pendingDecision != null ? pendingDecision.getGuestIdNumber() : "";
        boolean matches = scanned.equalsIgnoreCase(expected.trim());

        if (matches) {
            setCnicVerified(true);
            logCnicVerification(true, "CNIC_SCAN", scanned, "matched");
            textCnicResult.setText(getString(R.string.guard_cnic_match, scanned));
            textCnicResult.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.semantic_success_container));
            textCnicResult.setTextColor(ContextCompat.getColor(requireContext(), R.color.semantic_success_on_container));
        } else {
            setCnicVerified(false);
            logCnicVerification(false, "CNIC_SCAN", scanned, "mismatch");
            textCnicResult.setText(getString(R.string.guard_cnic_mismatch, scanned, expected));
            textCnicResult.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.md_error_container));
            textCnicResult.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_on_error_container));
        }
    }

    private void verifyManualCnic() {
        CharSequence raw = inputCnicManual.getText();
        String input = raw == null ? "" : raw.toString().trim();
        String expected = pendingDecision != null ? pendingDecision.getGuestIdNumber() : "";
        String normalized = GuestIdentityPolicy.normalizeCnic(input);

        if (normalized == null) {
            layoutCnicManualInput.setError(getString(R.string.error_invalid_cnic));
            logCnicVerification(false, "MANUAL_CNIC", input, "invalid_format");
            return;
        }
        layoutCnicManualInput.setError(null);

        if (normalized.equalsIgnoreCase(expected.trim())) {
            setCnicVerified(true);
            logCnicVerification(true, "MANUAL_CNIC", normalized, "matched");
            textCnicResult.setVisibility(View.VISIBLE);
            textCnicResult.setText(getString(R.string.guard_cnic_match_manual, normalized));
            textCnicResult.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.semantic_success_container));
            textCnicResult.setTextColor(ContextCompat.getColor(requireContext(), R.color.semantic_success_on_container));
        } else {
            setCnicVerified(false);
            logCnicVerification(false, "MANUAL_CNIC", normalized, "mismatch");
            textCnicResult.setVisibility(View.VISIBLE);
            textCnicResult.setText(getString(R.string.guard_cnic_mismatch_entered, normalized, expected));
            textCnicResult.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.md_error_container));
            textCnicResult.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_on_error_container));
            layoutCnicManualInput.setError(getString(R.string.guard_cnic_not_match_records));
        }
    }

    private void setCnicVerified(boolean verified) {
        cnicVerifiedEvidence = verified;
        if (!verified) {
            checkCnicVerified.setChecked(false);
        }
        updateAllowButtonState();
    }

    private void logCnicVerification(
            boolean success,
            @NonNull String method,
            @NonNull String providedValue,
            @NonNull String detail
    ) {
        if (pendingDecision == null) {
            return;
        }
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("passCode", pendingDecision.getPassCode());
        metadata.put("verificationMethod", method);
        metadata.put("providedCnic", providedValue);
        metadata.put("expectedCnic", pendingDecision.getGuestIdNumber());
        metadata.put("detail", detail);
        auditEventLogger.log(
                success ? AuditEventType.CNIC_VERIFIED : AuditEventType.CNIC_VERIFICATION_FAILED,
                "guest_pass",
                pendingDecision.getPassCode(),
                pendingDecision.getEntryRequestId(),
                currentGuardUid(),
                "guard",
                success ? "Guard CNIC verification passed" : "Guard CNIC verification failed",
                "guard_pending_fragment",
                success ? "success" : "failure",
                success ? "cnic_verified" : "cnic_verification_failed",
                GatePolicy.normalizeStoredValue(pendingDecision.getGateLabel()),
                metadata
        );
    }

    private void closeDecisionWithoutAction() {
        clearPendingDecision();
        if (!isAdded()) {
            return;
        }
        if (getParentFragmentManager().getBackStackEntryCount() > 0) {
            getParentFragmentManager().popBackStack();
            return;
        }
        if (requireActivity() instanceof NavigationHost) {
            ((NavigationHost) requireActivity()).showFragment(GuardQrScanFragment.newInstance(), false);
        }
    }

    private void routeToDashboard() {
        if (!isAdded() || !(requireActivity() instanceof NavigationHost)) {
            return;
        }
        ((NavigationHost) requireActivity()).showFragment(DashboardFragment.newInstance(), false);
    }

    private void routeToScanAndShowMessage(@NonNull String message) {
        if (!isAdded() || !(requireActivity() instanceof NavigationHost)) {
            return;
        }
        ((NavigationHost) requireActivity()).showFragment(GuardQrScanFragment.newInstance(message), false);
    }

    @NonNull
    private String currentGuardUid() {
        if (SessionManager.getCurrentProfile() == null) {
            return "";
        }
        return SessionManager.getCurrentProfile().getUid();
    }

    @NonNull
    private String formatMillis(long millis) {
        if (millis <= 0L) {
            return getString(R.string.not_available);
        }
        return timeFormat.format(new Date(millis));
    }

    @NonNull
    private String valueOrUnavailable(@NonNull String value) {
        String trimmed = value.trim();
        return trimmed.isEmpty() ? getString(R.string.not_available) : trimmed;
    }

    @NonNull
    private String formatRole(@NonNull String role) {
        String normalized = role.trim().toLowerCase(Locale.getDefault());
        if (normalized.isEmpty()) {
            return getString(R.string.not_available);
        }
        if (normalized.length() == 1) {
            return normalized.toUpperCase(Locale.getDefault());
        }
        return normalized.substring(0, 1).toUpperCase(Locale.getDefault())
                + normalized.substring(1);
    }

    @NonNull
    private String humanizeVerificationMethod(@NonNull String method) {
        String normalized = method.trim().toUpperCase(Locale.getDefault());
        if ("QR_SCAN".equals(normalized)) {
            return getString(R.string.verification_method_qr_scan);
        }
        if ("PASS_CODE".equals(normalized)) {
            return getString(R.string.verification_method_pass_code);
        }
        return method.replace('_', ' ');
    }

    private void recordPendingResolvedAllow() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("passCode", pendingDecision.getPassCode());
        metadata.put("verificationMethod", pendingDecision.getVerificationMethod());
        metadata.put("hasVehicle", pendingDecision.hasVehicle());
        metadata.put("vehiclePlate", pendingDecision.getVehiclePlate());
        auditEventLogger.log(
                AuditEventType.PENDING_DECISION_RESOLVED_ALLOW,
                "guest_pass",
                pendingDecision.getPassCode(),
                pendingDecision.getEntryRequestId(),
                currentGuardUid(),
                "guard",
                "Pending decision resolved with allow",
                "guard_pending_fragment",
                "success",
                "decision_allow",
                GatePolicy.normalizeStoredValue(pendingDecision.getGateLabel()),
                metadata
        );
    }

    private void recordPendingResolvedDeny() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("passCode", pendingDecision.getPassCode());
        metadata.put("verificationMethod", pendingDecision.getVerificationMethod());
        metadata.put("hasVehicle", pendingDecision.hasVehicle());
        metadata.put("vehiclePlate", pendingDecision.getVehiclePlate());
        auditEventLogger.log(
                AuditEventType.PENDING_DECISION_RESOLVED_DENY,
                "guest_pass",
                pendingDecision.getPassCode(),
                pendingDecision.getEntryRequestId(),
                currentGuardUid(),
                "guard",
                "Pending decision resolved with deny",
                "guard_pending_fragment",
                "failure",
                "decision_deny",
                GatePolicy.normalizeStoredValue(pendingDecision.getGateLabel()),
                metadata
        );
    }

    private void recordPendingInvalidated(@Nullable GuestPass currentPass) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("passCode", pendingDecision.getPassCode());
        metadata.put("verificationMethod", pendingDecision.getVerificationMethod());
        metadata.put("hasVehicle", pendingDecision.hasVehicle());
        metadata.put("vehiclePlate", pendingDecision.getVehiclePlate());
        if (currentPass != null) {
            metadata.put("currentStatus", currentPass.getStatus());
        }
        auditEventLogger.log(
                AuditEventType.PENDING_DECISION_INVALIDATED,
                "guest_pass",
                pendingDecision.getPassCode(),
                pendingDecision.getEntryRequestId(),
                currentGuardUid(),
                "guard",
                "Pending decision invalidated before resolution",
                "guard_pending_fragment",
                "failure",
                "decision_invalidated",
                GatePolicy.normalizeStoredValue(pendingDecision.getGateLabel()),
                metadata
        );
    }
}
