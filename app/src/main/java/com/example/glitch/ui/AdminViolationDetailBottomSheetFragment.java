package com.example.glitch.ui;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.glitch.R;
import com.example.glitch.auth.SessionManager;
import com.example.glitch.data.AlertRepository;
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
import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
    private static final String ARG_GUEST_PHONE = "guest_phone";
    private static final String ARG_GUEST_PASS_ID = "guest_pass_id";
    private static final String ARG_ENTRY_REQUEST_ID = "entry_request_id";
    private static final String ARG_PREVIOUS_GUEST_PASS_STATUS = "previous_guest_pass_status";
    private static final String ARG_PREVIOUS_ENTRY_REQUEST_STATUS = "previous_entry_request_status";
    private static final String ARG_SPONSOR_UID = "sponsor_uid";
    private static final String ARG_SPONSOR_NAME = "sponsor_name";
    private static final String ARG_SPONSOR_ROLE = "sponsor_role";
    private static final String ARG_SPONSOR_STUDENT_ID = "sponsor_student_id";
    private static final String ARG_STUDENT_UID = "student_uid";
    private static final String ARG_STUDENT_NAME = "student_name";
    private static final String ARG_STUDENT_EMAIL = "student_email";
    private static final String ARG_STUDENT_ID = "student_id";

    private static final String ACTION_EXONERATE = "exonerate";
    private static final String ACTION_WARN = "warning_issued";
    private static final String ACTION_BAN = "banned";
    private static final String ACTION_FINE = "fined";

    private String reportId, reporterName, reporterRole, detail, level, status;
    private String subjectType, guestName, guestCnic, guestPhone, guestPassId, entryRequestId, previousGuestPassStatus, previousEntryRequestStatus;
    private String sponsorUid, sponsorName, sponsorRole, sponsorStudentId;
    private String studentUid, studentName, studentEmail, studentId;
    private boolean isGuestViolation, isPending;

    private InterventionRepository interventionRepo;
    private ViolationReportRepository violationRepo;
    private AlertRepository alertRepo;

    private LinearLayout containerGuestReview;
    private LinearLayout containerStudentReview;
    private TextView textStudentActionTitle;
    private RadioGroup radioGuestAction;
    private RadioGroup radioStudentAction;
    private TextInputLayout layoutGuestReason;
    private TextInputEditText inputGuestReason;
    private MaterialButton buttonGuestBanDate;
    private TextView textGuestBanDate;
    private TextInputLayout layoutStudentReason;
    private TextInputEditText inputStudentReason;
    private TextInputLayout layoutStudentFineAmount;
    private TextInputEditText inputStudentFineAmount;
    private MaterialButton buttonSubmitReview;

    private Timestamp guestBanEndAt;

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
        args.putString(ARG_GUEST_PHONE, r.getGuestPhone());
        args.putString(ARG_GUEST_PASS_ID, r.getGuestPassId());
        args.putString(ARG_ENTRY_REQUEST_ID, r.getEntryRequestId());
        args.putString(ARG_PREVIOUS_GUEST_PASS_STATUS, r.getPreviousGuestPassStatus());
        args.putString(ARG_PREVIOUS_ENTRY_REQUEST_STATUS, r.getPreviousEntryRequestStatus());
        args.putString(ARG_SPONSOR_UID, r.getSponsorUid());
        args.putString(ARG_SPONSOR_NAME, r.getSponsorName());
        args.putString(ARG_SPONSOR_ROLE, r.getSponsorRole());
        args.putString(ARG_SPONSOR_STUDENT_ID, r.getSponsorStudentId());
        args.putString(ARG_STUDENT_UID, r.getSubjectStudentUid());
        args.putString(ARG_STUDENT_NAME, r.getSubjectStudentName());
        args.putString(ARG_STUDENT_EMAIL, r.getSubjectStudentEmail());
        args.putString(ARG_STUDENT_ID, r.getSubjectStudentId());
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
        alertRepo = RepositoryProvider.getAlertRepository();

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
        guestPhone = safe(a, ARG_GUEST_PHONE);
        guestPassId = safe(a, ARG_GUEST_PASS_ID);
        entryRequestId = safe(a, ARG_ENTRY_REQUEST_ID);
        previousGuestPassStatus = safe(a, ARG_PREVIOUS_GUEST_PASS_STATUS);
        previousEntryRequestStatus = safe(a, ARG_PREVIOUS_ENTRY_REQUEST_STATUS);
        sponsorUid = safe(a, ARG_SPONSOR_UID);
        sponsorName = safe(a, ARG_SPONSOR_NAME);
        sponsorRole = safe(a, ARG_SPONSOR_ROLE);
        sponsorStudentId = safe(a, ARG_SPONSOR_STUDENT_ID);
        studentUid = safe(a, ARG_STUDENT_UID);
        studentName = safe(a, ARG_STUDENT_NAME);
        studentEmail = safe(a, ARG_STUDENT_EMAIL);
        studentId = safe(a, ARG_STUDENT_ID);
        isGuestViolation = ViolationReport.SUBJECT_GUEST.equalsIgnoreCase(subjectType);
        isPending = ViolationReport.STATUS_PENDING.equalsIgnoreCase(status);

        bindViews(view);
        populateData(view);
        setupActions();
    }

    private void bindViews(@NonNull View view) {
        containerGuestReview = view.findViewById(R.id.container_guest_review);
        containerStudentReview = view.findViewById(R.id.container_student_review);
        textStudentActionTitle = view.findViewById(R.id.text_student_action_title);
        radioGuestAction = view.findViewById(R.id.radio_guest_action);
        radioStudentAction = view.findViewById(R.id.radio_student_action);
        layoutGuestReason = view.findViewById(R.id.layout_guest_reason);
        inputGuestReason = view.findViewById(R.id.input_guest_reason);
        buttonGuestBanDate = view.findViewById(R.id.button_guest_ban_date);
        textGuestBanDate = view.findViewById(R.id.text_guest_ban_date);
        layoutStudentReason = view.findViewById(R.id.layout_student_reason);
        inputStudentReason = view.findViewById(R.id.input_student_reason);
        layoutStudentFineAmount = view.findViewById(R.id.layout_student_fine_amount);
        inputStudentFineAmount = view.findViewById(R.id.input_student_fine_amount);
        buttonSubmitReview = view.findViewById(R.id.button_submit_review);
        view.findViewById(R.id.button_report_close).setOnClickListener(v -> dismiss());
    }

    private void populateData(@NonNull View view) {
        TextView textLevel = view.findViewById(R.id.text_report_level);
        TextView textStatus = view.findViewById(R.id.text_report_status);
        TextView textReporter = view.findViewById(R.id.text_report_reporter);
        TextView textDetail = view.findViewById(R.id.text_report_detail);
        TextView textSubject = view.findViewById(R.id.text_report_subject);
        TextView textSponsor = view.findViewById(R.id.text_report_sponsor);

        textLevel.setText(level.toUpperCase(Locale.US));
        textStatus.setText("Status: " + status.toUpperCase(Locale.US));
        textReporter.setText("Reported by: " + orNa(reporterName) + " (" + orNa(reporterRole) + ")");
        textDetail.setText(detail);

        if (isGuestViolation) {
            textSubject.setText("Guest: " + orNa(guestName) + " | CNIC: " + orNa(guestCnic) + " | Phone: " + orNa(guestPhone));
            String sponsorLine = "Sponsor: " + orNa(sponsorName) + " (" + orNa(sponsorRole) + ")";
            if (!sponsorStudentId.isEmpty()) {
                sponsorLine += " | Student ID: " + sponsorStudentId;
            }
            textSponsor.setText(sponsorLine);
            textSponsor.setVisibility(View.VISIBLE);
            containerGuestReview.setVisibility(View.VISIBLE);
            textStudentActionTitle.setText("Sponsor student action");
        } else {
            textSubject.setText("Student: " + orNa(studentName) + " | ID: " + orNa(studentId));
            textSponsor.setVisibility(View.GONE);
            containerGuestReview.setVisibility(View.GONE);
            textStudentActionTitle.setText("Student action");
        }

        View actionsContainer = view.findViewById(R.id.container_report_actions);
        actionsContainer.setVisibility(isPending ? View.VISIBLE : View.GONE);
        containerStudentReview.setVisibility(isPending ? View.VISIBLE : View.GONE);
        buttonSubmitReview.setEnabled(isPending);
    }

    private void setupActions() {
        radioGuestAction.setOnCheckedChangeListener((group, checkedId) -> {
            boolean banSelected = checkedId == R.id.radio_guest_ban;
            buttonGuestBanDate.setVisibility(banSelected ? View.VISIBLE : View.GONE);
            textGuestBanDate.setVisibility(banSelected && guestBanEndAt != null ? View.VISIBLE : View.GONE);
        });
        radioStudentAction.setOnCheckedChangeListener((group, checkedId) ->
                layoutStudentFineAmount.setVisibility(checkedId == R.id.radio_student_fine ? View.VISIBLE : View.GONE)
        );
        buttonGuestBanDate.setOnClickListener(v -> showBanDatePicker());
        buttonSubmitReview.setOnClickListener(v -> submitReview());
    }

    private void showBanDatePicker() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(
                requireContext(),
                (picker, year, month, dayOfMonth) -> {
                    Calendar chosen = Calendar.getInstance();
                    chosen.set(year, month, dayOfMonth, 23, 59, 59);
                    chosen.set(Calendar.MILLISECOND, 0);
                    guestBanEndAt = new Timestamp(chosen.getTime());
                    textGuestBanDate.setText(String.format(Locale.US, "Ban ends: %04d-%02d-%02d", year, month + 1, dayOfMonth));
                    textGuestBanDate.setVisibility(View.VISIBLE);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        dialog.getDatePicker().setMinDate(System.currentTimeMillis());
        dialog.show();
    }

    private void submitReview() {
        String guestAction = isGuestViolation ? selectedGuestAction() : "";
        String guestReason = readInput(inputGuestReason);
        String studentAction = selectedStudentAction();
        String studentReason = readInput(inputStudentReason);

        if (isGuestViolation && guestAction.isEmpty()) {
            showMessage("Select a guest action.");
            return;
        }
        if (studentAction.isEmpty()) {
            showMessage("Select a student action.");
            return;
        }
        if (isGuestViolation && guestReason.isEmpty()) {
            showMessage("Enter a guest action reason.");
            return;
        }
        if (studentReason.isEmpty()) {
            showMessage("Enter a student action reason.");
            return;
        }
        if (ACTION_BAN.equals(guestAction) && guestBanEndAt == null) {
            showMessage("Choose a ban end date.");
            return;
        }

        double fineAmount = 0.0;
        if (ACTION_FINE.equals(studentAction)) {
            fineAmount = parseAmount(readInput(inputStudentFineAmount));
            if (fineAmount <= 0) {
                showMessage("Enter a fine amount greater than zero.");
                return;
            }
            if (targetStudentId().isEmpty()) {
                showMessage("Student ID is required before a fine can be issued.");
                return;
            }
        }

        String adminUid = adminUid();
        Map<String, Object> reviewFields = buildReviewFields(guestAction, guestReason, studentAction, studentReason, fineAmount, adminUid);
        String finalStatus = resolveFinalStatus(guestAction, studentAction);
        List<ReviewStep> steps = buildReviewSteps(guestAction, guestReason, studentAction, studentReason, fineAmount, adminUid);

        setReviewEnabled(false);
        runReviewSteps(steps, 0, () -> completeViolationReview(finalStatus, reviewFields, adminUid));
    }

    @NonNull
    private Map<String, Object> buildReviewFields(
            @NonNull String guestAction,
            @NonNull String guestReason,
            @NonNull String studentAction,
            @NonNull String studentReason,
            double fineAmount,
            @NonNull String adminUid
    ) {
        Map<String, Object> fields = new HashMap<>();
        fields.put("reviewedByUid", adminUid);
        if (isGuestViolation) {
            fields.put("guestReviewAction", guestAction);
            fields.put("guestReviewReason", guestReason);
            fields.put("guestPassId", guestPassId);
            fields.put("entryRequestId", entryRequestId);
            fields.put("previousGuestPassStatus", previousGuestPassStatus);
            fields.put("previousEntryRequestStatus", previousEntryRequestStatus);
            if (guestBanEndAt != null) {
                fields.put("guestBanEndAt", guestBanEndAt);
            }
        }
        fields.put("studentReviewAction", studentAction);
        fields.put("studentReviewReason", studentReason);
        fields.put("studentFineAmount", fineAmount);
        fields.put("studentFineStudentId", targetStudentId());
        return fields;
    }

    @NonNull
    private List<ReviewStep> buildReviewSteps(
            @NonNull String guestAction,
            @NonNull String guestReason,
            @NonNull String studentAction,
            @NonNull String studentReason,
            double fineAmount,
            @NonNull String adminUid
    ) {
        List<ReviewStep> steps = new ArrayList<>();
        if (isGuestViolation) {
            if (ACTION_EXONERATE.equals(guestAction)) {
                String restoredStatus = restoredGuestPassStatus(previousGuestPassStatus);
                String restoredEntryStatus = restoredEntryRequestStatus(restoredStatus, previousEntryRequestStatus);
                steps.add(callback -> interventionRepo.updateGuestPassStatusForViolation(
                        guestPassId,
                        entryRequestId,
                        restoredStatus,
                        restoredEntryStatus,
                        adminUid,
                        guestReason,
                        callback
                ));
            } else if (ACTION_WARN.equals(guestAction)) {
                steps.add(callback -> interventionRepo.issueGuestWarningForCnic(
                        guestCnic,
                        guestName,
                        guestPhone,
                        reportId,
                        level,
                        guestReason,
                        adminUid,
                        callback
                ));
                steps.add(callback -> interventionRepo.updateGuestPassStatusForViolation(
                        guestPassId,
                        entryRequestId,
                        "exited",
                        "exited",
                        adminUid,
                        guestReason,
                        callback
                ));
            } else if (ACTION_BAN.equals(guestAction) && guestBanEndAt != null) {
                steps.add(callback -> interventionRepo.banGuestUntil(
                        guestCnic,
                        guestName,
                        guestPhone,
                        adminUid,
                        guestReason,
                        reportId,
                        guestBanEndAt,
                        callback
                ));
                steps.add(callback -> interventionRepo.updateGuestPassStatusForViolation(
                        guestPassId,
                        entryRequestId,
                        "exited",
                        "exited",
                        adminUid,
                        guestReason,
                        callback
                ));
            }
        }

        if (ACTION_FINE.equals(studentAction)) {
            steps.add(callback -> interventionRepo.createStudentFineForReport(
                    reportId,
                    targetStudentUid(),
                    targetStudentId(),
                    targetStudentName(),
                    level,
                    fineAmount,
                    studentReason,
                    adminUid,
                    callback
            ));
        }
        return steps;
    }

    private void runReviewSteps(@NonNull List<ReviewStep> steps, int index, @NonNull Runnable onSuccess) {
        if (index >= steps.size()) {
            onSuccess.run();
            return;
        }
        steps.get(index).run((success, message, exception) -> {
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                if (success) {
                    runReviewSteps(steps, index + 1, onSuccess);
                } else {
                    showMessage(message);
                    setReviewEnabled(true);
                }
            });
        });
    }

    private void completeViolationReview(
            @NonNull String finalStatus,
            @NonNull Map<String, Object> reviewFields,
            @NonNull String adminUid
    ) {
        violationRepo.completeReview(reportId, finalStatus, reviewFields, (success, message, exception) -> {
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                if (!success) {
                    showMessage(message);
                    setReviewEnabled(true);
                    return;
                }
                String alertStatus = ViolationReport.STATUS_EXONERATED.equals(finalStatus) ? "closed" : "actioned";
                String summary = ViolationReport.STATUS_EXONERATED.equals(finalStatus)
                        ? "Violation report exonerated."
                        : "Violation report reviewed: " + finalStatus.replace('_', ' ') + ".";
                updateLinkedViolationAlert(alertStatus, summary, adminUid);
                showMessage("Review recorded.");
                notifyParentAndDismiss();
            });
        });
    }

    @NonNull
    private String resolveFinalStatus(@NonNull String guestAction, @NonNull String studentAction) {
        boolean guestPenalty = isGuestViolation && !ACTION_EXONERATE.equals(guestAction);
        boolean studentPenalty = ACTION_FINE.equals(studentAction);
        if (!guestPenalty && !studentPenalty) {
            return ViolationReport.STATUS_EXONERATED;
        }
        if (guestPenalty && studentPenalty) {
            return ViolationReport.STATUS_ACTIONED;
        }
        if (studentPenalty) {
            return ViolationReport.STATUS_FINED;
        }
        if (ACTION_BAN.equals(guestAction)) {
            return ViolationReport.STATUS_BANNED;
        }
        if (ACTION_WARN.equals(guestAction)) {
            return ViolationReport.STATUS_WARNING_ISSUED;
        }
        return ViolationReport.STATUS_ACTIONED;
    }

    @NonNull
    private String selectedGuestAction() {
        int checkedId = radioGuestAction.getCheckedRadioButtonId();
        if (checkedId == R.id.radio_guest_exonerate) return ACTION_EXONERATE;
        if (checkedId == R.id.radio_guest_warn) return ACTION_WARN;
        if (checkedId == R.id.radio_guest_ban) return ACTION_BAN;
        return "";
    }

    @NonNull
    private String selectedStudentAction() {
        int checkedId = radioStudentAction.getCheckedRadioButtonId();
        if (checkedId == R.id.radio_student_exonerate) return ACTION_EXONERATE;
        if (checkedId == R.id.radio_student_fine) return ACTION_FINE;
        return "";
    }

    @NonNull
    private String restoredGuestPassStatus(@NonNull String previousStatus) {
        String normalized = previousStatus.trim().toLowerCase(Locale.US);
        if ("active".equals(normalized)) {
            return "active";
        }
        if ("overdue".equals(normalized)) {
            return "exited";
        }
        if ("used".equals(normalized) || "reported".equals(normalized)) {
            return "used";
        }
        return "used";
    }

    @NonNull
    private String restoredEntryRequestStatus(
            @NonNull String restoredPassStatus,
            @NonNull String previousRequestStatus
    ) {
        String normalizedRequestStatus = previousRequestStatus.trim().toLowerCase(Locale.US);
        if ("pending".equals(normalizedRequestStatus)
                || "active".equals(normalizedRequestStatus)
                || "denied".equals(normalizedRequestStatus)
                || "overdue".equals(normalizedRequestStatus)
                || "exited".equals(normalizedRequestStatus)) {
            return normalizedRequestStatus;
        }

        String normalizedPassStatus = restoredPassStatus.trim().toLowerCase(Locale.US);
        if ("active".equals(normalizedPassStatus)) {
            return "pending";
        }
        if ("used".equals(normalizedPassStatus)) {
            return "active";
        }
        if ("exited".equals(normalizedPassStatus)) {
            return "exited";
        }
        return normalizedPassStatus;
    }

    @NonNull
    private String targetStudentUid() {
        return isGuestViolation ? sponsorUid : studentUid;
    }

    @NonNull
    private String targetStudentName() {
        return isGuestViolation ? sponsorName : studentName;
    }

    @NonNull
    private String targetStudentId() {
        return isGuestViolation ? sponsorStudentId : studentId;
    }

    private double parseAmount(@NonNull String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ignored) {
            return 0.0;
        }
    }

    private void updateLinkedViolationAlert(
            @NonNull String status,
            @NonNull String summary,
            @NonNull String adminUid
    ) {
        if (alertRepo == null || reportId.trim().isEmpty()) {
            return;
        }
        alertRepo.updateLinkedAlertStatus(
                "violationReportId",
                reportId,
                status,
                summary,
                adminUid,
                (success, message, exception) -> {
                    // Violation reports remain the source of truth; alert mirroring is best effort.
                }
        );
    }

    private void notifyParentAndDismiss() {
        Bundle result = new Bundle();
        getParentFragmentManager().setFragmentResult(RESULT_KEY, result);
        dismiss();
    }

    private void setReviewEnabled(boolean enabled) {
        buttonSubmitReview.setEnabled(enabled);
        radioGuestAction.setEnabled(enabled);
        radioStudentAction.setEnabled(enabled);
        inputGuestReason.setEnabled(enabled);
        inputStudentReason.setEnabled(enabled);
        inputStudentFineAmount.setEnabled(enabled);
        buttonGuestBanDate.setEnabled(enabled);
    }

    private void showMessage(@NonNull String message) {
        Snackbar.make(requireView(), message, Snackbar.LENGTH_SHORT).show();
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

    private interface ReviewStep {
        void run(@NonNull InterventionRepository.OperationCallback callback);
    }
}
