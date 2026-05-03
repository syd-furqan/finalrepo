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
import com.google.android.material.button.MaterialButton;
import com.google.firebase.Timestamp;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * RecyclerView adapter for admin vehicle approval rows.
 * Pattern: Request summary with approve/deny actions for pending records.
 */
public class AdminVehicleReviewAdapter extends RecyclerView.Adapter<AdminVehicleReviewAdapter.VehicleReviewViewHolder> {
    private final List<VehicleRequestRecord> items = new ArrayList<>();
    private final VehicleReviewActionListener listener;

    AdminVehicleReviewAdapter(@NonNull VehicleReviewActionListener listener) {
        this.listener = listener;
    }

    void submitList(@NonNull List<VehicleRequestRecord> records) {
        items.clear();
        items.addAll(records);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VehicleReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_vehicle_review, parent, false);
        return new VehicleReviewViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VehicleReviewViewHolder holder, int position) {
        VehicleRequestRecord record = items.get(position);
        holder.textPlate.setText(record.getPlateNumber());
        holder.textDetails.setText(formatDetails(record));
        holder.textRequester.setText(holder.itemView.getContext().getString(
                R.string.admin_vehicle_requester_label,
                record.getRequesterUid().isEmpty() ? "Unknown" : record.getRequesterUid()
        ));
        holder.textSubmitted.setText(formatCreatedAt(holder, record.getCreatedAt()));

        String status = record.getStatus().isEmpty() ? "pending" : record.getStatus();
        holder.textStatus.setText(status.toUpperCase(Locale.getDefault()));
        VehicleRequestAdapter.ChipStyle style = VehicleRequestAdapter.resolveStatusStyle(status);
        holder.textStatus.setBackgroundResource(style.backgroundRes);
        holder.textStatus.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), style.textColorRes));

        boolean pending = record.isPending();
        holder.buttonApprove.setEnabled(pending);
        holder.buttonDeny.setEnabled(pending);
        holder.buttonApprove.setVisibility(pending ? View.VISIBLE : View.GONE);
        holder.buttonDeny.setVisibility(pending ? View.VISIBLE : View.GONE);
        holder.textReviewed.setVisibility(pending ? View.GONE : View.VISIBLE);
        holder.textReviewed.setText(formatReviewed(record));
        holder.buttonApprove.setOnClickListener(v -> listener.onApprove(record));
        holder.buttonDeny.setOnClickListener(v -> listener.onDeny(record));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @NonNull
    private String formatDetails(@NonNull VehicleRequestRecord record) {
        String description = record.getVehicleDescription();
        String color = record.getVehicleColor();
        if (description.isEmpty()) {
            return color;
        }
        if (color.isEmpty()) {
            return description;
        }
        return description + " | " + color;
    }

    @NonNull
    private String formatCreatedAt(@NonNull VehicleReviewViewHolder holder, @Nullable Timestamp createdAt) {
        if (createdAt == null) {
            return holder.itemView.getContext().getString(R.string.vehicle_created_at_unknown);
        }
        DateFormat formatter = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);
        Date date = createdAt.toDate();
        return holder.itemView.getContext().getString(R.string.vehicle_created_at_label, formatter.format(date));
    }

    @NonNull
    private String formatReviewed(@NonNull VehicleRequestRecord record) {
        if (record.getReviewNote().trim().isEmpty()) {
            return record.getReviewedAt() == null ? "" : "Reviewed";
        }
        return record.getReviewNote();
    }

    static class VehicleReviewViewHolder extends RecyclerView.ViewHolder {
        final TextView textPlate;
        final TextView textStatus;
        final TextView textDetails;
        final TextView textRequester;
        final TextView textSubmitted;
        final TextView textReviewed;
        final MaterialButton buttonApprove;
        final MaterialButton buttonDeny;

        VehicleReviewViewHolder(@NonNull View itemView) {
            super(itemView);
            textPlate = itemView.findViewById(R.id.text_admin_vehicle_plate);
            textStatus = itemView.findViewById(R.id.text_admin_vehicle_status);
            textDetails = itemView.findViewById(R.id.text_admin_vehicle_details);
            textRequester = itemView.findViewById(R.id.text_admin_vehicle_requester);
            textSubmitted = itemView.findViewById(R.id.text_admin_vehicle_submitted);
            textReviewed = itemView.findViewById(R.id.text_admin_vehicle_reviewed);
            buttonApprove = itemView.findViewById(R.id.button_approve_vehicle);
            buttonDeny = itemView.findViewById(R.id.button_deny_vehicle);
        }
    }

    interface VehicleReviewActionListener {
        void onApprove(@NonNull VehicleRequestRecord record);

        void onDeny(@NonNull VehicleRequestRecord record);
    }
}
