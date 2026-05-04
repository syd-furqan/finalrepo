package com.example.glitch.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Vehicle registration/removal application record.
 * Backward compatible with legacy staff vehicle-request documents.
 */
public class VehicleRequestRecord {
    public static final String KIND_REGISTER = "register";
    public static final String KIND_REMOVE = "remove";

    public static final String STATUS_SUBMITTED = "submitted";
    public static final String STATUS_RECEIVED = "received";
    public static final String STATUS_APPROVED = "approved";
    public static final String STATUS_DENIED = "denied";
    public static final String STATUS_CANCELLED = "cancelled";

    private final String id;
    private final String requesterUid;
    private final String requesterRole;
    private final String studentCategory;
    private final String stickerType;
    private final String requestKind;
    private final String plateNumber;
    private final String plateKey;
    private final String vehicleMake;
    private final String vehicleModel;
    private final String vehicleVariant;
    private final String vehicleColor;
    private final boolean owner;
    private final String linkedVehicleId;
    private final String removalReason;
    private final String status;
    private final String reviewerUid;
    private final String receivedByUid;
    private final String reviewNote;
    private final Timestamp createdAt;
    private final Timestamp updatedAt;
    private final Timestamp receivedAt;
    private final Timestamp reviewedAt;
    private final VehicleDocumentRef applicantCnicDoc;
    private final VehicleDocumentRef registrationDoc;
    private final VehicleDocumentRef ownerCnicDoc;
    private final List<VehicleDocumentRef> evidenceDocs;

    public VehicleRequestRecord(
            @NonNull String id,
            @NonNull String requesterUid,
            @NonNull String requesterRole,
            @NonNull String studentCategory,
            @NonNull String stickerType,
            @NonNull String requestKind,
            @NonNull String plateNumber,
            @NonNull String plateKey,
            @NonNull String vehicleMake,
            @NonNull String vehicleModel,
            @NonNull String vehicleVariant,
            @NonNull String vehicleColor,
            boolean owner,
            @NonNull String linkedVehicleId,
            @NonNull String removalReason,
            @NonNull String status,
            @NonNull String reviewerUid,
            @NonNull String receivedByUid,
            @NonNull String reviewNote,
            @Nullable Timestamp createdAt,
            @Nullable Timestamp updatedAt,
            @Nullable Timestamp receivedAt,
            @Nullable Timestamp reviewedAt,
            @NonNull VehicleDocumentRef applicantCnicDoc,
            @NonNull VehicleDocumentRef registrationDoc,
            @NonNull VehicleDocumentRef ownerCnicDoc,
            @NonNull List<VehicleDocumentRef> evidenceDocs
    ) {
        this.id = id;
        this.requesterUid = requesterUid;
        this.requesterRole = requesterRole;
        this.studentCategory = studentCategory;
        this.stickerType = stickerType;
        this.requestKind = requestKind;
        this.plateNumber = plateNumber;
        this.plateKey = plateKey;
        this.vehicleMake = vehicleMake;
        this.vehicleModel = vehicleModel;
        this.vehicleVariant = vehicleVariant;
        this.vehicleColor = vehicleColor;
        this.owner = owner;
        this.linkedVehicleId = linkedVehicleId;
        this.removalReason = removalReason;
        this.status = status;
        this.reviewerUid = reviewerUid;
        this.receivedByUid = receivedByUid;
        this.reviewNote = reviewNote;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.receivedAt = receivedAt;
        this.reviewedAt = reviewedAt;
        this.applicantCnicDoc = applicantCnicDoc;
        this.registrationDoc = registrationDoc;
        this.ownerCnicDoc = ownerCnicDoc;
        this.evidenceDocs = evidenceDocs;
    }

