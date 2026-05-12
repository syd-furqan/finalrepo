package com.example.glitch.ui;

import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.glitch.R;
import com.example.glitch.model.GuestPass;
import com.example.glitch.model.GuestPassStatusRules;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Read-only details screen for one guest pass record.
 */
public class GuestPassDetailsFragment extends Fragment {
    private static final String ARG_ID = "arg_id";
    private static final String ARG_SPONSOR_UID = "arg_sponsor_uid";
    private static final String ARG_SPONSOR_ROLE = "arg_sponsor_role";
    private static final String ARG_SPONSOR_NAME = "arg_sponsor_name";
    private static final String ARG_SPONSOR_EMAIL = "arg_sponsor_email";
    private static final String ARG_GUEST_NAME = "arg_guest_name";
    private static final String ARG_GUEST_ID = "arg_guest_id";
    private static final String ARG_GUEST_PHONE = "arg_guest_phone";
    private static final String ARG_HAS_VEHICLE = "arg_has_vehicle";
    private static final String ARG_VEHICLE_PLATE = "arg_vehicle_plate";
    private static final String ARG_GUEST_TYPE = "arg_guest_type";
    private static final String ARG_PASS_CODE = "arg_pass_code";
    private static final String ARG_ENTRY_REQUEST_ID = "arg_entry_request_id";
    private static final String ARG_GATE_LABEL = "arg_gate_label";
    private static final String ARG_STATUS = "arg_status";
    private static final String ARG_EXPIRES_AT = "arg_expires_at";
    private static final String ARG_ADMITTED_AT = "arg_admitted_at";
    private static final String ARG_ADMITTED_BY_UID = "arg_admitted_by_uid";
    private static final String ARG_ADMISSION_METHOD = "arg_admission_method";
    private static final String ARG_CREATED_AT = "arg_created_at";
    private static final long TS_UNSET = -1L;

    @NonNull
    public static GuestPassDetailsFragment newInstance(@NonNull GuestPass pass) {
        GuestPassDetailsFragment fragment = new GuestPassDetailsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ID, pass.getId());
        args.putString(ARG_SPONSOR_UID, pass.getSponsorUid());
        args.putString(ARG_SPONSOR_ROLE, pass.getSponsorRole());
        args.putString(ARG_SPONSOR_NAME, pass.getSponsorName());
        args.putString(ARG_SPONSOR_EMAIL, pass.getSponsorEmail());
        args.putString(ARG_GUEST_NAME, pass.getGuestName());
        args.putString(ARG_GUEST_ID, pass.getGuestIdNumber());
        args.putString(ARG_GUEST_PHONE, pass.getGuestPhone());
        args.putBoolean(ARG_HAS_VEHICLE, pass.hasVehicle());
        args.putString(ARG_VEHICLE_PLATE, pass.getVehiclePlate());
        args.putString(ARG_GUEST_TYPE, pass.getGuestType());
        args.putString(ARG_PASS_CODE, pass.getPassCode());
        args.putString(ARG_ENTRY_REQUEST_ID, pass.getEntryRequestId());
        args.putString(ARG_GATE_LABEL, pass.getGateLabel());
        args.putString(ARG_STATUS, pass.getStatus());
        args.putLong(ARG_EXPIRES_AT, asMillis(pass.getExpiresAt()));
        args.putLong(ARG_ADMITTED_AT, asMillis(pass.getAdmittedAt()));
        args.putString(ARG_ADMITTED_BY_UID, pass.getAdmittedByUid());
        args.putString(ARG_ADMISSION_METHOD, pass.getAdmissionMethod());
        args.putLong(ARG_CREATED_AT, asMillis(pass.getCreatedAt()));
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_guest_pass_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Bundle args = requireArguments();

        String passCode = safeArg(args, ARG_PASS_CODE);
        String guestName = safeArg(args, ARG_GUEST_NAME);
        String status = safeArg(args, ARG_STATUS);
        String sponsorName = safeArg(args, ARG_SPONSOR_NAME);
        String sponsorEmail = safeArg(args, ARG_SPONSOR_EMAIL);
        String sponsorRole = safeArg(args, ARG_SPONSOR_ROLE);
        String guestId = safeArg(args, ARG_GUEST_ID);
        String guestPhone = safeArg(args, ARG_GUEST_PHONE);
        boolean hasVehicle = args.getBoolean(ARG_HAS_VEHICLE, false);
        long createdAt = args.getLong(ARG_CREATED_AT, TS_UNSET);

        TextView textSummary = view.findViewById(R.id.text_pass_details_summary);
        TextView textMeta = view.findViewById(R.id.text_pass_details_meta);
        TextView textBody = view.findViewById(R.id.text_pass_details_body);
        ImageView imageQr = view.findViewById(R.id.image_pass_details_qr);
        MaterialButton buttonShare = view.findViewById(R.id.button_share_pass_details);

