package com.example.glitch.ui;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.glitch.R;
import com.example.glitch.data.RepositoryProvider;
import com.example.glitch.data.VehicleRequestRepository;
import com.example.glitch.model.GuestIdentityPolicy;
import com.example.glitch.model.RegisteredVehicleRecord;
import com.example.glitch.model.UserProfile;
import com.example.glitch.model.VehicleDocumentInput;
import com.example.glitch.model.VehicleDocumentRef;
import com.example.glitch.model.VehicleRemovalDraft;
import com.example.glitch.model.VehicleRequestRecord;
import com.example.glitch.model.VehicleRegistrationDraft;
import com.example.glitch.model.VehicleStickerPolicy;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageException;
import com.google.firebase.storage.StorageReference;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

/**
 * Shared vehicle-program screen for student and faculty sponsors.
 */
public class SponsorVehicleRegistrationFragment extends Fragment implements RegisteredVehicleAdapter.ActionListener, VehicleApplicationAdapter.ActionListener {
    private static final String ARG_TARGET_REQUEST_ID = "target_request_id";

    private enum PickTarget {
        APPLICANT_CNIC,
        REGISTRATION,
        OWNER_CNIC,
        EVIDENCE
    }

    private VehicleRequestRepository repository;
    private RegisteredVehicleAdapter registeredVehicleAdapter;
    private VehicleApplicationAdapter historyAdapter;

    private TextView textPolicySummary;
    private TextView textOpenApplication;
    private MaterialButton buttonCancelOpenApplication;
    private TextInputEditText inputPlate;
    private TextInputEditText inputMake;
    private TextInputEditText inputModel;
    private TextInputEditText inputVariant;
    private MaterialCheckBox checkIsOwner;
    private MaterialButton buttonPickApplicantCnic;
    private MaterialButton buttonPickRegistration;
    private MaterialButton buttonPickOwnerCnic;
    private MaterialButton buttonRemoveApplicantCnic;
    private MaterialButton buttonRemoveRegistration;
    private MaterialButton buttonRemoveOwnerCnic;
    private TextView textApplicantCnic;
    private TextView textRegistrationDoc;
    private TextView textOwnerCnic;
    private MaterialButton buttonSubmitRegistration;
    private LinearProgressIndicator progressRegistrationUpload;
    private TextView textRegistrationUploadProgress;
    private TextView textRegisteredEmpty;
    private TextView textHistoryEmpty;
    private RecyclerView historyRecycler;

    private View layoutRemoval;
    private TextView textRemovalVehicle;
    private TextInputEditText inputRemovalReason;
    private MaterialButton buttonAddEvidence;
    private MaterialButton buttonClearEvidence;
    private TextView textEvidence;
    private MaterialButton buttonSubmitRemoval;

    private VehicleDocumentInput applicantCnicDoc;
    private VehicleDocumentInput registrationDoc;
    private VehicleDocumentInput ownerCnicDoc;
    private final List<VehicleDocumentInput> removalEvidenceDocs = new ArrayList<>();
    private RegisteredVehicleRecord selectedVehicleForRemoval;
    private VehicleRequestRecord openRequest;
    private String targetRequestId = "";
    private boolean registrationUploadInProgress = false;

    private PickTarget currentPickTarget;
    private ActivityResultLauncher<String[]> documentPicker;

    @NonNull
    public static SponsorVehicleRegistrationFragment newInstance() {
        return new SponsorVehicleRegistrationFragment();
    }

