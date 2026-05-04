package com.example.glitch.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Input payload for submitting a vehicle registration application.
 */
public class VehicleRegistrationDraft {
    private final String requesterUid;
    private final String requesterRole;
    private final String studentCategory;
    private final String plateNumber;
    private final String vehicleMake;
    private final String vehicleModel;
    private final String vehicleVariant;
    private final boolean owner;
    private final VehicleDocumentInput applicantCnicDoc;
    private final VehicleDocumentInput registrationDoc;
    private final VehicleDocumentInput ownerCnicDoc;

    public VehicleRegistrationDraft(
            @NonNull String requesterUid,
            @NonNull String requesterRole,
            @NonNull String studentCategory,
            @NonNull String plateNumber,
            @NonNull String vehicleMake,
            @NonNull String vehicleModel,
            @NonNull String vehicleVariant,
            boolean owner,
            @NonNull VehicleDocumentInput applicantCnicDoc,
            @NonNull VehicleDocumentInput registrationDoc,
            @Nullable VehicleDocumentInput ownerCnicDoc
    ) {
        this.requesterUid = requesterUid;
        this.requesterRole = requesterRole;
        this.studentCategory = studentCategory;
        this.plateNumber = plateNumber;
        this.vehicleMake = vehicleMake;
        this.vehicleModel = vehicleModel;
        this.vehicleVariant = vehicleVariant;
        this.owner = owner;
        this.applicantCnicDoc = applicantCnicDoc;
        this.registrationDoc = registrationDoc;
        this.ownerCnicDoc = ownerCnicDoc;
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
    public String getPlateNumber() {
        return plateNumber;
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

    public boolean isOwner() {
        return owner;
    }

    @NonNull
    public VehicleDocumentInput getApplicantCnicDoc() {
        return applicantCnicDoc;
    }

    @NonNull
    public VehicleDocumentInput getRegistrationDoc() {
        return registrationDoc;
    }

    @Nullable
    public VehicleDocumentInput getOwnerCnicDoc() {
        return ownerCnicDoc;
    }
}