        RoleNavRouter.bindBottomNav(view, this, RoleDestination.PASSES);

        textSummary.setText(guestName + " • " + status.toUpperCase(Locale.getDefault()));
        textMeta.setText(getString(R.string.pass_code_label, passCode));
        try {
            imageQr.setImageBitmap(QrCodeHelper.generate(passCode, 420));
        } catch (Exception exception) {
            imageQr.setImageResource(android.R.drawable.ic_menu_report_image);
        }

        List<Field> fields = new ArrayList<>();
        fields.add(new Field("CNIC", fallback(guestId)));
        fields.add(new Field("Phone", fallback(guestPhone)));
        fields.add(new Field("Has Vehicle", hasVehicle ? "Yes" : "No"));
        fields.add(new Field("Created At", formatMillis(createdAt)));
        fields.add(new Field("Sponsor", fallback(sponsorName)));
        fields.add(new Field("Sponsor Email", fallback(sponsorEmail)));
        fields.add(new Field("Sponsor Type", formatRole(sponsorRole)));
        textBody.setText(buildBody(fields));

        boolean shareable = !GuestPassStatusRules.isArchivedStatus(status);
        buttonShare.setVisibility(shareable ? View.VISIBLE : View.GONE);
        buttonShare.setOnClickListener(v -> {
            try {
                PassShareHelper.share(this, toPass(args));
            } catch (Exception exception) {
                Snackbar.make(requireView(), R.string.archived_pass_not_shareable, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    @NonNull
    private GuestPass toPass(@NonNull Bundle args) {
        return new GuestPass(
                safeArg(args, ARG_ID),
                safeArg(args, ARG_SPONSOR_UID),
                safeArg(args, ARG_SPONSOR_ROLE),
                safeArg(args, ARG_SPONSOR_NAME),
                safeArg(args, ARG_SPONSOR_EMAIL),
                safeArg(args, ARG_GUEST_NAME),
                safeArg(args, ARG_GUEST_ID),
                safeArg(args, ARG_GUEST_PHONE),
                args.getBoolean(ARG_HAS_VEHICLE, false),
                safeArg(args, ARG_VEHICLE_PLATE),
                safeArg(args, ARG_GUEST_TYPE),
                safeArg(args, ARG_PASS_CODE),
                safeArg(args, ARG_ENTRY_REQUEST_ID),
                safeArg(args, ARG_GATE_LABEL),
                safeArg(args, ARG_STATUS),
                asTimestamp(args.getLong(ARG_EXPIRES_AT, TS_UNSET)),
                asTimestamp(args.getLong(ARG_ADMITTED_AT, TS_UNSET)),
                safeArg(args, ARG_ADMITTED_BY_UID),
                safeArg(args, ARG_ADMISSION_METHOD),
                asTimestamp(args.getLong(ARG_CREATED_AT, TS_UNSET))
        );
    }

    @Nullable
    private Timestamp asTimestamp(long millis) {
        if (millis <= TS_UNSET) {
            return null;
        }
        return new Timestamp(new java.util.Date(millis));
    }

    @NonNull
    private String safeArg(@NonNull Bundle args, @NonNull String key) {
        String value = args.getString(key);
        return value == null ? "" : value;
    }

    @NonNull
    private String formatMillis(long millis) {
        if (millis <= TS_UNSET) {
            return "Not available";
        }
        return new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(new java.util.Date(millis));
    }

    @NonNull
    private static String fallback(@NonNull String value) {
        return value.trim().isEmpty() ? "Not available" : value;
    }

    @NonNull
    private CharSequence buildBody(@NonNull List<Field> fields) {
        SpannableStringBuilder builder = new SpannableStringBuilder();
        for (int i = 0; i < fields.size(); i++) {
            Field field = fields.get(i);
            int labelStart = builder.length();
            builder.append(field.label).append(": ");
            builder.setSpan(
                    new StyleSpan(Typeface.BOLD),
                    labelStart,
                    builder.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
            builder.append(field.value);
            if (i < fields.size() - 1) {
                builder.append("\n");
            }
        }
        return builder;
    }

    @NonNull
    private String formatRole(@NonNull String role) {
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

    private static long asMillis(@Nullable Timestamp timestamp) {
        if (timestamp == null) {
            return TS_UNSET;
        }
        return timestamp.toDate().getTime();
    }

    private static final class Field {
        private final String label;
        private final String value;

        private Field(@NonNull String label, @NonNull String value) {
            this.label = label;
            this.value = value;
        }
    }
}