    @NonNull
    public static SponsorVehicleRegistrationFragment newInstance(@NonNull String targetRequestId) {
        SponsorVehicleRegistrationFragment fragment = new SponsorVehicleRegistrationFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TARGET_REQUEST_ID, targetRequestId.trim());
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
        return inflater.inflate(R.layout.fragment_sponsor_vehicle_registration, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = RepositoryProvider.getVehicleRequestRepository();
        Bundle args = getArguments();
        targetRequestId = args == null ? "" : safe(args.getString(ARG_TARGET_REQUEST_ID));

        textPolicySummary = view.findViewById(R.id.text_vehicle_policy_summary);
        textOpenApplication = view.findViewById(R.id.text_vehicle_open_application);
        buttonCancelOpenApplication = view.findViewById(R.id.button_cancel_open_vehicle_application);
        inputPlate = view.findViewById(R.id.input_vehicle_plate);
        inputMake = view.findViewById(R.id.input_vehicle_make);
        inputModel = view.findViewById(R.id.input_vehicle_model);
        inputVariant = view.findViewById(R.id.input_vehicle_variant);
        checkIsOwner = view.findViewById(R.id.check_is_owner);
        buttonPickApplicantCnic = view.findViewById(R.id.button_pick_applicant_cnic);
        buttonPickRegistration = view.findViewById(R.id.button_pick_registration_doc);
        buttonPickOwnerCnic = view.findViewById(R.id.button_pick_owner_cnic);
        buttonRemoveApplicantCnic = view.findViewById(R.id.button_remove_applicant_cnic);
        buttonRemoveRegistration = view.findViewById(R.id.button_remove_registration_doc);
        buttonRemoveOwnerCnic = view.findViewById(R.id.button_remove_owner_cnic);
        textApplicantCnic = view.findViewById(R.id.text_applicant_cnic_file);
        textRegistrationDoc = view.findViewById(R.id.text_registration_file);
        textOwnerCnic = view.findViewById(R.id.text_owner_cnic_file);
        buttonSubmitRegistration = view.findViewById(R.id.button_submit_vehicle_registration);
        progressRegistrationUpload = view.findViewById(R.id.progress_vehicle_registration_upload);
        textRegistrationUploadProgress = view.findViewById(R.id.text_vehicle_registration_upload_progress);
        textRegisteredEmpty = view.findViewById(R.id.text_registered_vehicles_empty);
        textHistoryEmpty = view.findViewById(R.id.text_vehicle_history_empty);

        layoutRemoval = view.findViewById(R.id.layout_removal_request);
        textRemovalVehicle = view.findViewById(R.id.text_selected_vehicle_for_removal);
        inputRemovalReason = view.findViewById(R.id.input_vehicle_removal_reason);
        buttonAddEvidence = view.findViewById(R.id.button_add_removal_evidence);
        buttonClearEvidence = view.findViewById(R.id.button_clear_removal_evidence);
        textEvidence = view.findViewById(R.id.text_removal_evidence_files);
        buttonSubmitRemoval = view.findViewById(R.id.button_submit_vehicle_removal);

        RecyclerView registeredRecycler = view.findViewById(R.id.recycler_registered_vehicles);
        historyRecycler = view.findViewById(R.id.recycler_vehicle_history);
        registeredVehicleAdapter = new RegisteredVehicleAdapter(this);
        historyAdapter = new VehicleApplicationAdapter(this);
        registeredRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        historyRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        registeredRecycler.setAdapter(registeredVehicleAdapter);
        historyRecycler.setAdapter(historyAdapter);

        RoleNavRouter.bindBottomNav(view, this, RoleDestination.SPONSOR_VEHICLES);
        GuestIdentityInputSupport.attachVehiclePlateFormatter(inputPlate);

        documentPicker = registerForActivityResult(new ActivityResultContracts.OpenDocument(), this::handleDocumentPicked);

        buttonPickApplicantCnic.setOnClickListener(v -> launchPicker(PickTarget.APPLICANT_CNIC));
        buttonPickRegistration.setOnClickListener(v -> launchPicker(PickTarget.REGISTRATION));
        buttonPickOwnerCnic.setOnClickListener(v -> launchPicker(PickTarget.OWNER_CNIC));
        buttonAddEvidence.setOnClickListener(v -> launchPicker(PickTarget.EVIDENCE));
        buttonRemoveApplicantCnic.setOnClickListener(v -> {
            applicantCnicDoc = null;
            textApplicantCnic.setText("No file selected");
            updateAttachmentRemoveButtons();
        });
        buttonRemoveRegistration.setOnClickListener(v -> {
            registrationDoc = null;
            textRegistrationDoc.setText("No file selected");
            updateAttachmentRemoveButtons();
        });
        buttonRemoveOwnerCnic.setOnClickListener(v -> {
            ownerCnicDoc = null;
            textOwnerCnic.setText("No file selected");
            updateAttachmentRemoveButtons();
        });
        buttonClearEvidence.setOnClickListener(v -> {
            removalEvidenceDocs.clear();
            updateEvidenceText();
        });
        checkIsOwner.setOnCheckedChangeListener((buttonView, isChecked) -> updateOwnerDocVisibility());
        buttonSubmitRegistration.setOnClickListener(v -> submitRegistrationApplication());
        buttonCancelOpenApplication.setOnClickListener(v -> cancelOpenApplication());
        buttonSubmitRemoval.setOnClickListener(v -> submitRemovalApplication());

        updateOwnerDocVisibility();
        updateEvidenceText();
        updateAttachmentRemoveButtons();

        UserProfile profile = AuthUiGuard.requireProfile(this);
        if (profile == null) {
            return;
        }
        bindPolicyText(profile);
        bindDataStreams(profile);
    }

