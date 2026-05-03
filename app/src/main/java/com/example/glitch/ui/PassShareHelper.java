package com.example.glitch.ui;

import android.content.Intent;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.example.glitch.R;
import com.example.glitch.model.GatePolicy;
import com.example.glitch.model.GuestPass;
import com.example.glitch.model.GuestPassStatusRules;

import java.io.File;
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
                "Access Number (Pass Code): " + passCode + "\n"
                        + "Guest: " + pass.getGuestName() + "\n"
                        + "CNIC: " + pass.getGuestIdNumber() + "\n"
                        + "Guest Type: " + pass.getGuestType() + "\n"
                        + "Vehicle Plate: " + (pass.hasVehicle() ? pass.getVehiclePlate() : "N/A") + "\n"
                        + "Gate: " + GatePolicy.toDisplayLabel(pass.getGateLabel()) + "\n"
                        + "Entry Request ID: " + pass.getEntryRequestId() + "\n"
                        + "Use this pass code if QR scan is unavailable.";

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("image/png");
        intent.putExtra(Intent.EXTRA_SUBJECT, fragment.getString(R.string.feature_student_guest_pass));
        intent.putExtra(Intent.EXTRA_TEXT, shareText);
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        fragment.startActivity(Intent.createChooser(intent, fragment.getString(R.string.share_action)));
    }
}
