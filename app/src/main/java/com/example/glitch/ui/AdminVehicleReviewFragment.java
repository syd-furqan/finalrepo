package com.example.glitch.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.glitch.R;
import com.example.glitch.data.RepositoryProvider;
import com.example.glitch.data.VehicleRequestRepository;
import com.example.glitch.model.UserProfile;
import com.example.glitch.model.VehicleDocumentRef;
import com.example.glitch.model.VehicleRequestRecord;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageException;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import com.example.glitch.ui.UiAnimations;

/**
 * Admin screen for reviewing sponsor vehicle applications.
 */
public class AdminVehicleReviewFragment extends Fragment implements AdminVehicleReviewAdapter.ActionListener {
    private android.view.ViewGroup animContent;
    private static final String ARG_TARGET_REQUEST_ID = "target_request_id";

    private VehicleRequestRepository repository;
    private com.example.glitch.data.AlertRepository alertRepository;
    private AdminVehicleReviewAdapter adapter;
    private TextView textEmpty;
    private TextView textSummary;
    private AutoCompleteTextView inputStatusFilter;
    private RecyclerView recyclerView;
    private String targetRequestId = "";
    private List<VehicleRequestRecord> allRequests = new ArrayList<>();

    @NonNull
    public static AdminVehicleReviewFragment newInstance() {
        return new AdminVehicleReviewFragment();
    }

