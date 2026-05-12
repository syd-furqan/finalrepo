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
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;

import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Dedicated guard exit decision screen shown for used/overdue scanned passes.
 */
public class GuardExitDecisionFragment extends Fragment {
    private static final String ARG_ENTRY_REQUEST_ID = "arg_entry_request_id";
    private static final String ARG_GUEST_NAME = "arg_guest_name";
    private static final String ARG_GUEST_ID_NUMBER = "arg_guest_id_number";
    private static final String ARG_GUEST_PHONE = "arg_guest_phone";
    private static final String ARG_STATUS = "arg_status";
    private static final String ARG_PASS_CODE = "arg_pass_code";
    private static final String ARG_SOURCE = "arg_source";
    private static final String SOURCE_SCAN = "scan";
    private static final String SOURCE_DASHBOARD = "dashboard";

    public static final String RESULT_KEY = "guard_exit_decision_result";
    public static final String RESULT_MESSAGE = "guard_exit_decision_message";

    private EntryRequestRepository entryRequestRepository;
    private GuestPassRepository guestPassRepository;
    private MaterialButton buttonLogExit;
    private MaterialButton buttonClose;
    private TextView textResult;

    private TextView textGuestName;
    private TextView textPassCode;
    private TextView textGuestId;
    private TextView textGuestPhone;
    private TextView textHasVehicle;
    private TextView textCreatedAt;
    private TextView textSponsor;
    private TextView textSponsorEmail;
    private TextView textSponsorRole;

    private final SimpleDateFormat timeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

    @NonNull
    public static GuardExitDecisionFragment newInstance(
            @NonNull String entryRequestId,
            @NonNull String guestName,
            @NonNull String guestIdNumber,
            @NonNull String guestPhone,
            @NonNull String status,
            @NonNull String passCode
    ) {
        return newInstance(entryRequestId, guestName, guestIdNumber, guestPhone, status, passCode, SOURCE_SCAN);
    }

    @NonNull
    public static GuardExitDecisionFragment newInstanceForDashboard(
            @NonNull String entryRequestId,
            @NonNull String guestName,
            @NonNull String guestIdNumber,
            @NonNull String guestPhone,
            @NonNull String status,
            @NonNull String passCode
    ) {
        return newInstance(entryRequestId, guestName, guestIdNumber, guestPhone, status, passCode, SOURCE_DASHBOARD);
    }

    @NonNull
    private static GuardExitDecisionFragment newInstance(
            @NonNull String entryRequestId,
            @NonNull String guestName,
            @NonNull String guestIdNumber,
            @NonNull String guestPhone,
            @NonNull String status,
            @NonNull String passCode,
            @NonNull String source
    ) {
        GuardExitDecisionFragment fragment = new GuardExitDecisionFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ENTRY_REQUEST_ID, entryRequestId);
        args.putString(ARG_GUEST_NAME, guestName);
        args.putString(ARG_GUEST_ID_NUMBER, guestIdNumber);
        args.putString(ARG_GUEST_PHONE, guestPhone);
        args.putString(ARG_STATUS, status);
        args.putString(ARG_PASS_CODE, passCode);
        args.putString(ARG_SOURCE, source);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_guard_exit_decision, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        entryRequestRepository = RepositoryProvider.getRepository();
        guestPassRepository = RepositoryProvider.getGuestPassRepository();

        Bundle args = requireArguments();
        String entryRequestId = safeArg(args, ARG_ENTRY_REQUEST_ID);
        String guestName = safeArg(args, ARG_GUEST_NAME);
        String guestIdNumber = safeArg(args, ARG_GUEST_ID_NUMBER);
        String guestPhoneValue = safeArg(args, ARG_GUEST_PHONE);
        String passCodeValue = safeArg(args, ARG_PASS_CODE);

        textGuestName = view.findViewById(R.id.text_pending_guest_name);
        textPassCode = view.findViewById(R.id.text_pending_pass_code);
        textGuestId = view.findViewById(R.id.text_pending_guest_id);
        textGuestPhone = view.findViewById(R.id.text_pending_guest_phone);
        textHasVehicle = view.findViewById(R.id.text_pending_has_vehicle);
        textCreatedAt = view.findViewById(R.id.text_pending_created_at);
        textSponsor = view.findViewById(R.id.text_pending_sponsor);
        textSponsorEmail = view.findViewById(R.id.text_pending_sponsor_email);
        textSponsorRole = view.findViewById(R.id.text_pending_sponsor_role);

        textResult = view.findViewById(R.id.text_exit_result);
        buttonClose = view.findViewById(R.id.button_exit_close);
        buttonLogExit = view.findViewById(R.id.button_exit_log);

