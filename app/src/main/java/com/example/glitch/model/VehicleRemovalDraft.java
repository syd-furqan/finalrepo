package com.example.glitch.model;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Input payload for submitting a vehicle-removal application.
 */
public class VehicleRemovalDraft {
    private final String requesterUid;
    private final String requesterRole;
    private final String studentCategory;
    private final String linkedVehicleId;
    private final String removalReason;
    private final List<VehicleDocumentInput> evidenceDocs;

    public VehicleRemovalDraft(
            @NonNull String requesterUid,
            @NonNull String requesterRole,
            @NonNull String studentCategory,
            @NonNull String linkedVehicleId,
            @NonNull String removalReason,
            @NonNull List<VehicleDocumentInput> evidenceDocs
    ) {
        this.requesterUid = requesterUid;
        this.requesterRole = requesterRole;
        this.studentCategory = studentCategory;
        this.linkedVehicleId = linkedVehicleId;
        this.removalReason = removalReason;
        this.evidenceDocs = new ArrayList<>(evidenceDocs);
    }

    @NonNull
    public String getRequesterUid() {
        return requesterUid;
    }

    @NonNull
    public String getRequesterRole() {
        return requesterRole;
    }

    @NonNull
    public String getStudentCategory() {
        return studentCategory;
    }

    @NonNull
    public String getLinkedVehicleId() {
        return linkedVehicleId;
    }

    @NonNull
    public String getRemovalReason() {
        return removalReason;
    }

    @NonNull
    public List<VehicleDocumentInput> getEvidenceDocs() {
        return new ArrayList<>(evidenceDocs);
    }
}
