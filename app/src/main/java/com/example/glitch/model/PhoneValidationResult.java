package com.example.glitch.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Result of libphonenumber validation for a guest phone number.
 * Stored in Firestore under the guest pass document for investigation purposes.
 */
public final class PhoneValidationResult {
    private final boolean valid;
    private final String rawInput;
    private final String countryCode;
    private final String regionCode;
    private final String countryName;
    private final String carrierName;
    private final String formattedE164;
    private final String formattedNational;
    private final String formattedInternational;
    private final String numberType;
    private final String failureReason;

    private PhoneValidationResult(
            boolean valid,
            @NonNull String rawInput,
            @NonNull String countryCode,
            @NonNull String regionCode,
            @NonNull String countryName,
            @NonNull String carrierName,
            @NonNull String formattedE164,
            @NonNull String formattedNational,
            @NonNull String formattedInternational,
            @NonNull String numberType,
            @NonNull String failureReason
    ) {
        this.valid = valid;
        this.rawInput = rawInput;
        this.countryCode = countryCode;
        this.regionCode = regionCode;
        this.countryName = countryName;
        this.carrierName = carrierName;
        this.formattedE164 = formattedE164;
        this.formattedNational = formattedNational;
        this.formattedInternational = formattedInternational;
        this.numberType = numberType;
        this.failureReason = failureReason;
    }

    @NonNull
    public static PhoneValidationResult success(
            @NonNull String rawInput,
            @NonNull String countryCode,
            @NonNull String regionCode,
            @NonNull String countryName,
            @NonNull String carrierName,
            @NonNull String formattedE164,
            @NonNull String formattedNational,
            @NonNull String formattedInternational,
            @NonNull String numberType
    ) {
        return new PhoneValidationResult(
                true, rawInput, countryCode, regionCode, countryName,
                carrierName, formattedE164, formattedNational, formattedInternational,
                numberType, ""
        );
    }

    @NonNull
    public static PhoneValidationResult failure(
            @NonNull String rawInput,
            @NonNull String countryCode,
            @NonNull String regionCode,
            @NonNull String failureReason
    ) {
        return new PhoneValidationResult(
                false, rawInput, countryCode, regionCode, "", "",
                "", "", "", "", failureReason
        );
    }

    public boolean isValid() { return valid; }

    @NonNull public String getRawInput() { return rawInput; }
    @NonNull public String getCountryCode() { return countryCode; }
    @NonNull public String getRegionCode() { return regionCode; }
    @NonNull public String getCountryName() { return countryName; }
    @NonNull public String getCarrierName() { return carrierName; }
    @NonNull public String getFormattedE164() { return formattedE164; }
    @NonNull public String getFormattedNational() { return formattedNational; }
    @NonNull public String getFormattedInternational() { return formattedInternational; }
    @NonNull public String getNumberType() { return numberType; }
    @NonNull public String getFailureReason() { return failureReason; }

    /** Converts to a flat map suitable for Firestore storage. */
    @NonNull
    public Map<String, Object> toFirestoreMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("valid", valid);
        map.put("rawInput", rawInput);
        map.put("countryCode", countryCode);
        map.put("regionCode", regionCode);
        map.put("countryName", countryName);
        map.put("carrierName", carrierName);
        map.put("formattedE164", formattedE164);
        map.put("formattedNational", formattedNational);
        map.put("formattedInternational", formattedInternational);
        map.put("numberType", numberType);
        map.put("failureReason", failureReason);
        return map;
    }
}