        textGuestName.setText(valueOrUnavailable(guestName));
        textPassCode.setText(getString(R.string.pass_code_label, passCodeValue));
        textGuestId.setText(getString(R.string.guard_pending_guest_id_label, valueOrUnavailable(guestIdNumber)));
        textGuestPhone.setText(getString(R.string.guard_pending_guest_phone_label, valueOrUnavailable(guestPhoneValue)));
        textHasVehicle.setText(getString(R.string.guard_pending_has_vehicle_label, getString(R.string.not_available)));
        textCreatedAt.setText(getString(R.string.guard_pending_created_label, getString(R.string.not_available)));
        textSponsor.setText(getString(R.string.guard_pending_sponsor_name_label, getString(R.string.not_available)));
        textSponsorEmail.setText(getString(R.string.guard_pending_sponsor_email_label, getString(R.string.not_available)));
        textSponsorRole.setText(getString(R.string.guard_pending_sponsor_role_label, getString(R.string.not_available)));

        loadGuestPassDetails(entryRequestId);

        buttonClose.setOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());
        buttonLogExit.setOnClickListener(v -> logExit(entryRequestId, guestName));

        RoleNavRouter.bindBottomNav(view, this, RoleDestination.SCAN);
        GuardLanguageUiBinder.bind(view, this);
    }

    private void loadGuestPassDetails(@NonNull String entryRequestId) {
        guestPassRepository.findPassByEntryRequestId(entryRequestId, new GuestPassRepository.PassLookupListener() {
            @Override
            public void onData(@Nullable GuestPass pass) {
                if (!isAdded() || pass == null) {
                    return;
                }
                requireActivity().runOnUiThread(() -> bindPass(pass));
            }

            @Override
            public void onError(@NonNull Exception exception) {
                // Keep fallback args view if pass lookup fails.
            }
        });
    }

    private void bindPass(@NonNull GuestPass pass) {
        textGuestName.setText(valueOrUnavailable(pass.getGuestName()));
        textPassCode.setText(getString(R.string.pass_code_label, pass.getPassCode()));
        textGuestId.setText(getString(R.string.guard_pending_guest_id_label, valueOrUnavailable(pass.getGuestIdNumber())));
        textGuestPhone.setText(getString(R.string.guard_pending_guest_phone_label, valueOrUnavailable(pass.getGuestPhone())));
        textHasVehicle.setText(getString(R.string.guard_pending_has_vehicle_label, pass.hasVehicle() ? "Yes" : "No"));
        textCreatedAt.setText(getString(R.string.guard_pending_created_label, formatMillis(pass.getCreatedAt())));
        textSponsor.setText(getString(
                R.string.guard_pending_sponsor_name_label,
                valueOrUnavailable(pass.getSponsorName())
        ));
        textSponsorEmail.setText(getString(R.string.guard_pending_sponsor_email_label, valueOrUnavailable(pass.getSponsorEmail())));
        textSponsorRole.setText(getString(R.string.guard_pending_sponsor_role_label, formatRole(pass.getSponsorRole())));
    }

    private void logExit(@NonNull String entryRequestId, @NonNull String guestName) {
        if (entryRequestId.trim().isEmpty()) {
            textResult.setText(R.string.pass_orphan_invalid);
            return;
        }
        setButtonsEnabled(false);
        entryRequestRepository.logExit(entryRequestId, (entrySuccess, entryMessage, entryError) -> {
            if (!isAdded()) {
                return;
            }
            if (!entrySuccess) {
                requireActivity().runOnUiThread(() -> {
                    setButtonsEnabled(true);
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
                        routeToScanWithMessage(getString(R.string.guard_exit_logged_for_guest, guestName));
                    } else {
                        setButtonsEnabled(true);
                        textResult.setText(getString(R.string.guard_exit_partial_issue, passMessage));
                        Snackbar.make(requireView(), textResult.getText(), Snackbar.LENGTH_LONG).show();
                    }
                });
            });
        });
    }

    private void routeToScanWithMessage(@NonNull String message) {
        if (!isAdded()) {
            return;
        }
        String source = safeArg(requireArguments(), ARG_SOURCE);
        if (SOURCE_DASHBOARD.equals(source)) {
            Bundle result = new Bundle();
            result.putString(RESULT_MESSAGE, message);
            getParentFragmentManager().setFragmentResult(RESULT_KEY, result);
            getParentFragmentManager().popBackStack();
            return;
        }
        if (!(requireActivity() instanceof NavigationHost)) {
            return;
        }
        ((NavigationHost) requireActivity()).showFragment(GuardQrScanFragment.newInstance(message), false);
    }

    private void setButtonsEnabled(boolean enabled) {
        buttonClose.setEnabled(enabled);
        buttonLogExit.setEnabled(enabled);
        buttonLogExit.setAlpha(enabled ? 1.0f : 0.5f);
    }

    @NonNull
    private String safeArg(@NonNull Bundle args, @NonNull String key) {
        String value = args.getString(key);
        return value == null ? "" : value;
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
        return normalized.substring(0, 1).toUpperCase(Locale.getDefault()) + normalized.substring(1);
    }

    @NonNull
    private String formatMillis(@Nullable com.google.firebase.Timestamp timestamp) {
        if (timestamp == null) {
            return getString(R.string.not_available);
        }
        return timeFormat.format(timestamp.toDate());
    }
}
