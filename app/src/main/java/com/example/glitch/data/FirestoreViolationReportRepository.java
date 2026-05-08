package com.example.glitch.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.glitch.model.GuestIdentityPolicy;
import com.example.glitch.model.GuestPass;
import com.example.glitch.model.ViolationReport;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirestoreViolationReportRepository implements ViolationReportRepository {
    private static final String COLLECTION_REPORTS = "violation_reports";
    private static final String COLLECTION_PASSES = "guest_passes";
    private static final String COLLECTION_USERS = "users";

    private final FirebaseFirestore firestore;
    private ListenerRegistration allReportsRegistration;
    private ListenerRegistration reporterRegistration;

    public FirestoreViolationReportRepository() {
        this(FirebaseFirestore.getInstance());
    }

    FirestoreViolationReportRepository(@NonNull FirebaseFirestore firestore) {
        this.firestore = firestore;
    }

    @Override
    public void submitReport(
            @NonNull String reporterUid,
            @NonNull String reporterRole,
            @NonNull String reporterName,
            @NonNull String detail,
            @NonNull String violationLevel,
            @NonNull String subjectType,
            @NonNull String guestCnic,
            @NonNull String guestName,
            @NonNull String guestPassId,
            @NonNull String sponsorUid,
            @NonNull String sponsorName,
            @NonNull String sponsorRole,
            @NonNull String subjectStudentUid,
            @NonNull String subjectStudentName,
            @NonNull String subjectStudentEmail,
            @NonNull OperationCallback callback
    ) {
        if (detail.trim().isEmpty() || violationLevel.trim().isEmpty()) {
            callback.onComplete(false, "Fill in all required fields.", null);
            return;
        }
        Map<String, Object> payload = new HashMap<>();
        payload.put("reporterUid", reporterUid.trim());
        payload.put("reporterRole", reporterRole.trim());
        payload.put("reporterName", reporterName.trim());
        payload.put("detail", detail.trim());
        payload.put("violationLevel", violationLevel.trim());
        payload.put("subjectType", subjectType.trim());
        payload.put("guestCnic", guestCnic.trim());
        payload.put("guestName", guestName.trim());
        payload.put("guestPassId", guestPassId.trim());
        payload.put("sponsorUid", sponsorUid.trim());
        payload.put("sponsorName", sponsorName.trim());
        payload.put("sponsorRole", sponsorRole.trim());
        payload.put("subjectStudentUid", subjectStudentUid.trim());
        payload.put("subjectStudentName", subjectStudentName.trim());
        payload.put("subjectStudentEmail", subjectStudentEmail.trim());
        payload.put("status", ViolationReport.STATUS_PENDING);
        payload.put("createdAt", FieldValue.serverTimestamp());
        payload.put("updatedAt", FieldValue.serverTimestamp());
        firestore.collection(COLLECTION_REPORTS)
                .add(payload)
                .addOnSuccessListener(ref -> callback.onComplete(true, "Violation report submitted.", null))
                .addOnFailureListener(error -> callback.onComplete(false, "Failed to submit report.", error));
    }

    @Override
    public void listenAllReports(@NonNull ReportListListener listener) {
        if (allReportsRegistration != null) allReportsRegistration.remove();
        allReportsRegistration = firestore.collection(COLLECTION_REPORTS)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) { listener.onError(error); return; }
                    List<ViolationReport> reports = new ArrayList<>();
                    if (snapshot != null) {
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            reports.add(ViolationReport.fromMap(doc.getId(), doc.getData()));
                        }
                    }
                    listener.onData(reports);
                });
    }

    @Override
    public void listenReportsByReporter(@NonNull String uid, @NonNull ReportListListener listener) {
        if (reporterRegistration != null) reporterRegistration.remove();
        reporterRegistration = firestore.collection(COLLECTION_REPORTS)
                .whereEqualTo("reporterUid", uid.trim())
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) { listener.onError(error); return; }
                    List<ViolationReport> reports = new ArrayList<>();
                    if (snapshot != null) {
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            reports.add(ViolationReport.fromMap(doc.getId(), doc.getData()));
                        }
                    }
                    listener.onData(reports);
                });
    }

    @Override
    public void ignoreReport(@NonNull String reportId, @NonNull String adminUid, @NonNull OperationCallback callback) {
        updateReportStatus(reportId, ViolationReport.STATUS_IGNORED, callback);
    }

    @Override
    public void markActioned(@NonNull String reportId, @NonNull String adminUid, @NonNull OperationCallback callback) {
        updateReportStatus(reportId, ViolationReport.STATUS_ACTIONED, callback);
    }

    private void updateReportStatus(@NonNull String reportId, @NonNull String status, @NonNull OperationCallback callback) {
        if (reportId.trim().isEmpty()) {
            callback.onComplete(false, "Report ID is required.", null);
            return;
        }
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", status);
        updates.put("updatedAt", FieldValue.serverTimestamp());
        firestore.collection(COLLECTION_REPORTS)
                .document(reportId.trim())
                .set(updates, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(unused -> callback.onComplete(true, "Report updated.", null))
                .addOnFailureListener(error -> callback.onComplete(false, "Failed to update report.", error));
    }

    @Override
    public void findActivePasForCnic(@NonNull String cnic, @NonNull PassInfoCallback callback) {
        String normalizedCnic = GuestIdentityPolicy.normalizeCnic(cnic);
        if (normalizedCnic == null) {
            callback.onNotFound("Invalid CNIC format.");
            return;
        }
        firestore.collection(COLLECTION_PASSES)
                .whereEqualTo("guestIdNumber", normalizedCnic)
                .whereIn("status", Arrays.asList("active", "used", "overdue"))
                .limit(1)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        callback.onNotFound("No active guest pass found for this CNIC.");
                        return;
                    }
                    DocumentSnapshot doc = snapshot.getDocuments().get(0);
                    String guestName = asString(doc.get("guestName"));
                    String passId = doc.getId();
                    String sponsorUid = asString(doc.get("sponsorUid"));
                    String sponsorName = asString(doc.get("sponsorName"));
                    String sponsorRole = asString(doc.get("sponsorRole"));
                    callback.onFound(guestName, passId, sponsorUid, sponsorName, sponsorRole);
                })
                .addOnFailureListener(callback::onError);
    }

    @Override
    public void findStudentByEmail(@NonNull String email, @NonNull StudentInfoCallback callback) {
        String normalizedEmail = email.trim().toLowerCase();
        if (normalizedEmail.isEmpty()) {
            callback.onNotFound("Enter a valid email.");
            return;
        }
        firestore.collection(COLLECTION_USERS)
                .whereEqualTo("email", normalizedEmail)
                .whereEqualTo("role", "student")
                .limit(1)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        callback.onNotFound("No student found with this email.");
                        return;
                    }
                    DocumentSnapshot doc = snapshot.getDocuments().get(0);
                    String uid = doc.getId();
                    String name = asString(doc.get("displayName"));
                    callback.onFound(uid, name);
                })
                .addOnFailureListener(callback::onError);
    }

    @Override
    public void removeListeners() {
        if (allReportsRegistration != null) { allReportsRegistration.remove(); allReportsRegistration = null; }
        if (reporterRegistration != null) { reporterRegistration.remove(); reporterRegistration = null; }
    }

    @NonNull
    private String asString(@Nullable Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }
}