    @NonNull
    public static AdminVehicleReviewFragment newInstance(@NonNull String targetRequestId) {
        AdminVehicleReviewFragment fragment = new AdminVehicleReviewFragment();
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
        return inflater.inflate(R.layout.fragment_admin_vehicle_review, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        animContent = view.findViewById(R.id.anim_content);

        UserProfile profile = AuthUiGuard.requireProfile(this);
        if (profile == null) {
            return;
        }
        if (!"admin".equalsIgnoreCase(profile.getRole())) {
            RoleNavRouter.route(this, RoleDestination.DIRECTORY);
            return;
        }

        repository = RepositoryProvider.getVehicleRequestRepository();
        alertRepository = RepositoryProvider.getAlertRepository();
        textEmpty = view.findViewById(R.id.text_vehicle_review_empty);
        textSummary = view.findViewById(R.id.text_vehicle_review_summary);
        inputStatusFilter = view.findViewById(R.id.input_vehicle_review_status_filter);
        Bundle args = getArguments();
        targetRequestId = args == null ? "" : safe(args.getString(ARG_TARGET_REQUEST_ID));

        recyclerView = view.findViewById(R.id.recycler_vehicle_review);
        adapter = new AdminVehicleReviewAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        RoleNavRouter.bindBottomNav(view, this, RoleDestination.ADMIN_VEHICLES);
        setupStatusFilter();

        repository.listenAllVehicleRequests(new VehicleRequestRepository.RequestListListener() {
            @Override
            public void onData(@NonNull List<VehicleRequestRecord> requests) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    allRequests = new ArrayList<>(requests);
                    renderFiltered();
                });
            }

            @Override
            public void onError(@NonNull Exception exception) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() ->
                        Snackbar.make(requireView(), R.string.error_vehicle_load, Snackbar.LENGTH_LONG).show());
            }
        });
    }

    private void setupStatusFilter() {
        List<String> statuses = Arrays.asList("all", "submitted", "received", "approved", "denied", "cancelled");
        ArrayAdapter<String> filterAdapter = new ArrayAdapter<>(
                requireContext(),
                R.layout.item_dropdown_option,
                statuses
        );
        inputStatusFilter.setAdapter(filterAdapter);
        inputStatusFilter.setText("all", false);
        inputStatusFilter.setOnItemClickListener((parent, view, position, id) -> renderFiltered());
    }

    private void renderFiltered() {
        String status = inputStatusFilter.getText() == null
                ? "all"
                : inputStatusFilter.getText().toString().trim().toLowerCase(Locale.getDefault());
        List<VehicleRequestRecord> filtered = new ArrayList<>();
        for (VehicleRequestRecord record : allRequests) {
            if ("all".equals(status) || record.getStatus().equalsIgnoreCase(status)) {
                filtered.add(record);
            }
        }
        adapter.submitList(filtered);
        textEmpty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
        bindSummary(filtered);
        scrollToTargetRequest();
    }

    @Override
    public void onViewDetails(@NonNull VehicleRequestRecord record) {
        showDetailsDialog(record);
    }

    @Override
    public void onMarkReceived(@NonNull VehicleRequestRecord record) {
        UserProfile profile = AuthUiGuard.requireProfile(this);
        if (profile == null) return;
        repository.markVehicleRequestReceived(record.getId(), profile.getUid(), (success, message, exception) -> {
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG).show());
        });
    }

    @Override
    public void onApprove(@NonNull VehicleRequestRecord record) {
        showReviewDialog(record, true);
    }

    @Override
    public void onDeny(@NonNull VehicleRequestRecord record) {
        showReviewDialog(record, false);
    }

    private void showReviewDialog(@NonNull VehicleRequestRecord record, boolean approved) {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_vehicle_review_note, null, false);
        TextInputEditText inputNote = dialogView.findViewById(R.id.input_review_note);

        String title = approved ? getString(R.string.vehicle_approve_title) : getString(R.string.vehicle_deny_title);
        String positiveLabel = approved ? getString(R.string.vehicle_approve_action) : getString(R.string.vehicle_deny_action);

        new AlertDialog.Builder(requireContext())
                .setTitle(title)
                .setView(dialogView)
                .setPositiveButton(positiveLabel, (dialog, which) -> {
                    CharSequence raw = inputNote.getText();
                    String note = raw == null ? "" : raw.toString().trim();
                    submitReview(record, approved, note);
                })
                .setNegativeButton(R.string.cancel_action, null)
                .show();
    }

    private void submitReview(@NonNull VehicleRequestRecord record, boolean approved, @NonNull String note) {
        UserProfile profile = AuthUiGuard.requireProfile(this);
        if (profile == null) return;

        repository.reviewVehicleRequest(
                record.getId(),
                profile.getUid(),
                approved,
                note,
                (success, message, exception) -> {
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() -> {
                        if (success) {
                            updateLinkedVehicleAlert(
                                    record.getId(),
                                    approved ? "Vehicle request approved." : "Vehicle request denied."
                            );
                        }
                        Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG).show();
                    });
                }
        );
    }

    private void scrollToTargetRequest() {
        if (targetRequestId.trim().isEmpty() || recyclerView == null) {
            return;
        }
        int position = adapter.indexOfRequestId(targetRequestId);
        if (position == RecyclerView.NO_POSITION) {
            Snackbar.make(requireView(), "Linked vehicle request was not found.", Snackbar.LENGTH_LONG).show();
            targetRequestId = "";
            return;
        }
        recyclerView.post(() -> recyclerView.smoothScrollToPosition(position));
        targetRequestId = "";
    }

    private void updateLinkedVehicleAlert(@NonNull String requestId, @NonNull String summary) {
        UserProfile profile = AuthUiGuard.requireProfile(this);
        if (profile == null || alertRepository == null) {
            return;
        }
        alertRepository.updateLinkedAlertStatus(
                "vehicleRequestId",
                requestId,
                "actioned",
                summary,
                profile.getUid(),
                (success, message, exception) -> {
                    // Vehicle review remains the source of truth; alert status is best-effort mirror state.
                }
        );
    }

    private void showDetailsDialog(@NonNull VehicleRequestRecord record) {
        StringBuilder body = new StringBuilder();
        body.append("Request ID: ").append(record.getId()).append("\n")
                .append("Kind: ").append(record.getRequestKind()).append("\n")
                .append("Status: ").append(record.getStatus()).append("\n")
                .append("Requester UID: ").append(record.getRequesterUid()).append("\n")
                .append("Requester Role: ").append(record.getRequesterRole()).append("\n")
                .append("Student Category: ").append(fallback(record.getStudentCategory())).append("\n")
                .append("Sticker: ").append(fallback(record.getStickerType())).append("\n")
                .append("Plate: ").append(fallback(record.getPlateNumber())).append("\n")
                .append("Vehicle: ").append(fallback(record.getVehicleDescription())).append("\n")
                .append("Owner Self: ").append(record.isOwner() ? "Yes" : "No").append("\n")
                .append("Linked Vehicle ID: ").append(fallback(record.getLinkedVehicleId())).append("\n")
                .append("Removal Reason: ").append(fallback(record.getRemovalReason())).append("\n")
                .append("Received By: ").append(fallback(record.getReceivedByUid())).append("\n")
                .append("Reviewer: ").append(fallback(record.getReviewerUid())).append("\n")
                .append("Review Note: ").append(fallback(record.getReviewNote()));

        new AlertDialog.Builder(requireContext())
                .setTitle("Vehicle Application Details")
                .setMessage(body.toString())
                .setPositiveButton(R.string.close_action, null)
                .setNeutralButton("Open Attachments", (dialog, which) -> showAttachmentPicker(record))
                .show();
    }

    private void showAttachmentPicker(@NonNull VehicleRequestRecord record) {
        List<String> labels = new ArrayList<>();
        List<VehicleDocumentRef> docs = new ArrayList<>();
        addAttachment(labels, docs, "Applicant CNIC", record.getApplicantCnicDoc());
        addAttachment(labels, docs, "Registration", record.getRegistrationDoc());
        addAttachment(labels, docs, "Owner CNIC", record.getOwnerCnicDoc());
        List<VehicleDocumentRef> evidenceDocs = record.getEvidenceDocs();
        for (int i = 0; i < evidenceDocs.size(); i++) {
            addAttachment(labels, docs, "Evidence " + (i + 1), evidenceDocs.get(i));
        }
        if (labels.isEmpty()) {
            Snackbar.make(requireView(), "No attachments available", Snackbar.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Application Attachments")
                .setItems(labels.toArray(new String[0]), (dialog, which) -> openAttachment(docs.get(which)))
                .show();
    }

    private void addAttachment(@NonNull List<String> labels, @NonNull List<VehicleDocumentRef> docs, @NonNull String label, @NonNull VehicleDocumentRef doc) {
        if (doc.getDownloadUrl().trim().isEmpty() && doc.getStoragePath().trim().isEmpty()) {
            return;
        }
        labels.add(label + " • " + fallback(doc.getName()));
        docs.add(doc);
    }

    private void openAttachment(@NonNull VehicleDocumentRef doc) {
        String url = doc.getDownloadUrl().trim();
        if (!url.isEmpty()) {
            openUrl(url);
            return;
        }
        String storagePath = doc.getStoragePath().trim();
        if (storagePath.isEmpty()) {
            Snackbar.make(requireView(), "Attachment link unavailable", Snackbar.LENGTH_SHORT).show();
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
            Snackbar.make(requireView(), "Unable to resolve attachment URL", Snackbar.LENGTH_SHORT).show();
            return;
        }
        StorageReference reference = buildStorageReference(buckets.get(index), storagePath);
        reference.getDownloadUrl()
                .addOnSuccessListener(uri -> openUrl(uri.toString()))
                .addOnFailureListener(error -> {
                    if (shouldTryNextBucket(error, index, buckets)) {
                        resolveAttachmentUrl(storagePath, buckets, index + 1);
                        return;
                    }
                    Snackbar.make(requireView(), "Unable to resolve attachment URL", Snackbar.LENGTH_SHORT).show();
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

    private void openUrl(@NonNull String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } catch (Exception e) {
            Snackbar.make(requireView(), "Unable to open attachment", Snackbar.LENGTH_SHORT).show();
        }
    }

    @NonNull
    private String fallback(@Nullable String value) {
        return value == null || value.trim().isEmpty() ? "N/A" : value;
    }

    @NonNull
    private String safe(@Nullable String value) {
        return value == null ? "" : value.trim();
    }

    private void bindSummary(@NonNull List<VehicleRequestRecord> requests) {
        if (requests.isEmpty()) {
            textSummary.setVisibility(View.GONE);
            return;
        }
        int submitted = 0;
        int received = 0;
        int approved = 0;
        int denied = 0;
        int cancelled = 0;
        for (VehicleRequestRecord r : requests) {
            String s = r.getStatus().trim().toLowerCase(Locale.getDefault());
            if (VehicleRequestRecord.STATUS_SUBMITTED.equals(s)) submitted++;
            else if (VehicleRequestRecord.STATUS_RECEIVED.equals(s)) received++;
            else if (VehicleRequestRecord.STATUS_APPROVED.equals(s)) approved++;
            else if (VehicleRequestRecord.STATUS_DENIED.equals(s)) denied++;
            else if (VehicleRequestRecord.STATUS_CANCELLED.equals(s)) cancelled++;
        }
        textSummary.setVisibility(View.VISIBLE);
        textSummary.setText("Submitted: " + submitted
                + "  ·  Received: " + received
                + "  ·  Approved: " + approved
                + "  ·  Denied: " + denied
                + "  ·  Cancelled: " + cancelled);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        repository.removeListeners();
    }
    @Override
    public void onResume() {
        super.onResume();
        if (animContent != null) UiAnimations.animateFallIn(animContent);
    }
}
