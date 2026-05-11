package com.example.glitch.ui;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.glitch.model.GuardPendingDecision;

/**
 * SharedPreferences-backed store for unresolved guard decisions.
 * One decision is stored per guard UID.
 */
public class GuardPendingDecisionStore {
    private static final String PREFS = "guard_pending_decision_store";
    private static final String KEY_PREFIX = "decision_";

    private final SharedPreferences prefs;

    public GuardPendingDecisionStore(@NonNull Context context) {
        this(context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE));
    }

    GuardPendingDecisionStore(@NonNull SharedPreferences prefs) {
        this.prefs = prefs;
    }

    public void save(@NonNull GuardPendingDecision decision) {
        String guardUid = decision.getGuardUid().trim();
        if (guardUid.isEmpty()) {
            return;
        }
        String json = decision.toJson();
        if (json == null || json.trim().isEmpty()) {
            return;
        }
        // Persist synchronously because navigation to the decision screen happens immediately
        // after save; async apply() can race and make the next fragment read "missing" data.
        prefs.edit().putString(keyForGuard(guardUid), json).commit();
    }

    @Nullable
    public GuardPendingDecision getForGuard(@NonNull String guardUid) {
        String key = keyForGuard(guardUid.trim());
        String json = prefs.getString(key, null);
        GuardPendingDecision decision = GuardPendingDecision.fromJson(json);
        if (decision == null || !decision.isValid()) {
            clearForGuard(guardUid);
            return null;
        }
        if (!guardUid.trim().equals(decision.getGuardUid().trim())) {
            clearForGuard(guardUid);
            return null;
        }
        return decision;
    }

    public boolean hasForGuard(@NonNull String guardUid) {
        return getForGuard(guardUid) != null;
    }

    public void clearForGuard(@NonNull String guardUid) {
        String trimmed = guardUid.trim();
        if (trimmed.isEmpty()) {
            return;
        }
        prefs.edit().remove(keyForGuard(trimmed)).apply();
    }

    @NonNull
    private String keyForGuard(@NonNull String guardUid) {
        return KEY_PREFIX + guardUid;
    }
}
