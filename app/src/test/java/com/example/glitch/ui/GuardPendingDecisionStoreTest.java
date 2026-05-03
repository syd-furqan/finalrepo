package com.example.glitch.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.SharedPreferences;

import androidx.annotation.Nullable;

import com.example.glitch.model.GuardPendingDecision;

import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class GuardPendingDecisionStoreTest {

    @Test
    public void saveLoadClear_byGuardUid() {
        InMemorySharedPreferences prefs = new InMemorySharedPreferences();
        GuardPendingDecisionStore store = new GuardPendingDecisionStore(prefs);
        GuardPendingDecision decision = sample("guard-1", "PASS1234", "req-1");

        store.save(decision);

        GuardPendingDecision loaded = store.getForGuard("guard-1");
        assertNotNull(loaded);
        assertEquals("PASS1234", loaded.getPassCode());
        assertTrue(store.hasForGuard("guard-1"));

        store.clearForGuard("guard-1");
        assertNull(store.getForGuard("guard-1"));
        assertFalse(store.hasForGuard("guard-1"));
    }

    @Test
    public void decisionSurvives_restartUsingSamePreferences() {
        InMemorySharedPreferences prefs = new InMemorySharedPreferences();
        GuardPendingDecisionStore firstStore = new GuardPendingDecisionStore(prefs);
        firstStore.save(sample("guard-9", "ABCD1234", "req-9"));

        GuardPendingDecisionStore secondStore = new GuardPendingDecisionStore(prefs);
        GuardPendingDecision loaded = secondStore.getForGuard("guard-9");

        assertNotNull(loaded);
        assertEquals("ABCD1234", loaded.getPassCode());
        assertEquals("req-9", loaded.getEntryRequestId());
    }

    @Test
    public void decisionIsolation_differentGuardDoesNotReadOthersDecision() {
        InMemorySharedPreferences prefs = new InMemorySharedPreferences();
        GuardPendingDecisionStore store = new GuardPendingDecisionStore(prefs);
        store.save(sample("guard-a", "CODEAAAA", "req-a"));

        assertNull(store.getForGuard("guard-b"));
        assertFalse(store.hasForGuard("guard-b"));
        assertNotNull(store.getForGuard("guard-a"));
    }

    private GuardPendingDecision sample(String guardUid, String passCode, String requestId) {
        return new GuardPendingDecision(
                guardUid,
                passCode,
                "QR_SCAN",
                requestId,
                "Ali",
                "123",
                false,
                "",
                "non_vehicle",
                "Sponsor",
                "student",
                "sponsor@example.com",
                "in-gate",
                100L,
                200L
        );
    }

    /**
     * Small in-memory SharedPreferences fake for local unit tests.
     */
    private static final class InMemorySharedPreferences implements SharedPreferences {
        private final Map<String, Object> values = new HashMap<>();

        @Override
        public Map<String, ?> getAll() {
            return Collections.unmodifiableMap(values);
        }

        @Nullable
        @Override
        public String getString(String key, @Nullable String defValue) {
            Object value = values.get(key);
            return value instanceof String ? (String) value : defValue;
        }

        @SuppressWarnings("unchecked")
        @Override
        public Set<String> getStringSet(String key, @Nullable Set<String> defValues) {
            Object value = values.get(key);
            if (value instanceof Set) {
                return new HashSet<>((Set<String>) value);
            }
            return defValues;
        }

        @Override
        public int getInt(String key, int defValue) {
            Object value = values.get(key);
            return value instanceof Integer ? (Integer) value : defValue;
        }

        @Override
        public long getLong(String key, long defValue) {
            Object value = values.get(key);
            return value instanceof Long ? (Long) value : defValue;
        }

        @Override
        public float getFloat(String key, float defValue) {
            Object value = values.get(key);
            return value instanceof Float ? (Float) value : defValue;
        }

        @Override
        public boolean getBoolean(String key, boolean defValue) {
            Object value = values.get(key);
            return value instanceof Boolean ? (Boolean) value : defValue;
        }

        @Override
        public boolean contains(String key) {
            return values.containsKey(key);
        }

        @Override
        public Editor edit() {
            return new Editor() {
                private final Map<String, Object> pending = new HashMap<>();
                private final Set<String> removals = new HashSet<>();
                private boolean clearRequested = false;

                @Override
                public Editor putString(String key, @Nullable String value) {
                    pending.put(key, value);
                    removals.remove(key);
                    return this;
                }

                @Override
                public Editor putStringSet(String key, @Nullable Set<String> valuesSet) {
                    pending.put(key, valuesSet == null ? null : new HashSet<>(valuesSet));
                    removals.remove(key);
                    return this;
                }

                @Override
                public Editor putInt(String key, int value) {
                    pending.put(key, value);
                    removals.remove(key);
                    return this;
                }

                @Override
                public Editor putLong(String key, long value) {
                    pending.put(key, value);
                    removals.remove(key);
                    return this;
                }

                @Override
                public Editor putFloat(String key, float value) {
                    pending.put(key, value);
                    removals.remove(key);
                    return this;
                }

                @Override
                public Editor putBoolean(String key, boolean value) {
                    pending.put(key, value);
                    removals.remove(key);
                    return this;
                }

                @Override
                public Editor remove(String key) {
                    removals.add(key);
                    pending.remove(key);
                    return this;
                }

                @Override
                public Editor clear() {
                    clearRequested = true;
                    pending.clear();
                    removals.clear();
                    return this;
                }

                @Override
                public boolean commit() {
                    apply();
                    return true;
                }

                @Override
                public void apply() {
                    if (clearRequested) {
                        values.clear();
                    }
                    for (String key : removals) {
                        values.remove(key);
                    }
                    values.putAll(pending);
                }
            };
        }

        @Override
        public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
            // no-op for tests
        }

        @Override
        public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
            // no-op for tests
        }
    }
}
