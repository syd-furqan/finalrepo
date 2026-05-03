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
 * RecyclerView adapter for the admin vehicle request review screen.
 * Pending rows show approve/deny buttons; reviewed rows show the review note.
 */
public class AdminVehicleReviewAdapter extends RecyclerView.Adapter<AdminVehicleReviewAdapter.ViewHolder> {

    public interface ActionListener {
        void onApprove(@NonNull VehicleRequestRecord record);
        void onDeny(@NonNull VehicleRequestRecord record);
    }

    private final List<VehicleRequestRecord> items = new ArrayList<>();
    private final ActionListener actionListener;

    public AdminVehicleReviewAdapter(@NonNull ActionListener actionListener) {
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
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_vehicle_review, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        VehicleRequestRecord record = items.get(position);
        holder.textPlate.setText(record.getPlateNumber());

        String description = record.getVehicleDescription();
        String color = record.getVehicleColor().trim();
        holder.textDetails.setText(color.isEmpty() ? description : description + "  |  " + color);

        holder.textRequester.setText(record.getRequesterUid());
        holder.textSubmitted.setText(formatTimestamp(holder, record.getCreatedAt(), "Submitted: "));

        String status = record.getStatus();
        VehicleRequestAdapter.ChipStyle chipStyle = VehicleRequestAdapter.resolveStatusStyle(status);
        holder.textStatus.setText(status.toUpperCase(Locale.getDefault()));
        holder.textStatus.setBackgroundResource(chipStyle.backgroundRes);
        holder.textStatus.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), chipStyle.textColorRes));

        boolean pending = record.isPending();
        holder.buttonApprove.setVisibility(pending ? View.VISIBLE : View.GONE);
        holder.buttonDeny.setVisibility(pending ? View.VISIBLE : View.GONE);

        if (pending) {
            holder.textReviewed.setVisibility(View.GONE);
            holder.buttonApprove.setOnClickListener(v -> actionListener.onApprove(record));
            holder.buttonDeny.setOnClickListener(v -> actionListener.onDeny(record));
        } else {
            holder.textReviewed.setVisibility(View.VISIBLE);
            String note = record.getReviewNote().trim();
            String reviewedAt = formatTimestamp(holder, record.getReviewedAt(), "Reviewed: ");
            holder.textReviewed.setText(note.isEmpty() ? reviewedAt : reviewedAt + "  ·  " + note);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @NonNull
    private String formatTimestamp(@NonNull ViewHolder holder, @Nullable Timestamp timestamp, @NonNull String prefix) {
        if (timestamp == null) {
            return prefix + "Unknown";
        }
        DateFormat fmt = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);
        Date date = timestamp.toDate();
        return prefix + fmt.format(date);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView textPlate;
        final TextView textStatus;
        final TextView textDetails;
        final TextView textRequester;
        final TextView textSubmitted;
        final TextView textReviewed;
        final MaterialButton buttonApprove;
        final MaterialButton buttonDeny;

        ViewHolder(@NonNull View itemView) {
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
}
