package com.example.glitch.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.glitch.R;
import com.example.glitch.model.UserProfile;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView adapter for admin user management list.
 * Pattern: Adapter with row-level active-state toggle callbacks.
 * Known issue: inline role edits are handled via separate form, not per-row controls.
 */
public class UserManagementAdapter extends RecyclerView.Adapter<UserManagementAdapter.UserViewHolder> {
    public interface UserActionListener {
        void onToggleActive(@NonNull UserProfile user, boolean isActive);
    }

    private final UserActionListener listener;
    private final List<UserProfile> users = new ArrayList<>();

    public UserManagementAdapter(@NonNull UserActionListener listener) {
        this.listener = listener;
    }

    public void submitList(@NonNull List<UserProfile> updated) {
        users.clear();
        users.addAll(updated);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user_profile, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        UserProfile user = users.get(position);
        holder.textDisplayName.setText(user.getDisplayName());
        holder.textEmailRole.setText(user.getEmail() + " • " + user.getRole());
        holder.switchActive.setOnCheckedChangeListener(null);
        holder.switchActive.setChecked(user.isActive());
        holder.switchActive.setOnCheckedChangeListener((buttonView, isChecked) -> listener.onToggleActive(user, isChecked));
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        final TextView textDisplayName;
        final TextView textEmailRole;
        final MaterialSwitch switchActive;

        UserViewHolder(@NonNull View itemView) {
            super(itemView);
            textDisplayName = itemView.findViewById(R.id.text_user_display_name);
            textEmailRole = itemView.findViewById(R.id.text_user_email_role);
            switchActive = itemView.findViewById(R.id.switch_user_active);
        }
    }
}
