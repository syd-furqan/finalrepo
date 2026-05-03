package com.example.glitch.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.glitch.R;
import com.example.glitch.model.EntryRequest;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * RecyclerView adapter for active entry request cards.
 * Pattern: Adapter + ViewHolder for one-card-per-request rendering.
 * Updated to visually highlight overdue requests.
 */
public class EntryRequestAdapter extends RecyclerView.Adapter<EntryRequestAdapter.EntryRequestViewHolder> {
    private final List<EntryRequest> requests = new ArrayList<>();
    private final EntryActionListener listener;
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm a", Locale.getDefault());

    public EntryRequestAdapter(@NonNull EntryActionListener listener) {
        this.listener = listener;
    }

    /**
     * Replaces current list and refreshes visible cards.
     */
    public void submitList(@NonNull List<EntryRequest> updatedRequests) {
        requests.clear();
        requests.addAll(updatedRequests);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public EntryRequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_entry_request, parent, false);
        return new EntryRequestViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EntryRequestViewHolder holder, int position) {
        EntryRequest request = requests.get(position);
        boolean isOverdue = "overdue".equalsIgnoreCase(request.getStatus());

        holder.textName.setText(request.getFullName());
        holder.textRoleChip.setText(isOverdue ? "OVERDUE" : request.getRoleTag());
        
        // Visual indicator for overdue status
        if (isOverdue) {
            holder.textRoleChip.setBackgroundResource(R.drawable.bg_chip_role_danger); // We will need to create this or use a color
            holder.textRoleChip.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.white));
            holder.textEntered.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.danger_red));
        } else {
            holder.textRoleChip.setBackgroundResource(R.drawable.bg_chip_role);
            holder.textRoleChip.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.primary_navy));
            holder.textEntered.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.text_dark));
        }

        String hostText = holder.itemView.getContext().getString(R.string.unknown_host_prefix, request.getHostName());
        holder.textHost.setText(hostText);
        holder.textGate.setText(request.getGateLabel());
        holder.textEntered.setText(formatTimestamp(request.getEnteredAt()));
        holder.imageRequestIcon.setImageResource(iconForType(request.getIconType()));

        holder.buttonDetails.setOnClickListener(view -> listener.onDetailsClicked(request));
    }

    @Override
    public int getItemCount() {
        return requests.size();
    }

    @NonNull
    private String formatTimestamp(Timestamp timestamp) {
        if (timestamp == null) {
            return "--:--";
        }
        return timeFormat.format(timestamp.toDate());
    }

    @DrawableRes
    private int iconForType(@NonNull String iconType) {
        String normalized = iconType.toLowerCase(Locale.getDefault());
        if (normalized.contains("service")) {
            return android.R.drawable.ic_menu_manage;
        }
        if (normalized.contains("contract")) {
            return android.R.drawable.ic_menu_upload;
        }
        return android.R.drawable.ic_menu_myplaces;
    }

    /**
     * View holder for one entry request row.
     */
    static class EntryRequestViewHolder extends RecyclerView.ViewHolder {
        final ImageView imageRequestIcon;
        final TextView textName;
        final TextView textRoleChip;
        final TextView textHost;
        final TextView textGate;
        final TextView textEntered;
        final MaterialButton buttonDetails;

        EntryRequestViewHolder(@NonNull View itemView) {
            super(itemView);
            imageRequestIcon = itemView.findViewById(R.id.image_request_icon);
            textName = itemView.findViewById(R.id.text_name);
            textRoleChip = itemView.findViewById(R.id.text_role_chip);
            textHost = itemView.findViewById(R.id.text_host);
            textGate = itemView.findViewById(R.id.text_gate);
            textEntered = itemView.findViewById(R.id.text_entered);
            buttonDetails = itemView.findViewById(R.id.button_details);
        }
    }

    public interface EntryActionListener {
        void onDetailsClicked(@NonNull EntryRequest request);
    }
}
