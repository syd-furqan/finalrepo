package com.example.glitch.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.glitch.R;
import com.example.glitch.model.FineCaseRecord;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Recycler adapter for admin charges list.
 */
public class AdminChargeAdapter extends RecyclerView.Adapter<AdminChargeAdapter.ViewHolder> {
    private final List<FineCaseRecord> charges = new ArrayList<>();
    private ChargeActionListener listener;

    public void setListener(ChargeActionListener listener) {
        this.listener = listener;
    }

    public void submitList(@NonNull List<FineCaseRecord> items) {
        charges.clear();
        charges.addAll(items);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_charge, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FineCaseRecord charge = charges.get(position);
        String status = charge.getChargeDisplayStatus();
        holder.textChargeId.setText("Charge: " + charge.getId());
        holder.textStatus.setText(status.toUpperCase(Locale.getDefault()));
        holder.textRequest.setText("Request: " + valueOr(charge.getRequestId(), "N/A"));
        holder.textGuest.setText("Visitor: " + valueOr(charge.getGuestName(), "Unknown")
                + " (" + valueOr(charge.getGuestIdNumber(), "N/A") + ")");
        holder.textSponsor.setText("Sponsor UID: " + valueOr(charge.getSponsorUid(), "N/A"));
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onChargeSelected(charge);
            }
        });
    }

    @Override
    public int getItemCount() {
        return charges.size();
    }

    private String valueOr(String value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? fallback : trimmed;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView textChargeId;
        final TextView textStatus;
        final TextView textRequest;
        final TextView textGuest;
        final TextView textSponsor;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            textChargeId = itemView.findViewById(R.id.text_charge_id);
            textStatus = itemView.findViewById(R.id.text_charge_status);
            textRequest = itemView.findViewById(R.id.text_charge_request);
            textGuest = itemView.findViewById(R.id.text_charge_guest);
            textSponsor = itemView.findViewById(R.id.text_charge_sponsor);
        }
    }

    public interface ChargeActionListener {
        void onChargeSelected(@NonNull FineCaseRecord record);
    }
}
