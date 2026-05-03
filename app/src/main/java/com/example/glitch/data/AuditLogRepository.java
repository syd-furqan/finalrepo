package com.example.glitch.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.glitch.model.AccessEvent;
import com.example.glitch.model.AuditExportFile;
import com.example.glitch.model.AuditLogFilter;

import java.util.List;

/**
 * Contract for admin audit log viewing and export.
 * Pattern: Repository abstraction for immutable access event records.
 * Known issue: CSV export currently returns in-memory text rather than file URI.
 */
public interface AuditLogRepository {
	void listenFirstPage(
			@NonNull AuditLogFilter filter,
			int pageSize,
			@NonNull AuditPageListener listener
	);

	void loadMore(
			@NonNull AuditLogFilter filter,
			@NonNull AuditPageCursor cursor,
			int pageSize,
			@NonNull AuditPageListener listener
	);

	void exportCsv(@NonNull AuditLogFilter filter, @NonNull ExportCallback callback);

	void exportPdf(
			@NonNull AuditLogFilter filter,
			@NonNull String generatedByUid,
			@NonNull String generatedByRole,
			@NonNull String timezoneId,
			@NonNull ExportCallback callback
	);

	void removeListeners();

	interface AuditPageListener {
		void onPage(
				@NonNull List<AccessEvent> events,
				@NonNull AuditPageCursor nextCursor,
				boolean hasMore
		);

		void onError(@NonNull Exception exception);
	}

	interface ExportCallback {
		void onComplete(boolean success, @Nullable AuditExportFile file, @Nullable Exception exception);
	}
}
