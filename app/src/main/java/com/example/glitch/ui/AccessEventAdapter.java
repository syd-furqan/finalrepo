package com.example.glitch.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.example.glitch.R;
import com.example.glitch.model.AccessEvent;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * RecyclerView adapter for admin audit event timelines.
 */
public class AccessEventAdapter extends RecyclerView.Adapter<AccessEventAdapter.EventViewHolder> {
    interface ActionListener {
        void onViewDetails(@NonNull AccessEvent event);
    }

    private final List<AccessEvent> items = new ArrayList<>();
    private final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
    @Nullable
    private ActionListener actionListener;

    void setActionListener(@Nullable ActionListener actionListener) {
        this.actionListener = actionListener;
    }

    void submitFirstPage(@NonNull List<AccessEvent> events) {
        items.clear();
        items.addAll(events);
        notifyDataSetChanged();
    }

    void appendPage(@NonNull List<AccessEvent> events) {
        if (events.isEmpty()) {
            return;
        }
        Set<String> existingIds = new HashSet<>();
        for (AccessEvent event : items) {
            existingIds.add(event.getId());
        }
        int start = items.size();
        int added = 0;
        for (AccessEvent event : events) {
            if (existingIds.contains(event.getId())) {
                continue;
            }
            items.add(event);
            existingIds.add(event.getId());
            added++;
        }
        if (added > 0) {
            notifyItemRangeInserted(start, added);
        }
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_access_event, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        AccessEvent event = items.get(position);
        holder.textEventType.setText(event.getEventType().trim().isEmpty() ? "EVENT" : event.getEventType());
        holder.textEventOutcome.setText(event.getOutcome().trim().isEmpty() ? "N/A" : event.getOutcome());
        holder.textDescription.setText(event.getDescription().trim().isEmpty() ? "No description" : event.getDescription());

        String actor = "Actor: " + valueOrFallback(event.getActorUid()) + " (" + valueOrFallback(event.getActorRole()) + ")";
        holder.textActor.setText(actor);

        String correlation = event.getCorrelationId().trim().isEmpty() ? valueOrFallback(event.getRequestId()) : event.getCorrelationId();
        holder.textRequestId.setText(holder.itemView.getContext().getString(R.string.request_id_label) + ": " + correlation);

        if (event.getCreatedAt() != null) {
            holder.textTime.setText(format.format(event.getCreatedAt().toDate()));
        } else {
            holder.textTime.setText("--");
        }

        holder.itemView.setOnClickListener(v -> {
            if (actionListener != null) {
                actionListener.onViewDetails(event);
            }
        });
    }

    @NonNull
    private String valueOrFallback(@NonNull String value) {
        String trimmed = value.trim();
        return trimmed.isEmpty() ? "N/A" : trimmed;
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class EventViewHolder extends RecyclerView.ViewHolder {
        final TextView textEventType;
        final TextView textEventOutcome;
        final TextView textDescription;
        final TextView textActor;
        final TextView textRequestId;
        final TextView textTime;

        EventViewHolder(@NonNull View itemView) {
            super(itemView);
            textEventType = itemView.findViewById(R.id.text_event_type);
            textEventOutcome = itemView.findViewById(R.id.text_event_outcome);
            textDescription = itemView.findViewById(R.id.text_event_description);
            textActor = itemView.findViewById(R.id.text_event_actor);
            textRequestId = itemView.findViewById(R.id.text_event_request_id);
            textTime = itemView.findViewById(R.id.text_event_time);
        }
    }
}