    private void bindPolicyText(@NonNull UserProfile profile) {
        String role = profile.getRole().trim().toLowerCase(Locale.getDefault());
        if ("student".equals(role)) {
            String category = VehicleStickerPolicy.normalizeStudentCategory(profile.getStudentCategory());
            String sticker = VehicleStickerPolicy.resolveStickerType(role, category);
            if (sticker.isEmpty()) {
                textPolicySummary.setText("Student category missing. Ask admin to set day_scholar, hostelite, or redc before applying.");
                return;
            }
            textPolicySummary.setText("Student category: " + category + "  •  Sticker: " + sticker + "  •  Max 2 active vehicles.");
            return;
        }
        textPolicySummary.setText("Faculty sticker: FC  •  Unlimited active vehicles.");
    }

    private void bindDataStreams(@NonNull UserProfile profile) {
        String uid = profile.getUid();
        repository.listenOpenVehicleRequest(uid, new VehicleRequestRepository.SingleRequestListener() {
            @Override
            public void onData(@Nullable VehicleRequestRecord request) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> bindOpenApplication(request));
            }

            @Override
            public void onError(@NonNull Exception exception) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> Snackbar.make(requireView(), "Failed to load open application", Snackbar.LENGTH_LONG).show());
            }
        });

        repository.listenRegisteredVehicles(uid, new VehicleRequestRepository.RegisteredVehicleListListener() {
            @Override
            public void onData(@NonNull List<RegisteredVehicleRecord> vehicles) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    List<RegisteredVehicleRecord> active = new ArrayList<>();
                    for (RegisteredVehicleRecord vehicle : vehicles) {
                        if ("active".equalsIgnoreCase(vehicle.getStatus())) {
                            active.add(vehicle);
                        }
                    }
                    registeredVehicleAdapter.submitList(active);
                    textRegisteredEmpty.setVisibility(active.isEmpty() ? View.VISIBLE : View.GONE);
                });
            }

            @Override
            public void onError(@NonNull Exception exception) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> Snackbar.make(requireView(), "Failed to load registered vehicles", Snackbar.LENGTH_LONG).show());
            }
        });

        repository.listenVehicleRequests(uid, new VehicleRequestRepository.RequestListListener() {
            @Override
            public void onData(@NonNull List<VehicleRequestRecord> requests) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    List<VehicleRequestRecord> history = new ArrayList<>();
                    for (VehicleRequestRecord record : requests) {
                        String status = record.getStatus().trim().toLowerCase(Locale.getDefault());
                        if (!record.isOpenApplication()
                                && ("approved".equals(status) || "cancelled".equals(status))) {
                            history.add(record);
                        }
                    }
                    historyAdapter.submitList(history);
                    textHistoryEmpty.setVisibility(history.isEmpty() ? View.VISIBLE : View.GONE);
                    scrollToTargetRequest();
                });
            }

            @Override
            public void onError(@NonNull Exception exception) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> Snackbar.make(requireView(), "Failed to load vehicle history", Snackbar.LENGTH_LONG).show());
            }
        });
    }

    private void bindOpenApplication(@Nullable VehicleRequestRecord request) {
        this.openRequest = request;
        if (request == null) {
            textOpenApplication.setText("You have no current vehicle registration applications");
            buttonCancelOpenApplication.setVisibility(View.GONE);
            setFormEnabled(true);
            return;
        }
        String kind = request.isRemovalRequest() ? "Removal" : "Registration";
        String canCancel = request.canCancelByApplicant() ? "Cancelable" : "Locked after admin received";
        textOpenApplication.setText(kind + " application • " + request.getStatus().toUpperCase(Locale.getDefault()) + "\n" + request.getPlateNumber() + "\n" + canCancel);
        if (!targetRequestId.isEmpty() && targetRequestId.equals(request.getId())) {
            Snackbar.make(requireView(), "Linked vehicle request is the open application.", Snackbar.LENGTH_LONG).show();
            targetRequestId = "";
        }
        buttonCancelOpenApplication.setVisibility(request.canCancelByApplicant() ? View.VISIBLE : View.GONE);
        setFormEnabled(false);
        layoutRemoval.setVisibility(View.GONE);
    }

    private void setFormEnabled(boolean enabled) {
        boolean controlsEnabled = enabled && !registrationUploadInProgress;
        inputPlate.setEnabled(controlsEnabled);
        inputMake.setEnabled(controlsEnabled);
        inputModel.setEnabled(controlsEnabled);
        inputVariant.setEnabled(controlsEnabled);
        checkIsOwner.setEnabled(controlsEnabled);
        buttonPickApplicantCnic.setEnabled(controlsEnabled);
        buttonPickRegistration.setEnabled(controlsEnabled);
        buttonPickOwnerCnic.setEnabled(controlsEnabled && !checkIsOwner.isChecked());
        buttonSubmitRegistration.setEnabled(controlsEnabled);
        buttonAddEvidence.setEnabled(controlsEnabled);
        buttonSubmitRemoval.setEnabled(controlsEnabled);
        inputRemovalReason.setEnabled(controlsEnabled);
    }

    private void updateOwnerDocVisibility() {
        boolean owner = checkIsOwner.isChecked();
        buttonPickOwnerCnic.setVisibility(owner ? View.GONE : View.VISIBLE);
        textOwnerCnic.setVisibility(owner ? View.GONE : View.VISIBLE);
        if (owner) {
            ownerCnicDoc = null;
            textOwnerCnic.setText("No file selected");
        }
        updateAttachmentRemoveButtons();
    }

    private void launchPicker(@NonNull PickTarget target) {
        currentPickTarget = target;
        documentPicker.launch(new String[]{"application/pdf", "image/png", "image/jpeg"});
    }

    private void handleDocumentPicked(@Nullable Uri uri) {
        if (uri == null || currentPickTarget == null || !isAdded()) {
            return;
        }
        try {
            requireContext().getContentResolver().takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
            );
        } catch (Exception ignored) {
            // Temporary URI permissions are sufficient for immediate submission path.
        }

        String name = resolveDisplayName(uri);
        VehicleDocumentInput input = new VehicleDocumentInput(uri, name);
        switch (currentPickTarget) {
            case APPLICANT_CNIC:
                applicantCnicDoc = input;
                textApplicantCnic.setText(name);
                break;
            case REGISTRATION:
                registrationDoc = input;
                textRegistrationDoc.setText(name);
                break;
            case OWNER_CNIC:
                ownerCnicDoc = input;
                textOwnerCnic.setText(name);
                break;
            case EVIDENCE:
                removalEvidenceDocs.add(input);
                updateEvidenceText();
                break;
            default:
                break;
        }
        updateAttachmentRemoveButtons();
    }

    private void updateEvidenceText() {
        if (removalEvidenceDocs.isEmpty()) {
            textEvidence.setText("No evidence files selected");
            buttonClearEvidence.setVisibility(View.GONE);
            return;
        }
        List<String> names = new ArrayList<>();
        for (VehicleDocumentInput input : removalEvidenceDocs) {
            names.add(input.getDisplayName());
        }
        textEvidence.setText(TextUtils.join("\n", names));
        buttonClearEvidence.setVisibility(View.VISIBLE);
    }

    private void updateAttachmentRemoveButtons() {
        buttonRemoveApplicantCnic.setVisibility(applicantCnicDoc == null ? View.GONE : View.VISIBLE);
        buttonRemoveRegistration.setVisibility(registrationDoc == null ? View.GONE : View.VISIBLE);
        boolean canRemoveOwner = !checkIsOwner.isChecked() && ownerCnicDoc != null;
        buttonRemoveOwnerCnic.setVisibility(canRemoveOwner ? View.VISIBLE : View.GONE);
    }

    private void scrollToTargetRequest() {
        if (targetRequestId.isEmpty() || historyRecycler == null) {
            return;
        }
        int position = historyAdapter.indexOfRequestId(targetRequestId);
        if (position == RecyclerView.NO_POSITION) {
            return;
        }
        historyRecycler.post(() -> historyRecycler.smoothScrollToPosition(position));
        Snackbar.make(requireView(), "Linked vehicle request is in history.", Snackbar.LENGTH_LONG).show();
        targetRequestId = "";
    }

    @NonNull
    private String safe(@Nullable String value) {
        return value == null ? "" : value.trim();
    }

    private void submitRegistrationApplication() {
        UserProfile profile = AuthUiGuard.requireProfile(this);
        if (profile == null) {
            return;
        }
        String role = profile.getRole().trim().toLowerCase(Locale.getDefault());
        String studentCategory = VehicleStickerPolicy.normalizeStudentCategory(profile.getStudentCategory());
        if ("student".equals(role) && !VehicleStickerPolicy.isSupportedStudentCategory(studentCategory)) {
            Snackbar.make(requireView(), "Student category missing. Ask admin to set day_scholar/hostelite/redc.", Snackbar.LENGTH_LONG).show();
            return;
        }
        if (openRequest != null) {
            Snackbar.make(requireView(), "Resolve the current open application before creating another.", Snackbar.LENGTH_LONG).show();
            return;
        }

        String plate = read(inputPlate);
        String make = read(inputMake);
        String model = read(inputModel);
        String variant = read(inputVariant);

        if (plate.isEmpty() || make.isEmpty() || model.isEmpty() || variant.isEmpty()) {
            Snackbar.make(requireView(), R.string.error_fill_required_fields, Snackbar.LENGTH_SHORT).show();
            return;
        }
        String normalizedPlate = GuestIdentityPolicy.normalizeVehiclePlate(plate);
        if (normalizedPlate == null) {
            Snackbar.make(requireView(), R.string.error_invalid_vehicle_plate, Snackbar.LENGTH_SHORT).show();
            return;
        }
        if (applicantCnicDoc == null || registrationDoc == null) {
            Snackbar.make(requireView(), "Attach applicant CNIC and registration documents.", Snackbar.LENGTH_SHORT).show();
            return;
        }
        if (!checkIsOwner.isChecked() && ownerCnicDoc == null) {
            Snackbar.make(requireView(), "Attach owner CNIC document.", Snackbar.LENGTH_SHORT).show();
            return;
        }

        VehicleRegistrationDraft draft = new VehicleRegistrationDraft(
                profile.getUid(),
                role,
                studentCategory,
                normalizedPlate,
                make,
                model,
                variant,
                checkIsOwner.isChecked(),
                applicantCnicDoc,
                registrationDoc,
                ownerCnicDoc
        );
        beginRegistrationUpload();
        repository.submitRegistrationApplication(
                draft,
                (success, message, exception) -> {
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() -> {
                        endRegistrationUpload();
                        Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG).show();
                        if (success) {
                            clearRegistrationForm();
                        }
                    });
                },
                (percent, bytesTransferred, totalBytes) -> {
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() ->
                            renderRegistrationUploadProgress(percent, bytesTransferred, totalBytes));
                }
        );
    }

    private void beginRegistrationUpload() {
        registrationUploadInProgress = true;
        renderRegistrationUploadProgress(0, 0L, 0L);
        setFormEnabled(openRequest == null);
    }

    private void endRegistrationUpload() {
        registrationUploadInProgress = false;
        progressRegistrationUpload.setVisibility(View.GONE);
        textRegistrationUploadProgress.setVisibility(View.GONE);
        setFormEnabled(openRequest == null);
    }

    private void renderRegistrationUploadProgress(int percent, long bytesTransferred, long totalBytes) {
        progressRegistrationUpload.setVisibility(View.VISIBLE);
        textRegistrationUploadProgress.setVisibility(View.VISIBLE);
        progressRegistrationUpload.setProgress(percent);
        String progressText = "Uploading required documents: "
                + percent
                + "%";
        if (totalBytes > 0) {
            progressText += " (" + formatKb(bytesTransferred) + " / " + formatKb(totalBytes) + ")";
        }
        textRegistrationUploadProgress.setText(progressText);
    }

    @NonNull
    private String formatKb(long bytes) {
        if (bytes <= 0L) {
            return "0 KB";
        }
        double kb = bytes / 1024.0;
        return String.format(Locale.getDefault(), "%.1f KB", kb);
    }

    private void clearRegistrationForm() {
        inputPlate.setText("");
        inputMake.setText("");
        inputModel.setText("");
        inputVariant.setText("");
        checkIsOwner.setChecked(true);
        applicantCnicDoc = null;
        registrationDoc = null;
        ownerCnicDoc = null;
        textApplicantCnic.setText("No file selected");
        textRegistrationDoc.setText("No file selected");
        textOwnerCnic.setText("No file selected");
        updateAttachmentRemoveButtons();
    }

    private void cancelOpenApplication() {
        if (openRequest == null || !openRequest.canCancelByApplicant()) {
            return;
        }
        repository.cancelVehicleRequest(openRequest.getId(), (success, message, exception) -> {
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG).show());
        });
    }

    @Override
    public void onRequestRemoval(@NonNull RegisteredVehicleRecord vehicle) {
        if (openRequest != null) {
            Snackbar.make(requireView(), "Resolve the current open application first.", Snackbar.LENGTH_LONG).show();
            return;
        }
        selectedVehicleForRemoval = vehicle;
        layoutRemoval.setVisibility(View.VISIBLE);
        textRemovalVehicle.setText("Selected: " + vehicle.getPlateNumber() + " • " + vehicle.getMake() + " " + vehicle.getModel());
        inputRemovalReason.setText("");
        removalEvidenceDocs.clear();
        updateEvidenceText();
    }

    private void submitRemovalApplication() {
        UserProfile profile = AuthUiGuard.requireProfile(this);
        if (profile == null) return;
        if (openRequest != null) {
            Snackbar.make(requireView(), "Resolve the current open application before creating another.", Snackbar.LENGTH_LONG).show();
            return;
        }
        if (selectedVehicleForRemoval == null) {
            Snackbar.make(requireView(), "Select a registered vehicle first.", Snackbar.LENGTH_SHORT).show();
            return;
        }
        String reason = read(inputRemovalReason);
        if (reason.isEmpty()) {
            Snackbar.make(requireView(), "Removal reason is required.", Snackbar.LENGTH_SHORT).show();
            return;
        }

        VehicleRemovalDraft draft = new VehicleRemovalDraft(
                profile.getUid(),
                profile.getRole(),
                profile.getStudentCategory(),
                selectedVehicleForRemoval.getId(),
                reason,
                new ArrayList<>(removalEvidenceDocs)
        );
        buttonSubmitRemoval.setEnabled(false);
        repository.submitRemovalApplication(draft, (success, message, exception) -> {
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                buttonSubmitRemoval.setEnabled(true);
                Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG).show();
                if (success) {
                    selectedVehicleForRemoval = null;
                    layoutRemoval.setVisibility(View.GONE);
                    removalEvidenceDocs.clear();
                    updateEvidenceText();
                }
            });
        });
    }

    @Override
    public void onViewDetails(@NonNull VehicleRequestRecord record) {
        StringBuilder message = new StringBuilder();
        message.append("Application Type: ").append(formatRequestKind(record.getRequestKind())).append("\n")
                .append("Status: ").append(formatLabel(record.getStatus())).append("\n")
                .append("Plate Number: ").append(fallback(record.getPlateNumber())).append("\n")
                .append("Vehicle Make: ").append(fallback(record.getVehicleMake())).append("\n")
                .append("Vehicle Model: ").append(fallback(record.getVehicleModel())).append("\n")
                .append("Vehicle Variant: ").append(fallback(record.getVehicleVariant())).append("\n")
                .append("Sticker Type: ").append(fallback(record.getStickerType())).append("\n")
                .append("Owner Verified: ").append(record.isOwner() ? "Yes" : "No").append("\n")
                .append("Submitted At: ").append(formatTimestamp(record.getCreatedAt())).append("\n")
                .append("Updated At: ").append(formatTimestamp(record.getUpdatedAt())).append("\n");
        if (record.isRemovalRequest()) {
            message.append("Removal Reason: ").append(fallback(record.getRemovalReason())).append("\n");
        }
        message.append("Review Note: ").append(fallback(record.getReviewNote()));

        new AlertDialog.Builder(requireContext())
                .setTitle("Vehicle Application Details")
                .setMessage(message.toString())
                .setPositiveButton(R.string.close_action, null)
                .setNeutralButton("Open Attachments", (dialog, which) -> showAttachmentChooser(record))
                .show();
    }

    @NonNull
    private String formatRequestKind(@NonNull String rawKind) {
        if ("remove".equalsIgnoreCase(rawKind)) {
            return "Removal";
        }
        return "Registration";
    }

    @NonNull
    private String formatLabel(@NonNull String raw) {
        String value = raw.trim();
        if (value.isEmpty()) {
            return "N/A";
        }
        return value.substring(0, 1).toUpperCase(Locale.getDefault())
                + value.substring(1).toLowerCase(Locale.getDefault());
    }

    @NonNull
    private String formatTimestamp(@Nullable com.google.firebase.Timestamp timestamp) {
        if (timestamp == null) {
            return "N/A";
        }
        DateFormat format = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);
        Date date = timestamp.toDate();
        return format.format(date);
    }

    private void showAttachmentChooser(@NonNull VehicleRequestRecord record) {
        List<String> labels = new ArrayList<>();
        List<VehicleDocumentRef> docs = new ArrayList<>();
        addAttachmentIfPresent(labels, docs, "Applicant CNIC", record.getApplicantCnicDoc());
        addAttachmentIfPresent(labels, docs, "Registration", record.getRegistrationDoc());
        addAttachmentIfPresent(labels, docs, "Owner CNIC", record.getOwnerCnicDoc());
        List<VehicleDocumentRef> evidence = record.getEvidenceDocs();
        for (int i = 0; i < evidence.size(); i++) {
            addAttachmentIfPresent(labels, docs, "Evidence " + (i + 1), evidence.get(i));
        }
        if (labels.isEmpty()) {
            Snackbar.make(requireView(), "No attachments available.", Snackbar.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(requireContext())
                .setTitle("Attachments")
                .setItems(labels.toArray(new String[0]), (dialog, which) -> openAttachment(docs.get(which)))
                .show();
    }

    private void addAttachmentIfPresent(
            @NonNull List<String> labels,
            @NonNull List<VehicleDocumentRef> docs,
            @NonNull String label,
            @NonNull VehicleDocumentRef doc
    ) {
        if (doc.getDownloadUrl().trim().isEmpty() && doc.getStoragePath().trim().isEmpty()) {
            return;
        }
        labels.add(label + " • " + doc.getName());
        docs.add(doc);
    }

    private void openAttachment(@NonNull VehicleDocumentRef doc) {
        String url = doc.getDownloadUrl().trim();
        if (!url.isEmpty()) {
            openAttachmentUrl(url);
            return;
        }
        String storagePath = doc.getStoragePath().trim();
        if (storagePath.isEmpty()) {
            if (isAdded()) {
                Snackbar.make(requireView(), "Attachment link unavailable", Snackbar.LENGTH_SHORT).show();
            }
            return;
        }
        resolveAttachmentUrl(storagePath, resolveBucketCandidates(doc), 0);
    }

    private void resolveAttachmentUrl(
            @NonNull String storagePath,
            @NonNull List<String> buckets,
            int index
    ) {
        if (index >= buckets.size()) {
            if (isAdded()) {
                Snackbar.make(requireView(), "Unable to resolve attachment URL", Snackbar.LENGTH_SHORT).show();
            }
            return;
        }
        StorageReference reference = buildStorageReference(buckets.get(index), storagePath);
        reference.getDownloadUrl()
                .addOnSuccessListener(uri -> openAttachmentUrl(uri.toString()))
                .addOnFailureListener(error -> {
                    if (shouldTryNextBucket(error, index, buckets)) {
                        resolveAttachmentUrl(storagePath, buckets, index + 1);
                        return;
                    }
                    if (isAdded()) {
                        Snackbar.make(requireView(), "Unable to resolve attachment URL", Snackbar.LENGTH_SHORT).show();
                    }
                });
    }

    @NonNull
    private StorageReference buildStorageReference(
            @NonNull String bucket,
            @NonNull String storagePath
    ) {
        if (bucket.trim().isEmpty()) {
            return FirebaseStorage.getInstance().getReference().child(storagePath);
        }
        return FirebaseStorage.getInstance("gs://" + bucket).getReference().child(storagePath);
    }

    @NonNull
    private List<String> resolveBucketCandidates(@NonNull VehicleDocumentRef doc) {
        LinkedHashSet<String> buckets = new LinkedHashSet<>();
        addBucketCandidate(buckets, doc.getBucket());
        addBucketCandidate(buckets, FirebaseStorage.getInstance().getReference().getBucket());
        for (String bucket : new ArrayList<>(buckets)) {
            addBucketCandidate(buckets, alternateBucketFor(bucket));
        }
        if (buckets.isEmpty()) {
            buckets.add("");
        }
        return new ArrayList<>(buckets);
    }

    private void addBucketCandidate(
            @NonNull LinkedHashSet<String> buckets,
            @Nullable String raw
    ) {
        String normalized = normalizeBucket(raw);
        if (!normalized.isEmpty()) {
            buckets.add(normalized);
        }
    }

    @NonNull
    private String normalizeBucket(@Nullable String raw) {
        if (raw == null) {
            return "";
        }
        String value = raw.trim();
        if (value.startsWith("gs://")) {
            value = value.substring(5);
        }
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    @NonNull
    private String alternateBucketFor(@NonNull String bucket) {
        if (bucket.endsWith(".firebasestorage.app")) {
            return bucket.replace(".firebasestorage.app", ".appspot.com");
        }
        if (bucket.endsWith(".appspot.com")) {
            return bucket.replace(".appspot.com", ".firebasestorage.app");
        }
        return "";
    }

    private boolean shouldTryNextBucket(
            @NonNull Exception error,
            int index,
            @NonNull List<String> buckets
    ) {
        if (index >= buckets.size() - 1) {
            return false;
        }
        if (!(error instanceof StorageException)) {
            return false;
        }
        int code = ((StorageException) error).getErrorCode();
        return code == -13010 || code == -13011 || code == -13012;
    }

    private void openAttachmentUrl(@NonNull String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } catch (Exception e) {
            if (isAdded()) {
                Snackbar.make(requireView(), "Unable to open attachment", Snackbar.LENGTH_SHORT).show();
            }
        }
    }

    @NonNull
    private String resolveDisplayName(@NonNull Uri uri) {
        Cursor cursor = null;
        try {
            cursor = requireContext().getContentResolver().query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (nameIndex >= 0) {
                    String name = cursor.getString(nameIndex);
                    if (name != null && !name.trim().isEmpty()) {
                        return name;
                    }
                }
            }
        } catch (Exception ignored) {
            // Fallback below.
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        String fallback = uri.getLastPathSegment();
        return fallback == null || fallback.trim().isEmpty() ? "document" : fallback;
    }

    @NonNull
    private String read(@NonNull TextInputEditText input) {
        CharSequence value = input.getText();
        return value == null ? "" : value.toString().trim();
    }

    @NonNull
    private String fallback(@Nullable String value) {
        if (value == null) {
            return "N/A";
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? "N/A" : trimmed;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        repository.removeListeners();
    }
}
