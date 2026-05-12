package com.example.glitch.ui;

import android.content.Intent;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.example.glitch.R;
import com.example.glitch.model.GuestPass;
import com.example.glitch.model.GuestPassStatusRules;
import com.google.firebase.Timestamp;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Shares guest pass details and QR image using standard Android share sheet.
 */
public final class PassShareHelper {
    private PassShareHelper() {
    }

    public static void share(@NonNull Fragment fragment, @NonNull GuestPass pass) throws Exception {
        if (!GuestPassStatusRules.isShareable(pass)) {
            throw new IllegalStateException("Archived passes cannot be shared.");
        }
        String passCode = pass.getPassCode().trim().toUpperCase(Locale.getDefault());
        File qrFile = QrCodeHelper.saveToCache(
                fragment.requireContext(),
                PassCardImageHelper.render(pass),
                "guest_pass_card_" + passCode + ".png"
        );
        Uri uri = FileProvider.getUriForFile(
                fragment.requireContext(),
                fragment.requireContext().getPackageName() + ".fileprovider",
                qrFile
        );

        String shareText =
                "Pass Code: " + passCode + "\n"
                        + "CNIC: " + pass.getGuestIdNumber() + "\n"
                        + "Phone: " + pass.getGuestPhone() + "\n"
                        + "Has Vehicle: " + (pass.hasVehicle() ? "Yes" : "No") + "\n"
                        + "Created At: " + formatTimestamp(pass.getCreatedAt()) + "\n"
                        + "Sponsor: " + fallback(pass.getSponsorName()) + "\n"
                        + "Sponsor Email: " + fallback(pass.getSponsorEmail()) + "\n"
                        + "Sponsor Type: " + formatRole(pass.getSponsorRole()) + "\n"
                        + "Use this pass code if QR scan is unavailable.";

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("image/png");
        intent.putExtra(Intent.EXTRA_SUBJECT, fragment.getString(R.string.feature_student_guest_pass));
        intent.putExtra(Intent.EXTRA_TEXT, shareText);
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        fragment.startActivity(Intent.createChooser(intent, fragment.getString(R.string.share_action)));
    }

    @NonNull
    private static String formatTimestamp(Timestamp timestamp) {
        if (timestamp == null) {
            return "Not available";
        }
        return new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                .format(timestamp.toDate());
    }

    @NonNull
    private static String formatRole(@NonNull String role) {
        String normalized = role.trim().toLowerCase(Locale.getDefault());
        if (normalized.isEmpty()) {
            return "Not available";
        }
        if (normalized.length() == 1) {
            return normalized.toUpperCase(Locale.getDefault());
        }
        return normalized.substring(0, 1).toUpperCase(Locale.getDefault())
                + normalized.substring(1);
    }

    @NonNull
    private static String fallback(@NonNull String value) {
        String trimmed = value.trim();
        return trimmed.isEmpty() ? "Not available" : trimmed;
    }
}
