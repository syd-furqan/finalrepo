package com.example.glitch.ui;

import android.content.Context;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.glitch.data.PhoneValidationService;
import com.example.glitch.model.GuestIdentityPolicy;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.List;

/**
 * UI-only input formatters for guest identity fields.
 */
public final class GuestIdentityInputSupport {
    private GuestIdentityInputSupport() {
    }

    public static void attachCnicFormatter(@NonNull TextInputEditText input) {
        attachFormattingWatcher(input, FormatterMode.CNIC, null);
    }

    public static void attachVehiclePlateFormatter(@NonNull TextInputEditText input) {
        attachFormattingWatcher(input, FormatterMode.VEHICLE_PLATE, null);
    }

    /** @deprecated Use attachCountryAwarePhoneFormatter instead. */
    public static void attachPhoneFormatter(@NonNull TextInputEditText input) {
        attachFormattingWatcher(input, FormatterMode.PHONE, null);
    }

    public static void attachGuestNameFilter(@NonNull TextInputEditText input) {
        InputFilter noDigits = (source, start, end, dest, dstart, dend) -> {
            for (int i = start; i < end; i++) {
                if (Character.isDigit(source.charAt(i))) {
                    return "";
                }
            }
            return null;
        };
        InputFilter[] existing = input.getFilters();
        InputFilter[] updated = new InputFilter[existing.length + 1];
        System.arraycopy(existing, 0, updated, 0, existing.length);
        updated[existing.length] = noDigits;
        input.setFilters(updated);
    }

    /**
     * Sets up the country code spinner and attaches a country-aware phone formatter.
     * The formatter re-applies whenever the selected country changes.
     *
     * @param spinner      country code spinner
     * @param phoneInput   national number input field
     * @param phoneLayout  TextInputLayout wrapping phoneInput (for maxLength updates)
     * @param context      context for adapter
     */
    public static void setupCountrySpinnerAndPhoneFormatter(
            @NonNull Spinner spinner,
            @NonNull TextInputEditText phoneInput,
            @NonNull TextInputLayout phoneLayout,
            @NonNull Context context
    ) {
        List<PhoneValidationService.CountryEntry> countries =
                PhoneValidationService.getSupportedCountries();

        ArrayAdapter<PhoneValidationService.CountryEntry> adapter = new ArrayAdapter<>(
                context,
                android.R.layout.simple_spinner_item,
                countries
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        // Holds the active country; updated on spinner selection
        final PhoneValidationService.CountryEntry[] activeCountry =
                {PhoneValidationService.getDefaultCountry()};

        // Attach the formatting watcher (country-aware)
        final boolean[] selfChange = {false};
        phoneInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int i, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int i, int b, int c) {}

            @Override
            public void afterTextChanged(Editable editable) {
                if (selfChange[0]) return;
                String raw = editable == null ? "" : editable.toString();
                String formatted = PhoneValidationService.formatForInput(raw, activeCountry[0]);
                if (raw.equals(formatted)) return;
                selfChange[0] = true;
                phoneInput.setText(formatted);
                phoneInput.setSelection(formatted.length());
                selfChange[0] = false;
            }
        });

        // Update country, maxLength, and reformat when spinner selection changes
        spinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view,
                                       int position, long id) {
                activeCountry[0] = countries.get(position);
                int maxLen = PhoneValidationService.maxInputLength(activeCountry[0]);
                phoneInput.setFilters(new InputFilter[]{new InputFilter.LengthFilter(maxLen)});
                // Reformat whatever is already typed
                String current = phoneInput.getText() == null ? "" : phoneInput.getText().toString();
                String reformatted = PhoneValidationService.formatForInput(current, activeCountry[0]);
                if (!current.equals(reformatted)) {
                    selfChange[0] = true;
                    phoneInput.setText(reformatted);
                    phoneInput.setSelection(reformatted.length());
                    selfChange[0] = false;
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        // Apply initial maxLength for default country
        int initialMax = PhoneValidationService.maxInputLength(activeCountry[0]);
        phoneInput.setFilters(new InputFilter[]{new InputFilter.LengthFilter(initialMax)});
    }

    /**
     * Returns the currently selected CountryEntry from the spinner.
     */
    @NonNull
    public static PhoneValidationService.CountryEntry getSelectedCountry(@NonNull Spinner spinner) {
        Object selected = spinner.getSelectedItem();
        if (selected instanceof PhoneValidationService.CountryEntry) {
            return (PhoneValidationService.CountryEntry) selected;
        }
        return PhoneValidationService.getDefaultCountry();
    }

    private enum FormatterMode { CNIC, VEHICLE_PLATE, PHONE }

    private static void attachFormattingWatcher(
            @NonNull TextInputEditText input,
            FormatterMode mode,
            @Nullable PhoneValidationService.CountryEntry country
    ) {
        final boolean[] selfChange = {false};
        input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable editable) {
                if (selfChange[0]) return;
                String raw = editable == null ? "" : editable.toString();
                String formatted;
                switch (mode) {
                    case CNIC:
                        formatted = GuestIdentityPolicy.formatCnicForInput(raw);
                        break;
                    case PHONE:
                        PhoneValidationService.CountryEntry c =
                                country != null ? country : PhoneValidationService.getDefaultCountry();
                        formatted = PhoneValidationService.formatForInput(raw, c);
                        break;
                    default:
                        formatted = GuestIdentityPolicy.formatVehiclePlateForInput(raw);
                        break;
                }
                if (raw.equals(formatted)) return;
                selfChange[0] = true;
                input.setText(formatted);
                input.setSelection(formatted.length());
                selfChange[0] = false;
            }
        });
    }
}
