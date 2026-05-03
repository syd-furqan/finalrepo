package com.example.glitch.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.glitch.R;
import com.example.glitch.auth.SessionManager;
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
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.snackbar.Snackbar;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Full-screen, non-cancelable guard decision screen for unresolved scanned passes.
 */
public class GuardPendingDecisionFragment extends Fragment {
    private EntryRequestRepository entryRequestRepository;
    private GuestPassRepository guestPassRepository;
    private GuardPendingDecisionStore pendingDecisionStore;
    private AuditEventLogger auditEventLogger;
    private GuardPendingDecision pendingDecision;
    private MaterialButton buttonAllow;
    private MaterialButton buttonDeny;
    private MaterialCheckBox checkGuestVerified;
    private MaterialCheckBox checkVehicleVerified;
    private boolean decisionActionable;
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

    @NonNull
    public static GuardPendingDecisionFragment newInstance() {
        return new GuardPendingDecisionFragment();
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

        requireActivity().getOnBackPressedDispatcher().addCallback(
                getViewLifecycleOwner(),
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        if (!isAdded()) {
                            return;
                        }
                        Snackbar.make(requireView(), R.string.guard_pending_back_blocked, Snackbar.LENGTH_SHORT).show();
                    }
                }
        );

        buttonAllow = view.findViewById(R.id.button_pending_allow);
        buttonDeny = view.findViewById(R.id.button_pending_deny);
        checkGuestVerified = view.findViewById(R.id.check_pending_guest_verified);
        checkVehicleVerified = view.findViewById(R.id.check_pending_vehicle_verified);
        RoleNavRouter.bindBottomNav(view, this, RoleDestination.VEHICLES);

        String guardUid = currentGuardUid();
        pendingDecision = pendingDecisionStore.getForGuard(guardUid);
        if (pendingDecision == null) {
            routeToScanAndShowMessage(getString(R.string.guard_pending_missing));
            return;
        }

        bindDecision(view, pendingDecision);
        setDecisionActionable(false);
        revalidatePendingDecision();
    }

    private void bindDecision(@NonNull View view, @NonNull GuardPendingDecision decision) {
        TextView textGuestName = view.findViewById(R.id.text_pending_guest_name);
        TextView textPassCode = view.findViewById(R.id.text_pending_pass_code);
        TextView textMethod = view.findViewById(R.id.text_pending_method);
        TextView textRequestId = view.findViewById(R.id.text_pending_request_id);
        TextView textGuestId = view.findViewById(R.id.text_pending_guest_id);
        TextView textGuestType = view.findViewById(R.id.text_pending_guest_type);
        TextView textVehiclePlate = view.findViewById(R.id.text_pending_vehicle_plate);
        TextView textGate = view.findViewById(R.id.text_pending_gate);
        TextView textSponsor = view.findViewById(R.id.text_pending_sponsor);
        TextView textSponsorRole = view.findViewById(R.id.text_pending_sponsor_role);
        TextView textCreatedAt = view.findViewById(R.id.text_pending_created_at);
        TextView textExpiresAt = view.findViewById(R.id.text_pending_expires_at);

        textGuestName.setText(decision.getGuestName());
        textPassCode.setText(getString(R.string.pass_code_label, decision.getPassCode()));
        textMethod.setText(getString(R.string.guard_pending_method_label, decision.getVerificationMethod()));
        textRequestId.setText(getString(R.string.guard_pending_request_label, decision.getEntryRequestId()));
        textGuestId.setText(getString(R.string.guard_pending_guest_id_label, valueOrUnavailable(decision.getGuestIdNumber())));
        textGuestType.setText(getString(
                R.string.guard_pending_guest_type_label,
                formatGuestType(decision.getGuestType())
        ));
        textVehiclePlate.setText(getString(
                R.string.guard_pending_vehicle_plate_label,
                decision.hasVehicle() ? valueOrUnavailable(decision.getVehiclePlate()) : "N/A"
        ));
        textGate.setText(getString(R.string.gate_label) + ": " + GatePolicy.toDisplayLabel(decision.getGateLabel()));
        textSponsor.setText(getString(
                R.string.guard_pending_sponsor_label,
                valueOrUnavailable(decision.getSponsorName()),
                valueOrUnavailable(decision.getSponsorEmail())
        ));
        textSponsorRole.setText(getString(
                R.string.guard_pending_sponsor_role_label,
                valueOrUnavailable(decision.getSponsorRole())
        ));
        textCreatedAt.setText(getString(
                R.string.guard_pending_created_label,
                formatMillis(decision.getCreatedAtMillis())
        ));
        textExpiresAt.setText(getString(
                R.string.guard_pending_expires_label,
                formatMillis(decision.getExpiresAtMillis())
        ));
    }

    private void revalidatePendingDecision() {
        if (!GuestPassTimePolicy.isEntryWindowOpenNow()) {
            clearAndReturnToScan(getString(R.string.pass_not_valid_time_window));
            return;
        }
        guestPassRepository.findPassByCode(pendingDecision.getPassCode(), new GuestPassRepository.PassLookupListener() {
            @Override
            public void onData(@Nullable GuestPass pass) {
                if (!isAdded()) {
                    return;
                }
                requireActivity().runOnUiThread(() -> {
                    if (!isPendingPassActionable(pass, pendingDecision)) {
                        recordPendingInvalidated(pass);
                        clearAndReturnToScan(getString(R.string.guard_pending_not_actionable));
                        return;
                    }
                    if (pendingDecision.hasVehicle() && pendingDecision.getVehiclePlate().trim().isEmpty()) {
                        recordPendingInvalidated(pass);
                        clearAndReturnToScan(getString(R.string.guard_pending_not_actionable));
                        return;
                    }
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
        checkGuestVerified.setChecked(false);
        checkGuestVerified.setOnCheckedChangeListener((buttonView, isChecked) -> updateAllowButtonState());
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
        if (!checkGuestVerified.isChecked()) {
            return false;
        }
        if (pendingDecision.hasVehicle()) {
            return checkVehicleVerified.isChecked();
        }
        return true;
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
            return "Not available";
        }
        return timeFormat.format(new Date(millis));
    }

    @NonNull
    private String valueOrUnavailable(@NonNull String value) {
        String trimmed = value.trim();
        return trimmed.isEmpty() ? "Not available" : trimmed;
    }

    @NonNull
    private String formatGuestType(@NonNull String value) {
        String normalized = value.trim().toLowerCase(Locale.getDefault());
        if (normalized.isEmpty()) {
            return "Not available";
        }
        if ("vehicle".equals(normalized)) {
            return "Vehicle";
        }
        if ("non_vehicle".equals(normalized)) {
            return "Non Vehicle";
        }
        return value;
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
