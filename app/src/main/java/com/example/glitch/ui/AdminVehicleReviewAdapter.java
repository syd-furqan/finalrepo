package com.example.glitch.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
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
 */
public class AdminVehicleReviewAdapter extends RecyclerView.Adapter<AdminVehicleReviewAdapter.ViewHolder> {

    public interface ActionListener {
        void onViewDetails(@NonNull VehicleRequestRecord record);

        void onMarkReceived(@NonNull VehicleRequestRecord record);

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

    public int indexOfRequestId(@NonNull String requestId) {
        String target = requestId.trim();
        if (target.isEmpty()) {
            return RecyclerView.NO_POSITION;
        }
        for (int i = 0; i < items.size(); i++) {
            if (target.equals(items.get(i).getId())) {
                return i;
            }
        }
        return RecyclerView.NO_POSITION;
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
        String details = (record.isRemovalRequest() ? "REMOVE • " : "REGISTER • ")
                + record.getVehicleDescription();
        holder.textDetails.setText(details);
        holder.textRequester.setText(record.getRequesterUid() + " • " + record.getRequesterRole());
        holder.textSubmitted.setText(formatTimestamp(record.getCreatedAt(), "Submitted: "));

        String status = record.getStatus();
        VehicleRequestAdapter.ChipStyle chipStyle = VehicleRequestAdapter.resolveStatusStyle(status);
        holder.textStatus.setText(status.toUpperCase(Locale.getDefault()));
        holder.textStatus.setBackgroundResource(chipStyle.backgroundRes);
        holder.textStatus.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), chipStyle.textColorRes));

        holder.buttonDetails.setOnClickListener(v -> actionListener.onViewDetails(record));

        if (record.isSubmitted()) {
            holder.buttonMarkReceived.setVisibility(View.VISIBLE);
            holder.buttonApprove.setVisibility(View.GONE);
            holder.buttonDeny.setVisibility(View.VISIBLE);
            holder.buttonMarkReceived.setOnClickListener(v -> actionListener.onMarkReceived(record));
            holder.buttonDeny.setOnClickListener(v -> actionListener.onDeny(record));
            holder.textReviewed.setVisibility(View.GONE);
            return;
        }

        if (record.isReceived()) {
            holder.buttonMarkReceived.setVisibility(View.GONE);
            holder.buttonApprove.setVisibility(View.VISIBLE);
            holder.buttonDeny.setVisibility(View.VISIBLE);
            holder.buttonApprove.setOnClickListener(v -> actionListener.onApprove(record));
            holder.buttonDeny.setOnClickListener(v -> actionListener.onDeny(record));
            holder.textReviewed.setVisibility(View.GONE);
            return;
        }

        holder.buttonMarkReceived.setVisibility(View.GONE);
        holder.buttonApprove.setVisibility(View.GONE);
        holder.buttonDeny.setVisibility(View.GONE);
        holder.textReviewed.setVisibility(View.VISIBLE);
        holder.textReviewed.setText(formatTimestamp(record.getReviewedAt(), "Reviewed: ")
                + "  ·  " + (record.getReviewNote().trim().isEmpty() ? "No note" : record.getReviewNote().trim()));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @NonNull
    private String formatTimestamp(Timestamp timestamp, @NonNull String prefix) {
        if (timestamp == null) {
            return prefix + "Unknown";
        }
        DateFormat format = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);
        Date date = timestamp.toDate();
        return prefix + format.format(date);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView textPlate;
        final TextView textStatus;
        final TextView textDetails;
        final TextView textRequester;
        final TextView textSubmitted;
        final TextView textReviewed;
        final MaterialButton buttonDetails;
        final MaterialButton buttonMarkReceived;
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
            buttonDetails = itemView.findViewById(R.id.button_view_vehicle_details);
            buttonMarkReceived = itemView.findViewById(R.id.button_mark_vehicle_received);
            buttonApprove = itemView.findViewById(R.id.button_approve_vehicle);
            buttonDeny = itemView.findViewById(R.id.button_deny_vehicle);
        }
    }
}
