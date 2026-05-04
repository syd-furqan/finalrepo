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
 * RecyclerView adapter for vehicle request status history.
 * Pattern: Simple list adapter with request summary rows.
 * Known issue: action buttons per row are not required in v1.
 */
public class VehicleRequestAdapter extends RecyclerView.Adapter<VehicleRequestAdapter.VehicleRequestViewHolder> {
    private final List<VehicleRequestRecord> items = new ArrayList<>();
    private final VehicleRequestActionListener actionListener;

    public VehicleRequestAdapter() {
        this((record) -> {
        });
    }

    public VehicleRequestAdapter(@NonNull VehicleRequestActionListener actionListener) {
        this.actionListener = actionListener;
    }

    public void submitList(@NonNull List<VehicleRequestRecord> records) {
        items.clear();
        items.addAll(records);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VehicleRequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_vehicle_request, parent, false);
        return new VehicleRequestViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VehicleRequestViewHolder holder, int position) {
        VehicleRequestRecord record = items.get(position);
        holder.textPlate.setText(record.getPlateNumber());
        holder.textModel.setText(record.getVehicleModel());
        holder.textCreatedAt.setText(formatCreatedAt(holder, record.getCreatedAt()));
        String status = record.getStatus() == null ? "" : record.getStatus();
        holder.textStatus.setText(status.toUpperCase(Locale.getDefault()));
        ChipStyle style = resolveStatusStyle(status);
        holder.textStatus.setBackgroundResource(style.backgroundRes);
        holder.textStatus.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), style.textColorRes));
        if ("pending".equalsIgnoreCase(status)) {
            holder.textHint.setText(R.string.vehicle_edit_pending_hint);
        } else {
            holder.textHint.setText(R.string.vehicle_edit_locked_hint);
        }
        holder.itemView.setOnClickListener(v -> actionListener.onVehicleRequestSelected(record));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VehicleRequestViewHolder extends RecyclerView.ViewHolder {
        final TextView textPlate;
        final TextView textModel;
        final TextView textStatus;
        final TextView textCreatedAt;
        final TextView textHint;

        VehicleRequestViewHolder(@NonNull View itemView) {
            super(itemView);
            textPlate = itemView.findViewById(R.id.text_plate);
            textModel = itemView.findViewById(R.id.text_vehicle_model);
            textStatus = itemView.findViewById(R.id.text_vehicle_status);
            textCreatedAt = itemView.findViewById(R.id.text_vehicle_created_at);
            textHint = itemView.findViewById(R.id.text_vehicle_hint);
        }
    }

    @NonNull
    private String formatCreatedAt(@NonNull VehicleRequestViewHolder holder, @Nullable Timestamp createdAt) {
        if (createdAt == null) {
            return holder.itemView.getContext().getString(R.string.vehicle_created_at_unknown);
        }
        DateFormat formatter = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);
        Date date = createdAt.toDate();
        return holder.itemView.getContext().getString(
                R.string.vehicle_created_at_label,
                formatter.format(date)
        );
    }

    @NonNull
    static ChipStyle resolveStatusStyle(@NonNull String rawStatus) {
        String status = rawStatus.trim().toLowerCase(Locale.getDefault());
        if ("approved".equals(status)) {
            return new ChipStyle(R.drawable.bg_chip_success, R.color.success_green);
        }
        if ("rejected".equals(status) || "denied".equals(status)) {
            return new ChipStyle(R.drawable.bg_chip_alert_critical, R.color.danger_red);
        }
        if ("cancelled".equals(status)) {
            return new ChipStyle(R.drawable.bg_chip_alert_critical, R.color.danger_red);
        }
        if ("received".equals(status)) {
            return new ChipStyle(R.drawable.bg_chip_role, R.color.primary_navy);
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

    public interface VehicleRequestActionListener {
        void onVehicleRequestSelected(@NonNull VehicleRequestRecord record);
    }
}
