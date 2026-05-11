package com.example.glitch.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.glitch.model.AuditEventType;
import com.example.glitch.model.GuestIdentityPolicy;
import com.example.glitch.model.GatePolicy;
import com.example.glitch.model.ViolationReport;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirestoreViolationReportRepository implements ViolationReportRepository {
    private static final String COLLECTION_REPORTS = "violation_reports";
    private static final String COLLECTION_ALERTS = "alerts";
    private static final String COLLECTION_PASSES = "guest_passes";
    private static final String COLLECTION_ENTRY_REQUESTS = "entry_requests";
    private static final String COLLECTION_USERS = "users";
    private static final String STATUS_REPORTED = "reported";

    private final FirebaseFirestore firestore;
    private final AuditEventLogger auditEventLogger;
    private final UserNotificationWriter notificationWriter;
    private ListenerRegistration allReportsRegistration;
    private ListenerRegistration reporterRegistration;

    public FirestoreViolationReportRepository() {
        this(FirebaseFirestore.getInstance());
    }

    FirestoreViolationReportRepository(@NonNull FirebaseFirestore firestore) {
        this.firestore = firestore;
        this.auditEventLogger = new AuditEventLogger(firestore);
        this.notificationWriter = new UserNotificationWriter(firestore);
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
            @NonNull String guestPhone,
            @NonNull String guestPassId,
            @NonNull String entryRequestId,
            @NonNull String previousGuestPassStatus,
            @NonNull String sponsorUid,
            @NonNull String sponsorName,
            @NonNull String sponsorRole,
            @NonNull String sponsorStudentId,
            @NonNull String subjectStudentUid,
            @NonNull String subjectStudentName,
            @NonNull String subjectStudentEmail,
            @NonNull String subjectStudentId,
            @NonNull OperationCallback callback
    ) {
        if (detail.trim().isEmpty() || violationLevel.trim().isEmpty()) {
            callback.onComplete(false, "Fill in all required fields.", null);
            return;
        }
        if (ViolationReport.SUBJECT_GUEST.equalsIgnoreCase(subjectType.trim())
                && (!guestPassId.trim().isEmpty() || !entryRequestId.trim().isEmpty())) {
            submitGuestReportWithCurrentContext(
                    reporterUid,
                    reporterRole,
                    reporterName,
                    detail,
                    violationLevel,
                    subjectType,
                    guestCnic,
                    guestName,
                    guestPhone,
                    guestPassId,
                    entryRequestId,
                    previousGuestPassStatus,
                    sponsorUid,
                    sponsorName,
                    sponsorRole,
                    sponsorStudentId,
                    subjectStudentUid,
                    subjectStudentName,
                    subjectStudentEmail,
                    subjectStudentId,
                    callback
            );
            return;
        }
        commitReport(
                reporterUid,
                reporterRole,
                reporterName,
                detail,
                violationLevel,
                subjectType,
                guestCnic,
                guestName,
                guestPhone,
                guestPassId,
                entryRequestId,
                previousGuestPassStatus,
                "",
                sponsorUid,
                sponsorName,
                sponsorRole,
                sponsorStudentId,
                subjectStudentUid,
                subjectStudentName,
                subjectStudentEmail,
                subjectStudentId,
                false,
                callback
        );
    }

    private void submitGuestReportWithCurrentContext(
            @NonNull String reporterUid,
            @NonNull String reporterRole,
            @NonNull String reporterName,
            @NonNull String detail,
            @NonNull String violationLevel,
            @NonNull String subjectType,
            @NonNull String guestCnic,
            @NonNull String guestName,
            @NonNull String guestPhone,
            @NonNull String guestPassId,
            @NonNull String entryRequestId,
            @NonNull String previousGuestPassStatus,
            @NonNull String sponsorUid,
            @NonNull String sponsorName,
            @NonNull String sponsorRole,
            @NonNull String sponsorStudentId,
            @NonNull String subjectStudentUid,
            @NonNull String subjectStudentName,
            @NonNull String subjectStudentEmail,
            @NonNull String subjectStudentId,
            @NonNull OperationCallback callback
    ) {
        if (guestPassId.trim().isEmpty()) {
            fetchEntryContextThenCommit(
                    reporterUid,
                    reporterRole,
                    reporterName,
                    detail,
                    violationLevel,
                    subjectType,
                    guestCnic,
                    guestName,
                    guestPhone,
                    guestPassId,
                    entryRequestId,
                    previousGuestPassStatus,
                    sponsorUid,
                    sponsorName,
                    sponsorRole,
                    sponsorStudentId,
                    subjectStudentUid,
                    subjectStudentName,
                    subjectStudentEmail,
                    subjectStudentId,
                    callback
            );
            return;
        }
        firestore.collection(COLLECTION_PASSES)
                .document(guestPassId.trim())
                .get()
                .addOnSuccessListener(passDoc -> {
                    String resolvedPassStatus = asString(passDoc.get("status"));
                    if (resolvedPassStatus.isEmpty()) {
                        resolvedPassStatus = previousGuestPassStatus.trim();
                    }
                    String resolvedEntryRequestId = entryRequestId.trim();
                    if (resolvedEntryRequestId.isEmpty()) {
                        resolvedEntryRequestId = asString(passDoc.get("entryRequestId"));
                    }
                    fetchEntryContextThenCommit(
                            reporterUid,
                            reporterRole,
                            reporterName,
                            detail,
                            violationLevel,
                            subjectType,
                            guestCnic,
                            guestName,
                            asString(passDoc.get("guestPhone")).isEmpty() ? guestPhone : asString(passDoc.get("guestPhone")),
                            guestPassId,
                            resolvedEntryRequestId,
                            resolvedPassStatus,
                            sponsorUid,
                            sponsorName,
                            sponsorRole,
                            sponsorStudentId,
                            subjectStudentUid,
                            subjectStudentName,
                            subjectStudentEmail,
                            subjectStudentId,
                            callback
                    );
                })
                .addOnFailureListener(error -> callback.onComplete(false, "Failed to read linked guest pass.", error));
    }

    private void fetchEntryContextThenCommit(
            @NonNull String reporterUid,
            @NonNull String reporterRole,
            @NonNull String reporterName,
            @NonNull String detail,
            @NonNull String violationLevel,
            @NonNull String subjectType,
            @NonNull String guestCnic,
            @NonNull String guestName,
            @NonNull String guestPhone,
            @NonNull String guestPassId,
            @NonNull String entryRequestId,
            @NonNull String previousGuestPassStatus,
            @NonNull String sponsorUid,
            @NonNull String sponsorName,
            @NonNull String sponsorRole,
            @NonNull String sponsorStudentId,
            @NonNull String subjectStudentUid,
            @NonNull String subjectStudentName,
            @NonNull String subjectStudentEmail,
            @NonNull String subjectStudentId,
            @NonNull OperationCallback callback
    ) {
        if (entryRequestId.trim().isEmpty()) {
            commitReport(
                    reporterUid,
                    reporterRole,
                    reporterName,
                    detail,
                    violationLevel,
                    subjectType,
                    guestCnic,
                    guestName,
                    guestPhone,
                    guestPassId,
                    entryRequestId,
                    previousGuestPassStatus,
                    "",
                    sponsorUid,
                    sponsorName,
                    sponsorRole,
                    sponsorStudentId,
                    subjectStudentUid,
                    subjectStudentName,
                    subjectStudentEmail,
                    subjectStudentId,
                    true,
                    callback
            );
            return;
        }
        firestore.collection(COLLECTION_ENTRY_REQUESTS)
                .document(entryRequestId.trim())
                .get()
                .addOnSuccessListener(requestDoc -> commitReport(
                        reporterUid,
                        reporterRole,
                        reporterName,
                        detail,
                        violationLevel,
                        subjectType,
                        guestCnic,
                        guestName,
                        asString(requestDoc.get("guestPhone")).isEmpty() ? guestPhone : asString(requestDoc.get("guestPhone")),
                        guestPassId,
                        entryRequestId,
                        previousGuestPassStatus,
                        asString(requestDoc.get("status")),
                        sponsorUid,
                        sponsorName,
                        sponsorRole,
                        sponsorStudentId,
                        subjectStudentUid,
                        subjectStudentName,
                        subjectStudentEmail,
                        subjectStudentId,
                        true,
                        callback
                ))
                .addOnFailureListener(error -> callback.onComplete(false, "Failed to read linked entry request.", error));
    }

    private void commitReport(
            @NonNull String reporterUid,
            @NonNull String reporterRole,
            @NonNull String reporterName,
            @NonNull String detail,
            @NonNull String violationLevel,
            @NonNull String subjectType,
            @NonNull String guestCnic,
            @NonNull String guestName,
            @NonNull String guestPhone,
            @NonNull String guestPassId,
            @NonNull String entryRequestId,
            @NonNull String previousGuestPassStatus,
            @NonNull String previousEntryRequestStatus,
            @NonNull String sponsorUid,
            @NonNull String sponsorName,
            @NonNull String sponsorRole,
            @NonNull String sponsorStudentId,
            @NonNull String subjectStudentUid,
            @NonNull String subjectStudentName,
            @NonNull String subjectStudentEmail,
            @NonNull String subjectStudentId,
            boolean reportLinkedGuestFlow,
            @NonNull OperationCallback callback
    ) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("reporterUid", reporterUid.trim());
        payload.put("reporterRole", reporterRole.trim());
        payload.put("reporterName", reporterName.trim());
        payload.put("detail", detail.trim());
        payload.put("violationLevel", violationLevel.trim());
        payload.put("subjectType", subjectType.trim());
        payload.put("guestCnic", guestCnic.trim());
        payload.put("guestName", guestName.trim());
        payload.put("guestPhone", guestPhone.trim());
        payload.put("guestPassId", guestPassId.trim());
        payload.put("entryRequestId", entryRequestId.trim());
        payload.put("previousGuestPassStatus", previousGuestPassStatus.trim());
        payload.put("previousEntryRequestStatus", previousEntryRequestStatus.trim());
        payload.put("sponsorUid", sponsorUid.trim());
        payload.put("sponsorName", sponsorName.trim());
        payload.put("sponsorRole", sponsorRole.trim());
        payload.put("sponsorStudentId", sponsorStudentId.trim());
        payload.put("subjectStudentUid", subjectStudentUid.trim());
        payload.put("subjectStudentName", subjectStudentName.trim());
        payload.put("subjectStudentEmail", subjectStudentEmail.trim());
        payload.put("subjectStudentId", subjectStudentId.trim());
        payload.put("source", AdminAlertPayloadFactory.SOURCE_MANUAL_VIOLATION);
        payload.put("status", ViolationReport.STATUS_PENDING);
        payload.put("createdAt", FieldValue.serverTimestamp());
        payload.put("updatedAt", FieldValue.serverTimestamp());
        DocumentReference reportRef = firestore.collection(COLLECTION_REPORTS).document();
        DocumentReference alertRef = firestore.collection(COLLECTION_ALERTS)
                .document("violation_report_" + reportRef.getId());
        WriteBatch batch = firestore.batch();
        batch.set(reportRef, payload);
        if (reportLinkedGuestFlow) {
            if (!guestPassId.trim().isEmpty()) {
                DocumentReference passRef = firestore.collection(COLLECTION_PASSES).document(guestPassId.trim());
                Map<String, Object> passUpdates = new HashMap<>();
                passUpdates.put("status", STATUS_REPORTED);
                passUpdates.put("reportedAt", FieldValue.serverTimestamp());
                passUpdates.put("reportReasonCode", AdminAlertPayloadFactory.SOURCE_MANUAL_VIOLATION);
                passUpdates.put("updatedAt", FieldValue.serverTimestamp());
                batch.set(passRef, passUpdates, SetOptions.merge());
            }
            if (!entryRequestId.trim().isEmpty()) {
                DocumentReference requestRef = firestore.collection(COLLECTION_ENTRY_REQUESTS).document(entryRequestId.trim());
                Map<String, Object> requestUpdates = new HashMap<>();
                requestUpdates.put("status", STATUS_REPORTED);
                requestUpdates.put("reportedAt", FieldValue.serverTimestamp());
                requestUpdates.put("reportedByUid", reporterUid.trim());
                requestUpdates.put("reportedByRole", reporterRole.trim());
                requestUpdates.put("reportedByName", reporterName.trim());
                requestUpdates.put("reportReasonCode", AdminAlertPayloadFactory.SOURCE_MANUAL_VIOLATION);
                requestUpdates.put("reportSource", "monitor_manual_violation");
                requestUpdates.put("updatedAt", FieldValue.serverTimestamp());
                batch.set(requestRef, requestUpdates, SetOptions.merge());
            }
        }
        addManualViolationNotificationToBatch(
                batch,
                subjectType,
                reportRef.getId(),
                sponsorUid,
                sponsorRole,
                sponsorName,
                guestName,
                subjectStudentUid,
                subjectStudentName
        );
        batch.set(alertRef, AdminAlertPayloadFactory.manualViolation(
                reportRef.getId(),
                reporterUid,
                reporterRole,
                reporterName,
                detail,
                violationLevel,
                subjectType,
                guestCnic,
                guestName,
                guestPhone,
                sponsorUid,
                sponsorName,
                sponsorRole,
                subjectStudentUid,
                subjectStudentName,
                subjectStudentEmail,
                subjectStudentId
        ));
        batch.commit()
                .addOnSuccessListener(unused -> {
                    Map<String, Object> auditMeta = new HashMap<>();
                    auditMeta.put("violationLevel", violationLevel.trim());
                    auditMeta.put("subjectType", subjectType.trim());
                    auditMeta.put("guestPassId", guestPassId.trim());
                    auditMeta.put("entryRequestId", entryRequestId.trim());
                    auditEventLogger.log(
                            AuditEventType.VIOLATION_REPORT_SUBMITTED,
                            "violation_report",
                            reportRef.getId(),
                            entryRequestId.trim().isEmpty() ? reportRef.getId() : entryRequestId.trim(),
                            reporterUid.trim(),
                            reporterRole.trim(),
                            "Violation report submitted: " + violationLevel.trim(),
                            "monitor_report",
                            "success",
                            violationLevel.trim().isEmpty() ? "manual_violation" : violationLevel.trim(),
                            GatePolicy.STORED_VALUE,
                            auditMeta
                    );
                    callback.onComplete(true, "Violation report submitted.", null);
                })
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
    public void getReportById(@NonNull String reportId, @NonNull SingleReportListener listener) {
        String normalizedId = reportId.trim();
        if (normalizedId.isEmpty()) {
            listener.onData(null);
            return;
        }
        firestore.collection(COLLECTION_REPORTS)
                .document(normalizedId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        listener.onData(null);
                        return;
                    }
                    listener.onData(ViolationReport.fromMap(doc.getId(), doc.getData()));
                })
                .addOnFailureListener(listener::onError);
    }

    @Override
    public void ignoreReport(@NonNull String reportId, @NonNull String adminUid, @NonNull OperationCallback callback) {
        updateReportStatus(reportId, ViolationReport.STATUS_IGNORED, callback);
    }

    @Override
    public void markActioned(@NonNull String reportId, @NonNull String adminUid, @NonNull OperationCallback callback) {
        updateReportStatus(reportId, ViolationReport.STATUS_ACTIONED, callback);
    }

    @Override
    public void completeReview(
            @NonNull String reportId,
            @NonNull String status,
            @NonNull Map<String, Object> reviewFields,
            @NonNull OperationCallback callback
    ) {
        if (reportId.trim().isEmpty()) {
            callback.onComplete(false, "Report ID is required.", null);
            return;
        }
        Map<String, Object> updates = new HashMap<>(reviewFields);
        updates.put("status", status.trim());
        updates.put("reviewedAt", FieldValue.serverTimestamp());
        updates.put("updatedAt", FieldValue.serverTimestamp());
        firestore.collection(COLLECTION_REPORTS)
                .document(reportId.trim())
                .set(updates, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    writeReviewNotifications(reportId.trim(), status.trim());
                    callback.onComplete(true, "Review recorded.", null);
                })
                .addOnFailureListener(error -> callback.onComplete(false, "Failed to record review.", error));
    }

    private void addManualViolationNotificationToBatch(
            @NonNull WriteBatch batch,
            @NonNull String subjectType,
            @NonNull String reportId,
            @NonNull String sponsorUid,
            @NonNull String sponsorRole,
            @NonNull String sponsorName,
            @NonNull String guestName,
            @NonNull String subjectStudentUid,
            @NonNull String subjectStudentName
    ) {
        if (ViolationReport.SUBJECT_STUDENT.equalsIgnoreCase(subjectType)) {
            notificationWriter.addToBatch(
                    batch,
                    subjectStudentUid,
                    "violation_created",
                    reportId,
                    "Violation Report Created",
                    "A violation report was created for " + fallback(subjectStudentName, "your student record") + ".",
                    COLLECTION_REPORTS
            );
            return;
        }
        if (UserNotificationWriter.supportsRole(sponsorRole)) {
            notificationWriter.addToBatch(
                    batch,
                    sponsorUid,
                    "violation_created",
                    reportId,
                    "Guest Violation Reported",
                    "A violation report was created for guest " + fallback(guestName, "Guest") + ".",
                    COLLECTION_REPORTS
            );
        }
    }

    private void writeReviewNotifications(@NonNull String reportId, @NonNull String status) {
        firestore.collection(COLLECTION_REPORTS)
                .document(reportId)
                .get()
                .addOnSuccessListener(doc -> {
                    ViolationReport report = ViolationReport.fromMap(doc.getId(), doc.getData());
                    String statusLabel = status.replace('_', ' ');
                    if (ViolationReport.SUBJECT_STUDENT.equalsIgnoreCase(report.getSubjectType())) {
                        notificationWriter.write(
                                report.getSubjectStudentUid(),
                                "violation_review_" + status,
                                reportId,
                                "Violation Review Completed",
                                "Your violation review was completed: " + statusLabel + ".",
                                COLLECTION_REPORTS
                        );
                        return;
                    }
                    if (UserNotificationWriter.supportsRole(report.getSponsorRole())) {
                        notificationWriter.write(
                                report.getSponsorUid(),
                                "violation_review_" + status,
                                reportId,
                                "Guest Violation Review Completed",
                                "Review for guest " + fallback(report.getGuestName(), "Guest") + " was completed: " + statusLabel + ".",
                                COLLECTION_REPORTS
                        );
                    }
                });
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
	                    String guestPhone = asString(doc.get("guestPhone"));
	                    String passId = doc.getId();
                    String entryRequestId = asString(doc.get("entryRequestId"));
                    String passStatus = asString(doc.get("status"));
                    String sponsorUid = asString(doc.get("sponsorUid"));
                    String sponsorName = asString(doc.get("sponsorName"));
                    String sponsorRole = asString(doc.get("sponsorRole"));
                    String sponsorStudentId = asString(doc.get("sponsorStudentId"));
                    if (sponsorStudentId.isEmpty()) {
                        sponsorStudentId = asString(doc.get("studentId"));
                    }
	                    callback.onFound(guestName, guestPhone, passId, entryRequestId, passStatus, sponsorUid, sponsorName, sponsorRole, sponsorStudentId);
                })
                .addOnFailureListener(callback::onError);
    }

    @Override
    public void findStudentByStudentId(@NonNull String studentId, @NonNull StudentInfoCallback callback) {
        String normalizedStudentId = studentId.trim();
        if (normalizedStudentId.isEmpty()) {
            callback.onNotFound("Enter a valid student ID.");
            return;
        }
        firestore.collection(COLLECTION_USERS)
                .whereEqualTo("studentId", normalizedStudentId)
                .whereEqualTo("role", "student")
                .limit(1)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        callback.onNotFound("No student found with this student ID.");
                        return;
                    }
                    DocumentSnapshot doc = snapshot.getDocuments().get(0);
                    String uid = doc.getId();
                    String name = asString(doc.get("displayName"));
                    String email = asString(doc.get("email"));
                    String foundStudentId = asString(doc.get("studentId"));
                    callback.onFound(uid, name, email, foundStudentId);
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

    @NonNull
    private String fallback(@NonNull String value, @NonNull String fallback) {
        return value.trim().isEmpty() ? fallback : value.trim();
    }
}
