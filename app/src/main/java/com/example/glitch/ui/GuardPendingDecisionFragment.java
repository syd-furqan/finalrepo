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
import com.example.glitch.data.EntryRequestRepository;
import com.example.glitch.data.GuestPassRepository;
import com.example.glitch.data.RepositoryProvider;
import com.example.glitch.model.GatePolicy;
import com.example.glitch.model.GuestPass;
import com.example.glitch.model.GuestPassStatusRules;
import com.example.glitch.model.GuestPassTimePolicy;
import com.example.glitch.model.GuardPendingDecision;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Full-screen, non-cancelable guard decision screen for unresolved scanned passes.
 */
public class GuardPendingDecisionFragment extends Fragment {
    private EntryRequestRepository entryRequestRepository;
    private GuestPassRepository guestPassRepository;
    private GuardPendingDecisionStore pendingDecisionStore;
    private GuardPendingDecision pendingDecision;
    private MaterialButton buttonAllow;
    private MaterialButton buttonDeny;
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
        RoleNavRouter.bindBottomNav(view, this, RoleDestination.VEHICLES);

        String guardUid = currentGuardUid();
        pendingDecision = pendingDecisionStore.getForGuard(guardUid);
        if (pendingDecision == null) {
            routeToScanAndShowMessage(getString(R.string.guard_pending_missing));
            return;
        }

        bindDecision(view, pendingDecision);
        setButtonsEnabled(false);
        revalidatePendingDecision();
    }

    private void bindDecision(@NonNull View view, @NonNull GuardPendingDecision decision) {
        TextView textGuestName = view.findViewById(R.id.text_pending_guest_name);
        TextView textPassCode = view.findViewById(R.id.text_pending_pass_code);
        TextView textMethod = view.findViewById(R.id.text_pending_method);
        TextView textRequestId = view.findViewById(R.id.text_pending_request_id);
        TextView textGuestId = view.findViewById(R.id.text_pending_guest_id);
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
                        clearAndReturnToScan(getString(R.string.guard_pending_not_actionable));
                        return;
                    }
                    buttonAllow.setOnClickListener(v -> allowDecision());
                    buttonDeny.setOnClickListener(v -> denyDecision());
                    setButtonsEnabled(true);
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
        setButtonsEnabled(false);
        entryRequestRepository.logEntry(pendingDecision.getEntryRequestId(), (entrySuccess, entryMessage, entryError) -> {
            if (!isAdded()) {
                return;
            }
            if (!entrySuccess) {
                requireActivity().runOnUiThread(() -> {
                    setButtonsEnabled(true);
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
                                setButtonsEnabled(true);
                                Snackbar.make(requireView(), passMessage, Snackbar.LENGTH_LONG).show();
                                return;
                            }
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
                    setButtonsEnabled(true);
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
                                setButtonsEnabled(true);
                                Snackbar.make(requireView(), passMessage, Snackbar.LENGTH_LONG).show();
                                return;
                            }
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
}
