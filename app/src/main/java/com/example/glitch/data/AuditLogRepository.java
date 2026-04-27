package com.example.glitch.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.glitch.model.AccessEvent;

import java.util.List;

/**
 * Contract for admin audit log viewing and export.
 * Pattern: Repository abstraction for immutable access event records.
 * Known issue: CSV export currently returns in-memory text rather than file URI.
 */
public interface AuditLogRepository {
	void listenAuditLogs(@NonNull AuditLogListener listener);

	void exportCsv(@NonNull ExportCallback callback);

	void removeListeners();

	interface AuditLogListener {
		void onData(@NonNull List<AccessEvent> events);

		void onError(@NonNull Exception exception);
	}

	interface ExportCallback {
		void onComplete(boolean success, @NonNull String csvContent, @Nullable Exception exception);
	}
}