    @NonNull
    public static VehicleRequestRecord fromMap(@NonNull String id, @Nullable Map<String, Object> map) {
        if (map == null) {
            return empty(id);
        }
        String plateNumber = asString(map.get("plateNumber"));
        String plateKey = asString(map.get("plateKey"));
        if (plateKey.isEmpty()) {
            plateKey = plateNumber;
        }
        String kind = normalizeKind(asString(map.get("requestKind")));
        String rawStatus = asString(map.get("status"));
        String status = normalizeStatus(rawStatus);
        List<VehicleDocumentRef> evidence = parseDocList(map.get("evidenceDocs"));
        if (evidence.isEmpty() && map.get("evidenceDoc") != null) {
            VehicleDocumentRef single = VehicleDocumentRef.fromMap(map.get("evidenceDoc"));
            if (!single.getStoragePath().isEmpty() || !single.getDownloadUrl().isEmpty()) {
                evidence.add(single);
            }
        }

        return new VehicleRequestRecord(
                id,
                asString(map.get("requesterUid")),
                asString(map.get("requesterRole")),
                asString(map.get("studentCategory")),
                asString(map.get("stickerType")),
                kind,
                plateNumber,
                plateKey,
                asString(map.get("vehicleMake")),
                asString(map.get("vehicleModel")),
                asString(map.get("vehicleVariant")),
                asString(map.get("vehicleColor")),
                asBoolean(map.get("isOwner")),
                asString(map.get("linkedVehicleId")),
                asString(map.get("removalReason")),
                status,
                asString(map.get("reviewerUid")),
                asString(map.get("receivedByUid")),
                asString(map.get("reviewNote")),
                asTimestamp(map.get("createdAt")),
                asTimestamp(map.get("updatedAt")),
                asTimestamp(map.get("receivedAt")),
                asTimestamp(map.get("reviewedAt")),
                VehicleDocumentRef.fromMap(map.get("applicantCnicDoc")),
                VehicleDocumentRef.fromMap(map.get("registrationDoc")),
                VehicleDocumentRef.fromMap(map.get("ownerCnicDoc")),
                evidence
        );
    }

    @NonNull
    private static VehicleRequestRecord empty(@NonNull String id) {
        return new VehicleRequestRecord(
                id,
                "",
                "",
                "",
                "",
                KIND_REGISTER,
                "",
                "",
                "",
                "",
                "",
                "",
                true,
                "",
                "",
                STATUS_SUBMITTED,
                "",
                "",
                "",
                null,
                null,
                null,
                null,
                new VehicleDocumentRef("", "", "", ""),
                new VehicleDocumentRef("", "", "", ""),
                new VehicleDocumentRef("", "", "", ""),
                new ArrayList<>()
        );
    }

    @NonNull
    private static String normalizeKind(@NonNull String raw) {
        String normalized = raw.trim().toLowerCase(Locale.getDefault());
        if (KIND_REMOVE.equals(normalized)) {
            return KIND_REMOVE;
        }
        return KIND_REGISTER;
    }

    @NonNull
    private static String normalizeStatus(@NonNull String raw) {
        String normalized = raw.trim().toLowerCase(Locale.getDefault());
        if ("pending".equals(normalized)) {
            return STATUS_SUBMITTED;
        }
        if (STATUS_RECEIVED.equals(normalized)
                || STATUS_APPROVED.equals(normalized)
                || STATUS_DENIED.equals(normalized)
                || STATUS_CANCELLED.equals(normalized)
                || STATUS_SUBMITTED.equals(normalized)) {
            return normalized;
        }
        return STATUS_SUBMITTED;
    }

    @NonNull
    @SuppressWarnings("unchecked")
    private static List<VehicleDocumentRef> parseDocList(@Nullable Object raw) {
        List<VehicleDocumentRef> result = new ArrayList<>();
        if (!(raw instanceof List)) {
            return result;
        }
        List<Object> list = (List<Object>) raw;
        for (Object item : list) {
            result.add(VehicleDocumentRef.fromMap(item));
        }
        return result;
    }

    public boolean isSubmitted() {
        return STATUS_SUBMITTED.equalsIgnoreCase(status);
    }

