package com.example.glitch.ui;

import android.os.Bundle;
import android.text.InputFilter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.glitch.R;
import com.example.glitch.auth.SessionManager;
import com.example.glitch.data.GuestPassRepository;
import com.example.glitch.data.RepositoryProvider;
import com.example.glitch.data.ViolationReportRepository;
import com.example.glitch.model.GuestIdentityPolicy;
import com.example.glitch.model.GuestPass;
import com.example.glitch.model.UserProfile;
import com.example.glitch.model.ViolationReport;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MonitorViolationReportFragment extends Fragment {
    private ViolationReportRepository repository;
    private GuestPassRepository guestPassRepository;

    private TextInputEditText inputDetail;
    private RadioGroup radioLevel;
    private RadioGroup radioSubjectType;
    private RadioGroup radioVisitorLookupMethod;
    private TextInputEditText inputGuestCnic;
    private TextInputEditText inputGuestPassCode;
    private LinearLayout containerVerifiedGuest;
    private TextView textVerifiedGuestName;
    private TextView textVerifiedSponsor;
    private TextInputEditText inputStudentId;
    private TextView textVerifiedStudentName;
    private View containerGuestVerify;
    private LinearLayout containerGuestCnicLookup;
    private LinearLayout containerGuestPasscodeLookup;
    private View containerStudentVerify;
    private MaterialButton buttonSubmit;
    private ActivityResultLauncher<ScanOptions> barcodeLauncher;

    private String verifiedGuestName = "";
    private String verifiedGuestCnic = "";
    private String verifiedGuestPhone = "";
    private String verifiedGuestPassId = "";
    private String verifiedGuestPassCode = "";
    private String verifiedEntryRequestId = "";
    private String verifiedGuestPassStatus = "";
    private String verifiedSponsorUid = "";
    private String verifiedSponsorName = "";
    private String verifiedSponsorRole = "";
    private String verifiedSponsorStudentId = "";
    private String verifiedStudentUid = "";
    private String verifiedStudentName = "";
    private String verifiedStudentEmail = "";
    private String verifiedStudentId = "";
    private boolean guestVerified = false;
    private boolean studentVerified = false;
    private final SimpleDateFormat dateTimeFormat = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());

    @NonNull
    public static MonitorViolationReportFragment newInstance() {
        return new MonitorViolationReportFragment();
    }

    @NonNull
    public static MonitorViolationReportFragment newInstanceForGuard() {
        return new MonitorViolationReportFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_monitor_violation_report, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = RepositoryProvider.getViolationReportRepository();
        guestPassRepository = RepositoryProvider.getGuestPassRepository();

        inputDetail = view.findViewById(R.id.input_violation_detail);
        radioLevel = view.findViewById(R.id.radio_violation_level);
        radioSubjectType = view.findViewById(R.id.radio_subject_type);
        radioVisitorLookupMethod = view.findViewById(R.id.radio_visitor_lookup_method);
        inputGuestCnic = view.findViewById(R.id.input_guest_cnic);
        inputGuestPassCode = view.findViewById(R.id.input_guest_pass_code);
        containerVerifiedGuest = view.findViewById(R.id.container_verified_guest);
        textVerifiedGuestName = view.findViewById(R.id.text_verified_guest_name);
        textVerifiedSponsor = view.findViewById(R.id.text_verified_sponsor);
        inputStudentId = view.findViewById(R.id.input_student_email);
        textVerifiedStudentName = view.findViewById(R.id.text_verified_student_name);
        containerGuestVerify = view.findViewById(R.id.container_guest_verify);
        containerGuestCnicLookup = view.findViewById(R.id.container_guest_cnic_lookup);
        containerGuestPasscodeLookup = view.findViewById(R.id.container_guest_passcode_lookup);
        containerStudentVerify = view.findViewById(R.id.container_student_verify);
        buttonSubmit = view.findViewById(R.id.button_submit_report);

        MaterialButton buttonVerifyGuest = view.findViewById(R.id.button_verify_guest);
        MaterialButton buttonVerifyGuestPasscode = view.findViewById(R.id.button_verify_guest_passcode);
        MaterialButton buttonVerifyStudent = view.findViewById(R.id.button_verify_student);
        View buttonScanGuestQr = view.findViewById(R.id.button_scan_guest_qr);

        GuestIdentityInputSupport.attachCnicFormatter(inputGuestCnic);
        inputGuestPassCode.setFilters(new InputFilter[]{
                new InputFilter.AllCaps(),
                new InputFilter.LengthFilter(32)
        });
        RoleNavRouter.bindBottomNav(view, this, RoleDestination.MONITOR_REPORT);

        barcodeLauncher = registerForActivityResult(new ScanContract(), result -> {
            if (result == null || result.getContents() == null || result.getContents().trim().isEmpty()) {
                Snackbar.make(requireView(), "Unable to read QR code.", Snackbar.LENGTH_SHORT).show();
                return;
            }
            String scanned = result.getContents().trim().toUpperCase();
            inputGuestPassCode.setText(scanned);
            updateVisitorLookupMethod();
            Snackbar.make(requireView(), "Pass code filled from QR. Tap Verify to continue.", Snackbar.LENGTH_SHORT).show();
        });

        radioSubjectType.setOnCheckedChangeListener((group, checkedId) -> {
            boolean visitor = checkedId == R.id.radio_subject_guest;
            containerGuestVerify.setVisibility(visitor ? View.VISIBLE : View.GONE);
            containerStudentVerify.setVisibility(visitor ? View.GONE : View.VISIBLE);
            resetVerification();
            updateSubmitState();
        });
        radioVisitorLookupMethod.setOnCheckedChangeListener((group, checkedId) -> {
            updateVisitorLookupMethod();
            resetVerification();
            updateSubmitState();
        });
        updateVisitorLookupMethod();

        buttonVerifyGuest.setOnClickListener(v -> verifyGuest());
        buttonVerifyGuestPasscode.setOnClickListener(v -> verifyGuest());
        buttonVerifyStudent.setOnClickListener(v -> verifyStudent());
        buttonScanGuestQr.setOnClickListener(v -> scanVisitorQr());
        buttonSubmit.setOnClickListener(v -> submitReport());
    }

    private void verifyGuest() {
        if (radioVisitorLookupMethod.getCheckedRadioButtonId() == R.id.radio_lookup_cnic) {
            verifyGuestByCnic();
            return;
        }
        verifyGuestByPassCode(read(inputGuestPassCode));
    }

    private void verifyGuestByCnic() {
        String normalized = GuestIdentityPolicy.normalizeCnic(read(inputGuestCnic));
        if (normalized == null) {
            Snackbar.make(requireView(), "Enter CNIC as xxxxx-xxxxxxx-x", Snackbar.LENGTH_SHORT).show();
            return;
        }
        repository.findActivePasForCnic(normalized, new ViolationReportRepository.PassInfoCallback() {
            @Override
            public void onFound(
                    @NonNull String guestName,
                    @NonNull String guestPhone,
                    @NonNull String guestPassId,
                    @NonNull String entryRequestId,
                    @NonNull String passStatus,
                    @NonNull String sponsorUid,
                    @NonNull String sponsorName,
                    @NonNull String sponsorRole,
                    @NonNull String sponsorStudentId
            ) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> bindVerifiedGuest(
                        guestName,
                        guestPhone,
                        normalized,
                        guestPassId,
                        "",
                        entryRequestId,
                        passStatus,
                        sponsorUid,
                        sponsorName,
                        sponsorRole,
                        "",
                        sponsorStudentId,
                        false,
                        null,
                        null
                ));
            }

            @Override
            public void onNotFound(@NonNull String message) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    guestVerified = false;
                    containerVerifiedGuest.setVisibility(View.GONE);
                    Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG).show();
                    updateSubmitState();
                });
            }

            @Override
            public void onError(@NonNull Exception exception) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> Snackbar.make(requireView(), "Verification failed. Try again.", Snackbar.LENGTH_LONG).show());
            }
        });
    }

    private void verifyGuestByPassCode(@NonNull String rawPassCode) {
        String passCode = rawPassCode.trim().toUpperCase();
        if (passCode.isEmpty()) {
            Snackbar.make(requireView(), "Enter pass code or scan QR, then tap Verify.", Snackbar.LENGTH_SHORT).show();
            return;
        }
        guestPassRepository.findPassByCode(passCode, new GuestPassRepository.PassLookupListener() {
            @Override
            public void onData(@Nullable GuestPass pass) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    if (pass == null) {
                        guestVerified = false;
                        containerVerifiedGuest.setVisibility(View.GONE);
                        Snackbar.make(requireView(), "No visitor pass found for this code.", Snackbar.LENGTH_LONG).show();
                        updateSubmitState();
                        return;
                    }
                    bindVerifiedGuest(
                            pass.getGuestName(),
                            pass.getGuestPhone(),
                            pass.getGuestIdNumber(),
                            pass.getId(),
                            pass.getPassCode(),
                            pass.getEntryRequestId(),
                            pass.getStatus(),
                            pass.getSponsorUid(),
                            pass.getSponsorName(),
                            pass.getSponsorRole(),
                            pass.getSponsorEmail(),
                            pass.getSponsorStudentId(),
                            pass.hasVehicle(),
                            pass.getCreatedAt(),
                            pass.getExpiresAt()
                    );
                });
            }

            @Override
            public void onError(@NonNull Exception exception) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> Snackbar.make(requireView(), "Verification failed. Try again.", Snackbar.LENGTH_LONG).show());
            }
        });
    }

    private void bindVerifiedGuest(
            @NonNull String guestName,
            @NonNull String guestPhone,
            @NonNull String guestCnic,
            @NonNull String guestPassId,
            @NonNull String guestPassCode,
            @NonNull String entryRequestId,
            @NonNull String passStatus,
            @NonNull String sponsorUid,
            @NonNull String sponsorName,
            @NonNull String sponsorRole,
            @NonNull String sponsorEmail,
            @NonNull String sponsorStudentId,
            boolean hasVehicle,
            @Nullable Timestamp createdAt,
            @Nullable Timestamp expiresAt
    ) {
        verifiedGuestName = guestName;
        verifiedGuestCnic = guestCnic;
        verifiedGuestPhone = guestPhone;
        verifiedGuestPassId = guestPassId;
        verifiedGuestPassCode = guestPassCode;
        verifiedEntryRequestId = entryRequestId;
        verifiedGuestPassStatus = passStatus;
        verifiedSponsorUid = sponsorUid;
        verifiedSponsorName = sponsorName;
        verifiedSponsorRole = sponsorRole;
        verifiedSponsorStudentId = sponsorStudentId;
        guestVerified = true;
        String resolvedPassCode = guestPassCode.trim().isEmpty() ? read(inputGuestPassCode) : guestPassCode.trim();
        textVerifiedGuestName.setText(
                "Visitor: " + valueOr(guestName, "N/A") + "\n"
                        + "Pass Code: " + valueOr(resolvedPassCode, "N/A") + "\n"
                        + "CNIC: " + valueOr(guestCnic, "N/A") + "\n"
                        + "Phone: " + valueOr(guestPhone, "N/A") + "\n"
                        + "Has Vehicle: " + (hasVehicle ? "Yes" : "No") + "\n"
                        + "Created At: " + formatTimestamp(createdAt)
        );
        textVerifiedSponsor.setText(
                "Sponsor: " + valueOr(sponsorName, "N/A") + "\n"
                        + "Sponsor Type: " + formatRoleLabel(sponsorRole) + "\n"
                        + "Sponsor Email: " + valueOr(sponsorEmail, "N/A")
        );
        containerVerifiedGuest.setVisibility(View.VISIBLE);
        updateSubmitState();
    }

    private void scanVisitorQr() {
        ScanOptions options = new ScanOptions();
        options.setPrompt("Point camera at visitor QR code");
        options.setBeepEnabled(true);
        options.setOrientationLocked(true);
        barcodeLauncher.launch(options);
    }

    private void verifyStudent() {
        String studentId = read(inputStudentId);
        if (studentId.isEmpty()) {
            Snackbar.make(requireView(), "Enter student ID.", Snackbar.LENGTH_SHORT).show();
            return;
        }
        repository.findStudentByStudentId(studentId, new ViolationReportRepository.StudentInfoCallback() {
            @Override
            public void onFound(
                    @NonNull String studentUid,
                    @NonNull String studentName,
                    @NonNull String studentEmail,
                    @NonNull String foundStudentId,
                    @NonNull String studentType
            ) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    verifiedStudentUid = studentUid;
                    verifiedStudentName = studentName;
                    verifiedStudentEmail = studentEmail;
                    verifiedStudentId = foundStudentId;
                    studentVerified = true;
                    textVerifiedStudentName.setText(
                            "Student: " + valueOr(studentName, "N/A") + "\n"
                                    + "Student Type: " + formatStudentType(studentType)
                    );
                    textVerifiedStudentName.setVisibility(View.VISIBLE);
                    updateSubmitState();
                });
            }

            @Override
            public void onNotFound(@NonNull String message) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    studentVerified = false;
                    textVerifiedStudentName.setVisibility(View.GONE);
                    Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG).show();
                    updateSubmitState();
                });
            }

            @Override
            public void onError(@NonNull Exception exception) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> Snackbar.make(requireView(), "Lookup failed. Try again.", Snackbar.LENGTH_LONG).show());
            }
        });
    }

    private void updateSubmitState() {
        boolean visitor = radioSubjectType.getCheckedRadioButtonId() == R.id.radio_subject_guest;
        boolean subjectVerified = visitor ? guestVerified : studentVerified;
        buttonSubmit.setEnabled(subjectVerified);
        buttonSubmit.setAlpha(subjectVerified ? 1.0f : 0.5f);
    }

    private void resetVerification() {
        guestVerified = false;
        studentVerified = false;
        verifiedGuestName = "";
        verifiedGuestCnic = "";
        verifiedGuestPhone = "";
        verifiedGuestPassId = "";
        verifiedGuestPassCode = "";
        verifiedEntryRequestId = "";
        verifiedGuestPassStatus = "";
        verifiedSponsorUid = "";
        verifiedSponsorName = "";
        verifiedSponsorRole = "";
        verifiedSponsorStudentId = "";
        verifiedStudentUid = "";
        verifiedStudentName = "";
        verifiedStudentEmail = "";
        verifiedStudentId = "";
        containerVerifiedGuest.setVisibility(View.GONE);
        textVerifiedStudentName.setVisibility(View.GONE);
    }

    private void updateVisitorLookupMethod() {
        boolean cnicLookup = radioVisitorLookupMethod.getCheckedRadioButtonId() == R.id.radio_lookup_cnic;
        containerGuestCnicLookup.setVisibility(cnicLookup ? View.VISIBLE : View.GONE);
        containerGuestPasscodeLookup.setVisibility(cnicLookup ? View.GONE : View.VISIBLE);
    }

    private void submitReport() {
        String detail = read(inputDetail);
        if (detail.isEmpty()) {
            Snackbar.make(requireView(), "Enter violation details.", Snackbar.LENGTH_SHORT).show();
            return;
        }
        int checkedId = radioLevel.getCheckedRadioButtonId();
        if (checkedId == -1) {
            Snackbar.make(requireView(), "Select a violation level.", Snackbar.LENGTH_SHORT).show();
            return;
        }
        String level;
        if (checkedId == R.id.radio_minor) {
            level = "minor";
        } else if (checkedId == R.id.radio_moderate) {
            level = "moderate";
        } else {
            level = "severe";
        }

        UserProfile profile = SessionManager.getCurrentProfile();
        if (profile == null) return;

        boolean visitor = radioSubjectType.getCheckedRadioButtonId() == R.id.radio_subject_guest;
        String subjectType = visitor ? ViolationReport.SUBJECT_GUEST : ViolationReport.SUBJECT_STUDENT;

        buttonSubmit.setEnabled(false);
        repository.submitReport(
                profile.getUid(),
                profile.getRole(),
                profile.getDisplayName(),
                detail,
                level,
                subjectType,
                visitor ? verifiedGuestCnic : "",
                verifiedGuestName,
                verifiedGuestPhone,
                verifiedGuestPassId,
                verifiedEntryRequestId,
                verifiedGuestPassStatus,
                verifiedSponsorUid,
                verifiedSponsorName,
                verifiedSponsorRole,
                verifiedSponsorStudentId,
                verifiedStudentUid,
                verifiedStudentName,
                verifiedStudentEmail,
                verifiedStudentId,
                (success, message, exception) -> {
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() -> {
                        Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG).show();
                        if (success) {
                            inputDetail.setText("");
                            radioLevel.clearCheck();
                            inputGuestCnic.setText("");
                            inputGuestPassCode.setText("");
                            inputStudentId.setText("");
                            resetVerification();
                        }
                        buttonSubmit.setEnabled(false);
                        buttonSubmit.setAlpha(0.5f);
                    });
                }
        );
    }

    @NonNull
    private String read(@NonNull TextInputEditText input) {
        CharSequence v = input.getText();
        return v == null ? "" : v.toString().trim();
    }

    @NonNull
    private String formatTimestamp(@Nullable Timestamp timestamp) {
        if (timestamp == null) {
            return "N/A";
        }
        Date date = timestamp.toDate();
        return dateTimeFormat.format(date);
    }

    @NonNull
    private String valueOr(@Nullable String value, @NonNull String fallback) {
        if (value == null) {
            return fallback;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? fallback : trimmed;
    }

    @NonNull
    private String formatRoleLabel(@Nullable String role) {
        String normalized = valueOr(role, "").toLowerCase();
        if (normalized.isEmpty()) {
            return "N/A";
        }
        if (normalized.length() == 1) {
            return normalized.toUpperCase();
        }
        return normalized.substring(0, 1).toUpperCase() + normalized.substring(1);
    }

    @NonNull
    private String formatStudentType(@Nullable String type) {
        String normalized = valueOr(type, "").toLowerCase();
        if (normalized.isEmpty()) {
            return "N/A";
        }
        if ("day_scholar".equals(normalized)) {
            return "Day Scholar";
        }
        if ("hostelite".equals(normalized)) {
            return "Hostelite";
        }
        if ("redc".equals(normalized)) {
            return "REDC";
        }
        if (normalized.length() == 1) {
            return normalized.toUpperCase();
        }
        return normalized.substring(0, 1).toUpperCase() + normalized.substring(1);
    }
}
