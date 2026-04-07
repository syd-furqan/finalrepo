package com.example.glitch.data;

import androidx.annotation.NonNull;

import com.example.glitch.model.UserProfile;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Firestore implementation of user-management repository.
 * Pattern: Adapter over users collection with realtime listing.
 * Known issue: user creation requires pre-existing FirebaseAuth uid to be truly login-capable.
 */
public class FirestoreUserManagementRepository implements UserManagementRepository {
    private final FirebaseFirestore firestore;
    private ListenerRegistration registration;

    public FirestoreUserManagementRepository() {
        this(FirebaseFirestore.getInstance());
    }

    FirestoreUserManagementRepository(@NonNull FirebaseFirestore firestore) {
        this.firestore = firestore;
    }

    @Override
    public void listenUsers(@NonNull UserListListener listener) {
        removeListeners();
        registration = firestore.collection("users")
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        listener.onError(error);
                        return;
                    }
                    List<UserProfile> users = new ArrayList<>();
                    if (snapshot != null) {
                        for (DocumentSnapshot document : snapshot.getDocuments()) {
                            users.add(UserProfile.fromMap(document.getId(), document.getData()));
                        }
                    }
                    listener.onData(users);
                });
    }

    @Override
    public void setUserActive(@NonNull String uid, boolean isActive, @NonNull OperationCallback callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("isActive", isActive);
        updates.put("updatedAt", FieldValue.serverTimestamp());
        firestore.collection("users").document(uid)
                .update(updates)
                .addOnSuccessListener(unused -> callback.onComplete(true, "User updated", null))
                .addOnFailureListener(error -> callback.onComplete(false, "Failed to update user", error));
    }

    @Override
    public void upsertUser(
            @NonNull String uid,
            @NonNull String email,
            @NonNull String role,
            @NonNull String displayName,
            boolean isActive,
            @NonNull OperationCallback callback
    ) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("email", email.trim().toLowerCase(Locale.getDefault()));
        payload.put("role", role.trim().toLowerCase(Locale.getDefault()));
        payload.put("displayName", displayName.trim());
        payload.put("isActive", isActive);
        payload.put("updatedAt", FieldValue.serverTimestamp());
        payload.put("createdAt", FieldValue.serverTimestamp());
        firestore.collection("users").document(uid)
                .set(payload)
                .addOnSuccessListener(unused -> callback.onComplete(true, "User saved", null))
                .addOnFailureListener(error -> callback.onComplete(false, "Failed to save user", error));
    }

    @Override
    public void removeListeners() {
        if (registration != null) {
            registration.remove();
            registration = null;
        }
    }
}
