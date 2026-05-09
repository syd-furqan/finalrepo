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
    private LinearLayout containerGuestVerify;
    private LinearLayout containerGuestCnicLookup;
    private LinearLayout containerGuestPasscodeLookup;
    private LinearLayout containerStudentVerify;
    private MaterialButton buttonSubmit;
    private ActivityResultLauncher<ScanOptions> barcodeLauncher;

    private String verifiedGuestName = "";
    private String verifiedGuestCnic = "";
    private String verifiedGuestPhone = "";
    private String verifiedGuestPassId = "";
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
        MaterialButton buttonScanGuestQr = view.findViewById(R.id.button_scan_guest_qr);

        GuestIdentityInputSupport.attachCnicFormatter(inputGuestCnic);
        inputGuestPassCode.setFilters(new InputFilter[]{
                new InputFilter.AllCaps(),
                new InputFilter.LengthFilter(32)
        });
        RoleNavRouter.bindBottomNav(view, this, RoleDestination.DASHBOARD);

        barcodeLauncher = registerForActivityResult(new ScanContract(), result -> {
            if (result == null || result.getContents() == null || result.getContents().trim().isEmpty()) {
                Snackbar.make(requireView(), "Unable to read QR code.", Snackbar.LENGTH_SHORT).show();
                return;
            }
            String scanned = result.getContents().trim().toUpperCase();
            inputGuestPassCode.setText(scanned);
            radioVisitorLookupMethod.check(R.id.radio_lookup_qr);
            updateVisitorLookupMethod();
            verifyGuestByPassCode(scanned);
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
                        entryRequestId,
                        passStatus,
                        sponsorUid,
                        sponsorName,
                        sponsorRole,
                        sponsorStudentId
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
            Snackbar.make(requireView(), "Enter a passcode or scan a QR code.", Snackbar.LENGTH_SHORT).show();
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
                            pass.getEntryRequestId(),
                            pass.getStatus(),
                            pass.getSponsorUid(),
                            pass.getSponsorName(),
                            pass.getSponsorRole(),
                            pass.getSponsorStudentId()
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
            @NonNull String entryRequestId,
            @NonNull String passStatus,
            @NonNull String sponsorUid,
            @NonNull String sponsorName,
            @NonNull String sponsorRole,
            @NonNull String sponsorStudentId
    ) {
        verifiedGuestName = guestName;
        verifiedGuestCnic = guestCnic;
        verifiedGuestPhone = guestPhone;
        verifiedGuestPassId = guestPassId;
        verifiedEntryRequestId = entryRequestId;
        verifiedGuestPassStatus = passStatus;
        verifiedSponsorUid = sponsorUid;
        verifiedSponsorName = sponsorName;
        verifiedSponsorRole = sponsorRole;
        verifiedSponsorStudentId = sponsorStudentId;
        guestVerified = true;
        String phoneText = guestPhone.trim().isEmpty() ? "" : " | Phone: " + guestPhone;
        textVerifiedGuestName.setText("Visitor: " + guestName + " | CNIC: " + guestCnic + phoneText);
        textVerifiedSponsor.setText("Sponsor: " + sponsorName + " (" + sponsorRole + ")");
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
            public void onFound(@NonNull String studentUid, @NonNull String studentName, @NonNull String studentEmail, @NonNull String foundStudentId) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    verifiedStudentUid = studentUid;
                    verifiedStudentName = studentName;
                    verifiedStudentEmail = studentEmail;
                    verifiedStudentId = foundStudentId;
                    studentVerified = true;
                    textVerifiedStudentName.setText("Student: " + studentName + " | ID: " + foundStudentId);
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
        RadioButton selectedLevel = requireView().findViewById(checkedId);
        String level = selectedLevel.getText().toString().split(" ")[0].toLowerCase();

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
}
