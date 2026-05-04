package com.example.glitch.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.glitch.R;
import com.example.glitch.model.RegisteredVehicleRecord;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Shows sponsor's registered vehicles and triggers removal request flow.
 */
public class RegisteredVehicleAdapter extends RecyclerView.Adapter<RegisteredVehicleAdapter.ViewHolder> {
    public interface ActionListener {
        void onRequestRemoval(@NonNull RegisteredVehicleRecord vehicle);
    }

    private final List<RegisteredVehicleRecord> items = new ArrayList<>();
    private final ActionListener listener;

    public RegisteredVehicleAdapter(@NonNull ActionListener listener) {
        this.listener = listener;
    }

    public void submitList(@NonNull List<RegisteredVehicleRecord> records) {
        items.clear();
        items.addAll(records);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_registered_vehicle, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RegisteredVehicleRecord record = items.get(position);
        holder.textPlate.setText(record.getPlateNumber());
        String subtitle = (record.getMake() + " " + record.getModel() + " " + record.getVariant()).trim();
        holder.textModel.setText(subtitle.isEmpty() ? holder.itemView.getContext().getString(R.string.vehicle_model) : subtitle);
        holder.textSticker.setText(record.getStickerType().isEmpty() ? "N/A" : record.getStickerType().toUpperCase(Locale.getDefault()));
        boolean active = "active".equalsIgnoreCase(record.getStatus());
        holder.buttonRemove.setVisibility(active ? View.VISIBLE : View.GONE);
        holder.buttonRemove.setOnClickListener(v -> listener.onRequestRemoval(record));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView textPlate;
        final TextView textModel;
        final TextView textSticker;
        final MaterialButton buttonRemove;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            textPlate = itemView.findViewById(R.id.text_registered_vehicle_plate);
            textModel = itemView.findViewById(R.id.text_registered_vehicle_model);
            textSticker = itemView.findViewById(R.id.text_registered_vehicle_sticker);
            buttonRemove = itemView.findViewById(R.id.button_request_vehicle_removal);
        }
    }
}