    public boolean isReceived() {
        return STATUS_RECEIVED.equalsIgnoreCase(status);
    }

    public boolean isApproved() {
        return STATUS_APPROVED.equalsIgnoreCase(status);
    }

    public boolean isDenied() {
        return STATUS_DENIED.equalsIgnoreCase(status);
    }

    public boolean isCancelled() {
        return STATUS_CANCELLED.equalsIgnoreCase(status);
    }

    public boolean isOpenApplication() {
        return isSubmitted() || isReceived();
    }

    public boolean canCancelByApplicant() {
        return isSubmitted();
    }

    public boolean isRegisterRequest() {
        return KIND_REGISTER.equalsIgnoreCase(requestKind);
    }

    public boolean isRemovalRequest() {
        return KIND_REMOVE.equalsIgnoreCase(requestKind);
    }

    /** Backward compatibility for old adapter logic. */
    public boolean isPending() {
        return isSubmitted() || "pending".equalsIgnoreCase(status);
    }

    @NonNull
    public String getId() {
        return id;
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
    public String getStickerType() {
        return stickerType;
    }

    @NonNull
    public String getRequestKind() {
        return requestKind;
    }

    @NonNull
    public String getPlateNumber() {
        return plateNumber;
    }

    @NonNull
    public String getPlateKey() {
        return plateKey.isEmpty() ? plateNumber : plateKey;
    }

    @NonNull
    public String getVehicleMake() {
        return vehicleMake;
    }

    @NonNull
    public String getVehicleModel() {
        return vehicleModel;
    }

    @NonNull
    public String getVehicleVariant() {
        return vehicleVariant;
    }

    @NonNull
    public String getVehicleColor() {
        return vehicleColor;
    }

    public boolean isOwner() {
        return owner;
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
    public String getStatus() {
        return status;
    }

    @NonNull
    public String getReviewerUid() {
        return reviewerUid;
    }

    @NonNull
    public String getReceivedByUid() {
        return receivedByUid;
    }

    @NonNull
    public String getReviewNote() {
        return reviewNote;
    }

    @Nullable
    public Timestamp getCreatedAt() {
        return createdAt;
    }

    @Nullable
    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    @Nullable
    public Timestamp getReceivedAt() {
        return receivedAt;
    }

    @Nullable
    public Timestamp getReviewedAt() {
        return reviewedAt;
    }

    @NonNull
    public VehicleDocumentRef getApplicantCnicDoc() {
        return applicantCnicDoc;
    }

    @NonNull
    public VehicleDocumentRef getRegistrationDoc() {
        return registrationDoc;
    }

    @NonNull
    public VehicleDocumentRef getOwnerCnicDoc() {
        return ownerCnicDoc;
    }

    @NonNull
    public List<VehicleDocumentRef> getEvidenceDocs() {
        return evidenceDocs;
    }

    /** Full make + model + variant string for display. */
    @NonNull
    public String getVehicleDescription() {
        String make = vehicleMake.trim();
        String model = vehicleModel.trim();
        String variant = vehicleVariant.trim();
        String primary;
        if (make.isEmpty()) {
            primary = model;
        } else if (model.isEmpty()) {
            primary = make;
        } else {
            primary = make + " " + model;
        }
        if (variant.isEmpty()) {
            return primary;
        }
        if (primary.isEmpty()) {
            return variant;
        }
        return primary + " " + variant;
    }

    @NonNull
    private static String asString(@Nullable Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static boolean asBoolean(@Nullable Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue() != 0;
        }
        if (value instanceof String) {
            String normalized = ((String) value).trim().toLowerCase(Locale.getDefault());
            return "true".equals(normalized) || "1".equals(normalized) || "yes".equals(normalized);
        }
        return false;
    }

    @Nullable
    private static Timestamp asTimestamp(@Nullable Object value) {
        if (value instanceof Timestamp) {
            return (Timestamp) value;
        }
        return null;
    }
}
