package com.example.glitch.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.glitch.R;
import com.example.glitch.auth.SessionManager;
import com.example.glitch.data.InterventionRepository;
import com.example.glitch.data.RepositoryProvider;
import com.example.glitch.data.ViolationReportRepository;
import com.example.glitch.model.UserProfile;
import com.example.glitch.model.ViolationReport;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class AdminViolationDetailBottomSheetFragment extends BottomSheetDialogFragment {
    public static final String TAG = "AdminViolationDetailSheet";
    public static final String RESULT_KEY = "violation_detail_result";

    private static final String ARG_REPORT_ID = "report_id";
    private static final String ARG_REPORTER_NAME = "reporter_name";
    private static final String ARG_REPORTER_ROLE = "reporter_role";
    private static final String ARG_DETAIL = "detail";
    private static final String ARG_LEVEL = "level";
    private static final String ARG_STATUS = "status";
    private static final String ARG_SUBJECT_TYPE = "subject_type";
    private static final String ARG_GUEST_NAME = "guest_name";
    private static final String ARG_GUEST_CNIC = "guest_cnic";
    private static final String ARG_SPONSOR_UID = "sponsor_uid";
    private static final String ARG_SPONSOR_NAME = "sponsor_name";
    private static final String ARG_SPONSOR_ROLE = "sponsor_role";
    private static final String ARG_STUDENT_UID = "student_uid";
    private static final String ARG_STUDENT_NAME = "student_name";
    private static final String ARG_STUDENT_EMAIL = "student_email";

    // Unpacked fields
    private String reportId, reporterName, reporterRole, detail, level, status;
    private String subjectType, guestName, guestCnic, sponsorUid, sponsorName, sponsorRole;
    private String studentUid, studentName, studentEmail;
    private boolean isGuestViolation, isPending;

    private InterventionRepository interventionRepo;
    private ViolationReportRepository violationRepo;

    private TextInputLayout layoutWarnMessage;
    private TextInputEditText inputWarnMessage;
    private TextInputLayout layoutChargeAmount;
    private TextInputEditText inputChargeAmount;

    private MaterialButton buttonWarn;
    private MaterialButton buttonCharge;
    private MaterialButton buttonBan;
    private MaterialButton buttonIgnore;

    @NonNull
    public static AdminViolationDetailBottomSheetFragment newInstance(@NonNull ViolationReport r) {
        AdminViolationDetailBottomSheetFragment f = new AdminViolationDetailBottomSheetFragment();
        Bundle args = new Bundle();
        args.putString(ARG_REPORT_ID, r.getId());
        args.putString(ARG_REPORTER_NAME, r.getReporterName());
        args.putString(ARG_REPORTER_ROLE, r.getReporterRole());
        args.putString(ARG_DETAIL, r.getDetail());
        args.putString(ARG_LEVEL, r.getViolationLevel());
        args.putString(ARG_STATUS, r.getStatus());
        args.putString(ARG_SUBJECT_TYPE, r.getSubjectType());
        args.putString(ARG_GUEST_NAME, r.getGuestName());
        args.putString(ARG_GUEST_CNIC, r.getGuestCnic());
        args.putString(ARG_SPONSOR_UID, r.getSponsorUid());
        args.putString(ARG_SPONSOR_NAME, r.getSponsorName());
        args.putString(ARG_SPONSOR_ROLE, r.getSponsorRole());
        args.putString(ARG_STUDENT_UID, r.getSubjectStudentUid());
        args.putString(ARG_STUDENT_NAME, r.getSubjectStudentName());
        args.putString(ARG_STUDENT_EMAIL, r.getSubjectStudentEmail());
        f.setArguments(args);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_admin_violation_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        interventionRepo = RepositoryProvider.getInterventionRepository();
        violationRepo = RepositoryProvider.getViolationReportRepository();

        Bundle a = requireArguments();
        reportId = safe(a, ARG_REPORT_ID);
        reporterName = safe(a, ARG_REPORTER_NAME);
        reporterRole = safe(a, ARG_REPORTER_ROLE);
        detail = safe(a, ARG_DETAIL);
        level = safe(a, ARG_LEVEL);
        status = safe(a, ARG_STATUS);
        subjectType = safe(a, ARG_SUBJECT_TYPE);
        guestName = safe(a, ARG_GUEST_NAME);
        guestCnic = safe(a, ARG_GUEST_CNIC);
        sponsorUid = safe(a, ARG_SPONSOR_UID);
        sponsorName = safe(a, ARG_SPONSOR_NAME);
        sponsorRole = safe(a, ARG_SPONSOR_ROLE);
        studentUid = safe(a, ARG_STUDENT_UID);
        studentName = safe(a, ARG_STUDENT_NAME);
        studentEmail = safe(a, ARG_STUDENT_EMAIL);
        isGuestViolation = ViolationReport.SUBJECT_GUEST.equalsIgnoreCase(subjectType);
        isPending = ViolationReport.STATUS_PENDING.equalsIgnoreCase(status);

        bindViews(view);
        populateData(view);
        setupActions();
    }

    private void bindViews(@NonNull View view) {
        layoutWarnMessage = view.findViewById(R.id.layout_warning_detail);
        inputWarnMessage = view.findViewById(R.id.input_warning_detail);
        layoutChargeAmount = view.findViewById(R.id.layout_charge_amount);
        inputChargeAmount = view.findViewById(R.id.input_charge_amount);
        buttonWarn = view.findViewById(R.id.button_action_warn);
        buttonCharge = view.findViewById(R.id.button_action_charge);
        buttonBan = view.findViewById(R.id.button_action_ban);
        buttonIgnore = view.findViewById(R.id.button_action_ignore);
        view.findViewById(R.id.button_report_close).setOnClickListener(v -> dismiss());
    }

    private void populateData(@NonNull View view) {
        TextView textLevel = view.findViewById(R.id.text_report_level);
        TextView textStatus = view.findViewById(R.id.text_report_status);
        TextView textReporter = view.findViewById(R.id.text_report_reporter);
        TextView textDetail = view.findViewById(R.id.text_report_detail);
        TextView textSubject = view.findViewById(R.id.text_report_subject);
        TextView textSponsor = view.findViewById(R.id.text_report_sponsor);

        textLevel.setText(level.toUpperCase());
        textStatus.setText("Status: " + status.toUpperCase());
        textReporter.setText("Reported by: " + orNa(reporterName) + " (" + orNa(reporterRole) + ")");
        textDetail.setText(detail);

        if (isGuestViolation) {
            textSubject.setText("Guest: " + orNa(guestName) + " | CNIC: " + orNa(guestCnic));
            textSponsor.setText("Sponsor: " + orNa(sponsorName) + " (" + orNa(sponsorRole) + ")");
            textSponsor.setVisibility(View.VISIBLE);
        } else {
            textSubject.setText("Student: " + orNa(studentName) + " | " + orNa(studentEmail));
            textSponsor.setVisibility(View.GONE);
        }

        buttonBan.setVisibility(isGuestViolation ? View.VISIBLE : View.GONE);

        View actionsContainer = view.findViewById(R.id.container_report_actions);
        actionsContainer.setVisibility(isPending ? View.VISIBLE : View.GONE);
        buttonWarn.setEnabled(isPending);
        buttonCharge.setEnabled(isPending);
        buttonBan.setEnabled(isPending);
        buttonIgnore.setEnabled(isPending);
    }

    private void setupActions() {
        buttonWarn.setOnClickListener(v -> {
            if (layoutWarnMessage.getVisibility() == View.VISIBLE) {
                submitWarn();
            } else {
                layoutWarnMessage.setVisibility(View.VISIBLE);
                layoutChargeAmount.setVisibility(View.GONE);
            }
        });

        buttonCharge.setOnClickListener(v -> {
            if (layoutChargeAmount.getVisibility() == View.VISIBLE) {
                submitCharge();
            } else {
                layoutChargeAmount.setVisibility(View.VISIBLE);
                layoutWarnMessage.setVisibility(View.GONE);
            }
        });

        buttonBan.setOnClickListener(v -> banGuest());
        buttonIgnore.setOnClickListener(v -> ignoreReport());
    }

    private void submitWarn() {
        String message = readInput(inputWarnMessage);
        if (message.isEmpty()) {
            Snackbar.make(requireView(), "Enter a warning message.", Snackbar.LENGTH_SHORT).show();
            return;
        }
        String adminUid = adminUid();
        String targetUid = isGuestViolation ? sponsorUid : studentUid;
        String targetName = isGuestViolation ? sponsorName : studentName;
        String targetRole = isGuestViolation ? sponsorRole : "student";

        setButtonsEnabled(false);
        interventionRepo.issueWarning(targetUid, targetName, targetRole, reportId, level, message, adminUid,
                (success, msg, ex) -> {
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() -> {
                        if (success) markActioned(adminUid);
                        else {
                            Snackbar.make(requireView(), msg, Snackbar.LENGTH_LONG).show();
                            setButtonsEnabled(true);
                        }
                    });
                }
        );
    }

    private void submitCharge() {
        String adminUid = adminUid();
        String targetUid = isGuestViolation ? sponsorUid : studentUid;
        String targetName = isGuestViolation ? guestName : studentName;
        String targetCnic = isGuestViolation ? guestCnic : "";

        setButtonsEnabled(false);
        interventionRepo.createChargeForReport(reportId, targetUid, targetName, targetCnic, level, adminUid,
                (success, msg, ex) -> {
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() -> {
                        if (success) markActioned(adminUid);
                        else {
                            Snackbar.make(requireView(), msg, Snackbar.LENGTH_LONG).show();
                            setButtonsEnabled(true);
                        }
                    });
                }
        );
    }

    private void banGuest() {
        if (guestCnic.isEmpty()) {
            Snackbar.make(requireView(), "No CNIC available for ban.", Snackbar.LENGTH_SHORT).show();
            return;
        }
        setButtonsEnabled(false);
        interventionRepo.banGuest(guestCnic, adminUid(), level, reportId,
                (success, msg, ex) -> {
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() -> {
                        if (success) markActioned(adminUid());
                        else {
                            Snackbar.make(requireView(), msg, Snackbar.LENGTH_LONG).show();
                            setButtonsEnabled(true);
                        }
                    });
                }
        );
    }

    private void ignoreReport() {
        setButtonsEnabled(false);
        violationRepo.ignoreReport(reportId, adminUid(), (success, msg, ex) -> {
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                Snackbar.make(requireView(), success ? "Report ignored." : msg, Snackbar.LENGTH_SHORT).show();
                notifyParentAndDismiss();
            });
        });
    }

    private void markActioned(@NonNull String adminUid) {
        violationRepo.markActioned(reportId, adminUid, (success, msg, ex) -> {
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                Snackbar.make(requireView(), "Action recorded.", Snackbar.LENGTH_SHORT).show();
                notifyParentAndDismiss();
            });
        });
    }

    private void notifyParentAndDismiss() {
        Bundle result = new Bundle();
        getParentFragmentManager().setFragmentResult(RESULT_KEY, result);
        dismiss();
    }

    private void setButtonsEnabled(boolean enabled) {
        buttonWarn.setEnabled(enabled);
        buttonCharge.setEnabled(enabled);
        buttonBan.setEnabled(enabled);
        buttonIgnore.setEnabled(enabled);
    }

    @NonNull
    private String adminUid() {
        UserProfile profile = SessionManager.getCurrentProfile();
        if (profile == null) return "unknown_admin";
        String uid = profile.getUid().trim();
        return uid.isEmpty() ? "unknown_admin" : uid;
    }

    @NonNull
    private String readInput(@NonNull TextInputEditText input) {
        CharSequence v = input.getText();
        return v == null ? "" : v.toString().trim();
    }

    @NonNull
    private String orNa(@NonNull String value) {
        return value.isEmpty() ? "N/A" : value;
    }

    @NonNull
    private String safe(@NonNull Bundle b, @NonNull String key) {
        String v = b.getString(key);
        return v == null ? "" : v;
    }
}
