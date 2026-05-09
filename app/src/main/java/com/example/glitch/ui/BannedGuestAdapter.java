package com.example.glitch.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.glitch.R;
import com.example.glitch.model.GuestBanRecord;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BannedGuestAdapter extends RecyclerView.Adapter<BannedGuestAdapter.ViewHolder> {
    private final List<GuestBanRecord> bans = new ArrayList<>();
    private OnUnbanClickListener listener;

    public void setListener(OnUnbanClickListener listener) {
        this.listener = listener;
    }

    public void submitList(@NonNull List<GuestBanRecord> items) {
        bans.clear();
        bans.addAll(items);
        notifyDataSetChanged();
    }

    public int indexOfCnic(@NonNull String cnic) {
        String target = cnic.trim();
        if (target.isEmpty()) {
            return RecyclerView.NO_POSITION;
        }
        for (int i = 0; i < bans.size(); i++) {
            GuestBanRecord record = bans.get(i);
            if (target.equals(record.getCnic()) || target.equals(record.getId())) {
                return i;
            }
        }
        return RecyclerView.NO_POSITION;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_banned_guest, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        GuestBanRecord ban = bans.get(position);
        holder.textCnic.setText("CNIC: " + ban.getCnic());
        holder.textStatus.setText(ban.getStatus().toUpperCase(Locale.getDefault()));
        holder.textReason.setText("Reason: " + valueOr(ban.getReasonCode(), "N/A"));
        if (ban.getStartAt() != null) {
            String date = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                    .format(ban.getStartAt().toDate());
            holder.textDate.setText("Banned on: " + date);
        } else {
            holder.textDate.setText("Ban date unknown");
        }
        holder.buttonUnban.setOnClickListener(v -> {
            if (listener != null) listener.onUnban(ban);
        });
    }

    @Override
    public int getItemCount() {
        return bans.size();
    }

    private String valueOr(String value, String fallback) {
        return (value == null || value.trim().isEmpty()) ? fallback : value.trim();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView textCnic, textStatus, textReason, textDate;
        final MaterialButton buttonUnban;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            textCnic = itemView.findViewById(R.id.text_ban_cnic);
            textStatus = itemView.findViewById(R.id.text_ban_status);
            textReason = itemView.findViewById(R.id.text_ban_reason);
            textDate = itemView.findViewById(R.id.text_ban_date);
            buttonUnban = itemView.findViewById(R.id.button_unban);
        }
    }

    public interface OnUnbanClickListener {
        void onUnban(@NonNull GuestBanRecord record);
    }
}
