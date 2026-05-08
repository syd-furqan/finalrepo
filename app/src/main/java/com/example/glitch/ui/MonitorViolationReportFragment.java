package com.example.glitch.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.glitch.R;
import com.example.glitch.auth.SessionManager;
import com.example.glitch.data.RepositoryProvider;
import com.example.glitch.data.ViolationReportRepository;
import com.example.glitch.model.GuestIdentityPolicy;
import com.example.glitch.model.UserProfile;
import com.example.glitch.model.ViolationReport;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

public class MonitorViolationReportFragment extends Fragment {
    private ViolationReportRepository repository;

    private TextInputEditText inputDetail;
    private RadioGroup radioLevel;
    private RadioGroup radioSubjectType;
    private TextInputEditText inputGuestCnic;
    private LinearLayout containerVerifiedGuest;
    private TextView textVerifiedGuestName;
    private TextView textVerifiedSponsor;
    private TextInputEditText inputStudentEmail;
    private TextView textVerifiedStudentName;
    private LinearLayout containerSubjectToggle;
    private LinearLayout containerGuestVerify;
    private LinearLayout containerStudentVerify;
    private MaterialButton buttonSubmit;

    // Verified data
    private String verifiedGuestName = "";
    private String verifiedGuestPassId = "";
    private String verifiedSponsorUid = "";
    private String verifiedSponsorName = "";
    private String verifiedSponsorRole = "";
    private String verifiedStudentUid = "";
    private String verifiedStudentName = "";
    private boolean guestVerified = false;
    private boolean studentVerified = false;

    private final boolean isGuard;

    public MonitorViolationReportFragment() {
        this.isGuard = false;
    }

    private MonitorViolationReportFragment(boolean isGuard) {
        this.isGuard = isGuard;
    }

    @NonNull
    public static MonitorViolationReportFragment newInstance() {
        return new MonitorViolationReportFragment(false);
    }

    @NonNull
    public static MonitorViolationReportFragment newInstanceForGuard() {
        return new MonitorViolationReportFragment(true);
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

        inputDetail = view.findViewById(R.id.input_violation_detail);
        radioLevel = view.findViewById(R.id.radio_violation_level);
        radioSubjectType = view.findViewById(R.id.radio_subject_type);
        inputGuestCnic = view.findViewById(R.id.input_guest_cnic);
        containerVerifiedGuest = view.findViewById(R.id.container_verified_guest);
        textVerifiedGuestName = view.findViewById(R.id.text_verified_guest_name);
        textVerifiedSponsor = view.findViewById(R.id.text_verified_sponsor);
        inputStudentEmail = view.findViewById(R.id.input_student_email);
        textVerifiedStudentName = view.findViewById(R.id.text_verified_student_name);
        containerSubjectToggle = view.findViewById(R.id.container_subject_toggle);
        containerGuestVerify = view.findViewById(R.id.container_guest_verify);
        containerStudentVerify = view.findViewById(R.id.container_student_verify);
        buttonSubmit = view.findViewById(R.id.button_submit_report);

        MaterialButton buttonVerifyGuest = view.findViewById(R.id.button_verify_guest);
        MaterialButton buttonVerifyStudent = view.findViewById(R.id.button_verify_student);

        GuestIdentityInputSupport.attachCnicFormatter(inputGuestCnic);
        RoleNavRouter.bindBottomNav(view, this, RoleDestination.DASHBOARD);

        if (isGuard) {
            containerSubjectToggle.setVisibility(View.VISIBLE);
            radioSubjectType.setOnCheckedChangeListener((group, checkedId) -> {
                boolean isGuardSubjectGuest = checkedId == R.id.radio_subject_guest;
                containerGuestVerify.setVisibility(isGuardSubjectGuest ? View.VISIBLE : View.GONE);
                containerStudentVerify.setVisibility(isGuardSubjectGuest ? View.GONE : View.VISIBLE);
                resetVerification();
                updateSubmitState();
            });
        }

        buttonVerifyGuest.setOnClickListener(v -> verifyGuest());
        buttonVerifyStudent.setOnClickListener(v -> verifyStudent());
        buttonSubmit.setOnClickListener(v -> submitReport());
    }

