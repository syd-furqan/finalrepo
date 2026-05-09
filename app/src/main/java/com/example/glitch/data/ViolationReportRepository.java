package com.example.glitch.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.glitch.model.ViolationReport;

import java.util.List;

public interface ViolationReportRepository {

    void submitReport(
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
            @NonNull String subjectStudentId,
            @NonNull OperationCallback callback
    );

    void listenAllReports(@NonNull ReportListListener listener);

    void listenReportsByReporter(@NonNull String uid, @NonNull ReportListListener listener);

    void ignoreReport(@NonNull String reportId, @NonNull String adminUid, @NonNull OperationCallback callback);

    void markActioned(@NonNull String reportId, @NonNull String adminUid, @NonNull OperationCallback callback);

    void findActivePasForCnic(@NonNull String cnic, @NonNull PassInfoCallback callback);

    void findStudentByStudentId(@NonNull String studentId, @NonNull StudentInfoCallback callback);

    void removeListeners();

    interface ReportListListener {
        void onData(@NonNull List<ViolationReport> reports);
        void onError(@NonNull Exception exception);
    }

    interface OperationCallback {
        void onComplete(boolean success, @NonNull String message, @Nullable Exception exception);
    }

    interface PassInfoCallback {
        void onFound(@NonNull String guestName, @NonNull String guestPassId, @NonNull String sponsorUid, @NonNull String sponsorName, @NonNull String sponsorRole);
        void onNotFound(@NonNull String message);
        void onError(@NonNull Exception exception);
    }

    interface StudentInfoCallback {
        void onFound(
                @NonNull String studentUid,
                @NonNull String studentName,
                @NonNull String studentEmail,
                @NonNull String studentId
        );
        void onNotFound(@NonNull String message);
        void onError(@NonNull Exception exception);
    }
}
