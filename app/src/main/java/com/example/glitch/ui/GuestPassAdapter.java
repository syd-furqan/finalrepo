package com.example.glitch.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.glitch.R;
import com.example.glitch.model.GuestPass;
import com.example.glitch.model.GuestPassStatusRules;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * RecyclerView adapter for guest pass history and cancellation actions.
 * Pattern: Adapter + callback listener for row-level actions.
 * Known issue: QR image rendering is represented by pass code text in v1.
 */
public class GuestPassAdapter extends RecyclerView.Adapter<GuestPassAdapter.GuestPassViewHolder> {
    public interface GuestPassActionListener {
        void onCancelPass(@NonNull GuestPass pass);

        void onSharePass(@NonNull GuestPass pass);

        void onViewPassDetails(@NonNull GuestPass pass);
    }

    private final GuestPassActionListener listener;
    private final List<GuestPass> items = new ArrayList<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

    public GuestPassAdapter(@NonNull GuestPassActionListener listener) {
        this.listener = listener;
    }

    public void submitList(@NonNull List<GuestPass> passes) {
        items.clear();
        items.addAll(passes);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public GuestPassViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_guest_pass, parent, false);
        return new GuestPassViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GuestPassViewHolder holder, int position) {
        GuestPass pass = items.get(position);
        holder.textGuestName.setText(pass.getGuestName());
        holder.textPassCode.setText(holder.itemView.getContext().getString(R.string.pass_code_label, pass.getPassCode()));
        try {
            holder.imagePassQr.setImageBitmap(QrCodeHelper.generate(pass.getPassCode(), 360));
        } catch (Exception exception) {
            holder.imagePassQr.setImageResource(android.R.drawable.ic_menu_report_image);
        }
        String status = pass.getStatus() == null ? "" : pass.getStatus();
        holder.textStatus.setText(status.toUpperCase(Locale.getDefault()));
        ChipStyle chipStyle = resolveStatusStyle(status);
        holder.textStatus.setBackgroundResource(chipStyle.backgroundRes);
        holder.textStatus.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), chipStyle.textColorRes));
        if (pass.getExpiresAt() != null) {
            holder.textExpiry.setText(holder.itemView.getContext().getString(
                    R.string.pass_expires_label,
                    dateFormat.format(pass.getExpiresAt().toDate())
            ));
        } else {
            holder.textExpiry.setText(R.string.pass_expiry_unknown);
        }
        boolean active = "active".equalsIgnoreCase(pass.getStatus());
        holder.buttonCancel.setVisibility(active ? View.VISIBLE : View.GONE);
        holder.buttonCancel.setOnClickListener(v -> listener.onCancelPass(pass));
        boolean shareable = GuestPassStatusRules.isShareable(pass);
        holder.buttonShare.setVisibility(shareable ? View.VISIBLE : View.GONE);
        holder.buttonShare.setOnClickListener(v -> listener.onSharePass(pass));
        holder.buttonDetails.setOnClickListener(v -> listener.onViewPassDetails(pass));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class GuestPassViewHolder extends RecyclerView.ViewHolder {
        final TextView textGuestName;
        final TextView textPassCode;
        final ImageView imagePassQr;
        final TextView textExpiry;
        final TextView textStatus;
        final MaterialButton buttonShare;
        final MaterialButton buttonDetails;
        final MaterialButton buttonCancel;

        GuestPassViewHolder(@NonNull View itemView) {
            super(itemView);
            textGuestName = itemView.findViewById(R.id.text_pass_guest_name);
            textPassCode = itemView.findViewById(R.id.text_pass_code);
            imagePassQr = itemView.findViewById(R.id.image_pass_qr);
            textExpiry = itemView.findViewById(R.id.text_pass_expiry);
            textStatus = itemView.findViewById(R.id.text_pass_status);
            buttonShare = itemView.findViewById(R.id.button_share_pass);
            buttonDetails = itemView.findViewById(R.id.button_view_pass_details);
            buttonCancel = itemView.findViewById(R.id.button_cancel_pass);
        }
    }

    @NonNull
    static ChipStyle resolveStatusStyle(@NonNull String rawStatus) {
        String status = rawStatus.trim().toLowerCase(Locale.getDefault());
        if ("active".equals(status)) {
            return new ChipStyle(R.drawable.bg_chip_success, R.color.success_green);
        }
        if ("cancelled".equals(status) || "revoked".equals(status)
                || "expired".equals(status) || "denied".equals(status)) {
            return new ChipStyle(R.drawable.bg_chip_alert_critical, R.color.danger_red);
        }
        if ("used".equals(status)) {
            return new ChipStyle(R.drawable.bg_chip_role, R.color.nav_unselected);
        }
        return new ChipStyle(R.drawable.bg_chip_role, R.color.primary_navy);
    }

    static class ChipStyle {
        final int backgroundRes;
        final int textColorRes;

        ChipStyle(int backgroundRes, int textColorRes) {
            this.backgroundRes = backgroundRes;
            this.textColorRes = textColorRes;
        }
    }
}