    private void verifyGuest() {
        String rawCnic = read(inputGuestCnic);
        String normalized = GuestIdentityPolicy.normalizeCnic(rawCnic);
        if (normalized == null) {
            Snackbar.make(requireView(), "Enter CNIC as xxxxx-xxxxxxx-x", Snackbar.LENGTH_SHORT).show();
            return;
        }
        repository.findActivePasForCnic(normalized, new ViolationReportRepository.PassInfoCallback() {
            @Override
            public void onFound(@NonNull String guestName, @NonNull String guestPassId, @NonNull String sponsorUid, @NonNull String sponsorName, @NonNull String sponsorRole) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    verifiedGuestName = guestName;
                    verifiedGuestPassId = guestPassId;
                    verifiedSponsorUid = sponsorUid;
                    verifiedSponsorName = sponsorName;
                    verifiedSponsorRole = sponsorRole;
                    guestVerified = true;
                    textVerifiedGuestName.setText("Guest: " + guestName);
                    textVerifiedSponsor.setText("Sponsor: " + sponsorName + " (" + sponsorRole + ")");
                    containerVerifiedGuest.setVisibility(View.VISIBLE);
                    updateSubmitState();
                });
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

    private void verifyStudent() {
        String email = read(inputStudentEmail);
        if (email.isEmpty()) {
            Snackbar.make(requireView(), "Enter student email.", Snackbar.LENGTH_SHORT).show();
            return;
        }
        repository.findStudentByEmail(email, new ViolationReportRepository.StudentInfoCallback() {
            @Override
            public void onFound(@NonNull String studentUid, @NonNull String studentName) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    verifiedStudentUid = studentUid;
                    verifiedStudentName = studentName;
                    studentVerified = true;
                    textVerifiedStudentName.setText("Student: " + studentName);
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
        boolean subjectVerified;
        if (isGuard) {
            boolean isGuestSubject = radioSubjectType.getCheckedRadioButtonId() == R.id.radio_subject_guest;
            subjectVerified = isGuestSubject ? guestVerified : studentVerified;
        } else {
            subjectVerified = guestVerified;
        }
        buttonSubmit.setEnabled(subjectVerified);
        buttonSubmit.setAlpha(subjectVerified ? 1.0f : 0.5f);
    }

    private void resetVerification() {
        guestVerified = false;
        studentVerified = false;
        verifiedGuestName = "";
        verifiedGuestPassId = "";
        verifiedSponsorUid = "";
        verifiedSponsorName = "";
        verifiedSponsorRole = "";
        verifiedStudentUid = "";
        verifiedStudentName = "";
        containerVerifiedGuest.setVisibility(View.GONE);
        textVerifiedStudentName.setVisibility(View.GONE);
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

        boolean isGuestSubject = !isGuard || (radioSubjectType.getCheckedRadioButtonId() == R.id.radio_subject_guest);
        String subjectType = isGuestSubject ? ViolationReport.SUBJECT_GUEST : ViolationReport.SUBJECT_STUDENT;
        String rawCnic = isGuestSubject ? GuestIdentityPolicy.normalizeCnicOrEmpty(read(inputGuestCnic)) : "";

        buttonSubmit.setEnabled(false);
        repository.submitReport(
                profile.getUid(),
                profile.getRole(),
                profile.getDisplayName(),
                detail,
                level,
                subjectType,
                rawCnic,
                verifiedGuestName,
                verifiedGuestPassId,
                verifiedSponsorUid,
                verifiedSponsorName,
                verifiedSponsorRole,
                verifiedStudentUid,
                verifiedStudentName,
                isGuard ? read(inputStudentEmail) : "",
                (success, message, exception) -> {
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() -> {
                        Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG).show();
                        if (success) {
                            inputDetail.setText("");
                            radioLevel.clearCheck();
                            inputGuestCnic.setText("");
                            if (inputStudentEmail != null) inputStudentEmail.setText("");
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
