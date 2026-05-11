package com.example.glitch.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.glitch.model.PhoneValidationResult;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

import java.util.ArrayList;
import java.util.List;

/**
 * Validates guest phone numbers using libphonenumber.
 * Produces a PhoneValidationResult with carrier and country metadata for Firestore storage.
 */
public final class PhoneValidationService {

    private static final PhoneNumberUtil PHONE_UTIL = PhoneNumberUtil.getInstance();

    private PhoneValidationService() {}

    // -------------------------------------------------------------------------
    // Supported country list
    // -------------------------------------------------------------------------

    public static final class CountryEntry {
        public final String regionCode;   // ISO 3166-1 alpha-2, e.g. "PK"
        public final String countryName;  // Display name
        public final String dialCode;     // e.g. "+92"
        public final int nationalDigits;  // Expected digit count (excluding leading 0 if any)
        public final int dashAfterDigit;  // Insert dash after this many digits from the start (0 = no dash)

        CountryEntry(String regionCode, String countryName, String dialCode,
                     int nationalDigits, int dashAfterDigit) {
            this.regionCode = regionCode;
            this.countryName = countryName;
            this.dialCode = dialCode;
            this.nationalDigits = nationalDigits;
            this.dashAfterDigit = dashAfterDigit;
        }

        @NonNull
        @Override
        public String toString() {
            return dialCode + "  " + countryName;
        }
    }

    private static final List<CountryEntry> COUNTRIES = new ArrayList<>();

    static {
        // Format: regionCode, displayName, dialCode, nationalDigits, dashAfterDigit
        COUNTRIES.add(new CountryEntry("PK", "Pakistan",      "+92",  10, 3));
        COUNTRIES.add(new CountryEntry("US", "USA",           "+1",   10, 3));
        COUNTRIES.add(new CountryEntry("GB", "United Kingdom","+44",  10, 4));
        COUNTRIES.add(new CountryEntry("AE", "UAE",           "+971",  9, 2));
        COUNTRIES.add(new CountryEntry("SA", "Saudi Arabia",  "+966",  9, 2));
        COUNTRIES.add(new CountryEntry("IN", "India",         "+91",  10, 5));
        COUNTRIES.add(new CountryEntry("CN", "China",         "+86",  11, 3));
        COUNTRIES.add(new CountryEntry("TR", "Turkey",        "+90",  10, 3));
        COUNTRIES.add(new CountryEntry("DE", "Germany",       "+49",  10, 3));
        COUNTRIES.add(new CountryEntry("FR", "France",        "+33",   9, 1));
        COUNTRIES.add(new CountryEntry("CA", "Canada",        "+1",   10, 3));
        COUNTRIES.add(new CountryEntry("AU", "Australia",     "+61",   9, 1));
        COUNTRIES.add(new CountryEntry("NG", "Nigeria",       "+234", 10, 3));
        COUNTRIES.add(new CountryEntry("EG", "Egypt",         "+20",  10, 2));
        COUNTRIES.add(new CountryEntry("BD", "Bangladesh",    "+880", 10, 4));
        COUNTRIES.add(new CountryEntry("AF", "Afghanistan",   "+93",   9, 2));
    }

    @NonNull
    public static List<CountryEntry> getSupportedCountries() {
        return COUNTRIES;
    }

    /** Returns the default country (Pakistan). */
    @NonNull
    public static CountryEntry getDefaultCountry() {
        return COUNTRIES.get(0);
    }

    // -------------------------------------------------------------------------
    // Validation
    // -------------------------------------------------------------------------

    /**
     * Validates a phone number using libphonenumber.
     *
     * @param nationalNumber the digits the user typed (no dial code, may include dash)
     * @param country        the selected country entry
     * @return a PhoneValidationResult with valid=true on success or valid=false with reason
     */
    @NonNull
    public static PhoneValidationResult validate(
            @NonNull String nationalNumber,
            @NonNull CountryEntry country
    ) {
        String digitsOnly = nationalNumber.replaceAll("[^0-9]", "");
        String rawInput = country.dialCode + nationalNumber.trim();

        if (digitsOnly.isEmpty()) {
            return PhoneValidationResult.failure(rawInput, country.dialCode,
                    country.regionCode, "Phone number is empty.");
        }

        // Build the full E.164-ish string for parsing
        String fullNumber = country.dialCode + digitsOnly;

        Phonenumber.PhoneNumber parsed;
        try {
            parsed = PHONE_UTIL.parse(fullNumber, country.regionCode);
        } catch (NumberParseException e) {
            return PhoneValidationResult.failure(rawInput, country.dialCode,
                    country.regionCode, "Cannot parse number: " + e.getErrorType().name());
        }

        boolean isValid = PHONE_UTIL.isValidNumberForRegion(parsed, country.regionCode);
        if (!isValid) {
            // Try the looser global check before giving up
            boolean globallyValid = PHONE_UTIL.isValidNumber(parsed);
            if (!globallyValid) {
                return PhoneValidationResult.failure(
                        rawInput, country.dialCode, country.regionCode,
                        "Invalid number for " + country.countryName + "."
                );
            }
        }

        String e164 = PHONE_UTIL.format(parsed, PhoneNumberUtil.PhoneNumberFormat.E164);
        String national = PHONE_UTIL.format(parsed, PhoneNumberUtil.PhoneNumberFormat.NATIONAL);
        String international = PHONE_UTIL.format(parsed, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL);

        String carrier = "Unknown carrier";
        String countryName = country.countryName;
        String numberType = PHONE_UTIL.getNumberType(parsed).name();

        return PhoneValidationResult.success(
                rawInput,
                country.dialCode,
                country.regionCode,
                countryName,
                carrier,
                e164,
                national,
                international,
                numberType
        );
    }

    /**
     * Formats digits typed by the user for the selected country in real time.
     * e.g. for PK (dashAfterDigit=3): "3001234567" → "300-1234567"
     */
    @NonNull
    public static String formatForInput(@NonNull String rawInput, @NonNull CountryEntry country) {
        String digits = rawInput.replaceAll("[^0-9]", "");
        if (digits.length() > country.nationalDigits) {
            digits = digits.substring(0, country.nationalDigits);
        }
        int dash = country.dashAfterDigit;
        if (dash <= 0 || digits.length() <= dash) {
            return digits;
        }
        return digits.substring(0, dash) + "-" + digits.substring(dash);
    }

    /** Returns max character length of the formatted national input for a given country. */
    public static int maxInputLength(@NonNull CountryEntry country) {
        // digits + 1 dash (if applicable)
        return country.nationalDigits + (country.dashAfterDigit > 0 ? 1 : 0);
    }
}
