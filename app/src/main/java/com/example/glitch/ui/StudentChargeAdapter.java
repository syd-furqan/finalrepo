package com.example.glitch.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.example.glitch.R;
import com.example.glitch.model.FineCaseRecord;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class StudentChargeAdapter extends RecyclerView.Adapter<StudentChargeAdapter.ViewHolder> {
    private final List<FineCaseRecord> charges = new ArrayList<>();
    private OnRemovalRequestListener listener;

    public void setListener(OnRemovalRequestListener listener) {
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
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_student_charge, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FineCaseRecord charge = charges.get(position);
        holder.textLevel.setText(valueOr(charge.getReasonCode(), "N/A").toUpperCase(Locale.getDefault()));
        holder.textStatus.setText(capitalize(charge.getChargeDisplayStatus()));

        String guestInfo = valueOr(charge.getGuestName(), "Unknown")
                + " (" + valueOr(charge.getGuestIdNumber(), "N/A") + ")";
        holder.textGuestInfo.setText(guestInfo);

        if (charge.getCreatedAt() != null) {
            String date = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                    .format(charge.getCreatedAt().toDate());
            holder.textDate.setText("Charged on: " + date);
        } else {
            holder.textDate.setText("");
        }

        boolean isIssued = "issued".equalsIgnoreCase(charge.getStatus());
        boolean isRemovalRequested = charge.isRemovalRequested();

        holder.buttonRequestRemoval.setVisibility(isIssued ? View.VISIBLE : View.GONE);
        holder.layoutPaymentNote.setVisibility(View.GONE);
        holder.buttonConfirmRemoval.setVisibility(View.GONE);

        holder.buttonRequestRemoval.setOnClickListener(v -> {
            holder.layoutPaymentNote.setVisibility(View.VISIBLE);
            holder.buttonConfirmRemoval.setVisibility(View.VISIBLE);
            holder.buttonRequestRemoval.setVisibility(View.GONE);
        });

        holder.buttonConfirmRemoval.setOnClickListener(v -> {
            CharSequence noteText = holder.inputPaymentNote.getText();
            String note = noteText == null ? "" : noteText.toString().trim();
            if (note.isEmpty()) {
                holder.layoutPaymentNote.setError("Enter a payment note.");
                return;
            }
            holder.layoutPaymentNote.setError(null);
            if (listener != null) listener.onRequestRemoval(charge, note);
        });
    }

    @Override
    public int getItemCount() {
        return charges.size();
    }

    @NonNull
    private String valueOr(@Nullable String value, @NonNull String fallback) {
        return (value == null || value.trim().isEmpty()) ? fallback : value.trim();
    }

    @NonNull
    private String capitalize(@NonNull String text) {
        if (text.isEmpty()) return text;
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView textLevel, textStatus, textGuestInfo, textDate;
        final TextInputLayout layoutPaymentNote;
        final TextInputEditText inputPaymentNote;
        final MaterialButton buttonRequestRemoval, buttonConfirmRemoval;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            textLevel = itemView.findViewById(R.id.text_charge_violation_level);
            textStatus = itemView.findViewById(R.id.text_charge_status_label);
            textGuestInfo = itemView.findViewById(R.id.text_charge_guest_info);
            textDate = itemView.findViewById(R.id.text_charge_date);
            layoutPaymentNote = itemView.findViewById(R.id.layout_payment_note);
            inputPaymentNote = itemView.findViewById(R.id.input_payment_note);
            buttonRequestRemoval = itemView.findViewById(R.id.button_request_removal);
            buttonConfirmRemoval = itemView.findViewById(R.id.button_confirm_removal_request);
        }
    }

    public interface OnRemovalRequestListener {
        void onRequestRemoval(@NonNull FineCaseRecord charge, @NonNull String paymentNote);
    }
}
