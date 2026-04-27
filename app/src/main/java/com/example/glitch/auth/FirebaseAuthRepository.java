package com.example.glitch.auth;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.glitch.model.UserProfile;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Firebase implementation of AuthRepository with Firestore user-profile gate checks.
 * Pattern: Adapter coordinating FirebaseAuth and Firestore profile validation.
 * Known issue: signup endpoints are intentionally disabled by not exposing registration APIs.
 */
public class FirebaseAuthRepository implements AuthRepository {
    public static final String USERS_COLLECTION = "users";
    private static final String TAG = "FirebaseAuthRepository";
    private static final String CONTACT_ADMIN_IST = "Contact admin (IST)";

    private final FirebaseAuth auth;
    private final FirebaseFirestore firestore;

    /**
     * Creates repository using singleton Firebase instances.
     */
    public FirebaseAuthRepository() {
        this(FirebaseAuth.getInstance(), FirebaseFirestore.getInstance());
    }

    FirebaseAuthRepository(@NonNull FirebaseAuth auth, @NonNull FirebaseFirestore firestore) {
        this.auth = auth;
        this.firestore = firestore;
    }

    /**
     * Attempts email/password sign-in then validates Firestore profile authorization.
     */
    @Override
    public void login(@NonNull String email, @NonNull String password, @NonNull AuthCallback callback) {
        auth.signInWithEmailAndPassword(email.trim(), password)
                .addOnSuccessListener(result -> validateCurrentSession(callback))
                .addOnFailureListener(error -> {
                    Log.e(TAG, "Firebase sign-in failed for " + email, error);
                    callback.onResult(false, null, normalizeAuthError(error));
                });
    }

    /**
     * Signs out FirebaseAuth and clears local session cache.
     */
    @Override
    public void logout() {
        auth.signOut();
        SessionManager.clear();
    }

    /**
     * Validates that current Firebase user has an active supported profile in users collection.
     */
    @Override
    public void validateCurrentSession(@NonNull AuthCallback callback) {
        FirebaseUser firebaseUser = auth.getCurrentUser();
        if (firebaseUser == null) {
            SessionManager.clear();
            callback.onResult(false, null, "Please login to continue.");
            return;
        }
        firestore.collection(USERS_COLLECTION)
                .document(firebaseUser.getUid())
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.exists() || snapshot.getData() == null) {
                        Log.w(TAG, "Profile missing for uid=" + firebaseUser.getUid());
                        logout();
                        callback.onResult(false, null, CONTACT_ADMIN_IST);
                        return;
                    }

                    UserProfile profile = UserProfile.fromMap(firebaseUser.getUid(), snapshot.getData());
                    if (!profile.isActive() || !profile.hasSupportedRole()) {
                        Log.w(TAG, "Profile gate rejected. uid=" + firebaseUser.getUid()
                                + ", role=" + profile.getRole() + ", isActive=" + profile.isActive());
                        logout();
                        callback.onResult(false, null, CONTACT_ADMIN_IST);
                        return;
                    }
                    SessionManager.setCurrentProfile(profile);
                    callback.onResult(true, profile, "Login successful");
                })
                .addOnFailureListener(error -> {
                    Log.e(TAG, "Profile validation read failed for uid=" + firebaseUser.getUid(), error);
                    callback.onResult(false, null, "Unable to validate user profile.");
                });
    }

    /**
     * Returns current cached session profile.
     */
    @Nullable
    @Override
    public UserProfile getCurrentProfile() {
        return SessionManager.getCurrentProfile();
    }

    @NonNull
    private String normalizeAuthError(@Nullable Exception error) {
        String rawMessage = error == null ? "" : String.valueOf(error.getMessage());
        String normalizedMessage = rawMessage.toLowerCase();
        if (error instanceof FirebaseNetworkException
                || normalizedMessage.contains("network error")
                || normalizedMessage.contains("timeout")
                || normalizedMessage.contains("unable to resolve host")) {
            return "Network timeout. Check emulator/device internet, then retry.";
        }
        if (error instanceof FirebaseAuthInvalidUserException
                || error instanceof FirebaseAuthInvalidCredentialsException) {
            return "Invalid email or password.";
        }
        if (error instanceof FirebaseAuthException) {
            String message = error.getMessage();
            if (message != null && !message.trim().isEmpty()) {
                return message;
            }
            return "Authentication failed. Please try again.";
        }
        if (error == null || error.getMessage() == null || error.getMessage().trim().isEmpty()) {
            return "Login failed. Please verify credentials.";
        }
        return error.getMessage();
    }
}
