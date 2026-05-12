package com.example.glitch.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.glitch.R;
import com.example.glitch.model.VehicleRequestRecord;
import com.google.firebase.Timestamp;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Shared list adapter for vehicle applications.
 */
public class VehicleApplicationAdapter extends RecyclerView.Adapter<VehicleApplicationAdapter.ViewHolder> {
    public interface ActionListener {
        void onViewDetails(@NonNull VehicleRequestRecord record);
    }

    private final List<VehicleRequestRecord> items = new ArrayList<>();
    @Nullable
    private final ActionListener actionListener;

    public VehicleApplicationAdapter() {
        this(null);
    }

    public VehicleApplicationAdapter(@Nullable ActionListener actionListener) {
        this.actionListener = actionListener;
    }

    public void submitList(@NonNull List<VehicleRequestRecord> records) {
        items.clear();
        items.addAll(records);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_vehicle_application, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        VehicleRequestRecord record = items.get(position);
        holder.textPlate.setText(record.getPlateNumber());
        holder.textKind.setVisibility(View.GONE);
        holder.textMeta.setText(buildMeta(record));

        String status = record.getStatus();
        VehicleRequestAdapter.ChipStyle chipStyle = VehicleRequestAdapter.resolveStatusStyle(status);
        holder.textStatus.setText(status.toUpperCase(Locale.getDefault()));
        holder.textStatus.setBackgroundResource(chipStyle.backgroundRes);
        holder.textStatus.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), chipStyle.textColorRes));

        holder.buttonDetails.setVisibility(actionListener == null ? View.GONE : View.VISIBLE);
        holder.buttonDetails.setOnClickListener(v -> {
            if (actionListener != null) {
                actionListener.onViewDetails(record);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public int indexOfRequestId(@NonNull String requestId) {
        String normalizedId = requestId.trim();
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getId().equals(normalizedId)) {
                return i;
            }
        }
        return RecyclerView.NO_POSITION;
    }

    @NonNull
    private String buildMeta(@NonNull VehicleRequestRecord record) {
        String type = record.getStickerType().trim().isEmpty() ? "Sticker N/A" : "Sticker " + record.getStickerType();
        String time = formatTime(record.getCreatedAt());
        if (record.isRemovalRequest()) {
            return type + "  •  " + time + "  •  " + record.getRemovalReason();
        }
        return type + "  •  " + time + "  •  " + record.getVehicleDescription();
    }

    @NonNull
    private String formatTime(@Nullable Timestamp timestamp) {
        if (timestamp == null) {
            return "Created: Unknown";
        }
        DateFormat format = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);
        Date date = timestamp.toDate();
        return "Created: " + format.format(date);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView textPlate;
        final TextView textKind;
        final TextView textStatus;
        final TextView textMeta;
        final com.google.android.material.button.MaterialButton buttonDetails;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            textPlate = itemView.findViewById(R.id.text_vehicle_app_plate);
            textKind = itemView.findViewById(R.id.text_vehicle_app_kind);
            textStatus = itemView.findViewById(R.id.text_vehicle_app_status);
            textMeta = itemView.findViewById(R.id.text_vehicle_app_meta);
            buttonDetails = itemView.findViewById(R.id.button_vehicle_app_details);
        }
    }
}
