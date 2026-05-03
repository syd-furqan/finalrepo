package com.example.glitch.data;

import androidx.annotation.NonNull;

import com.example.glitch.model.AccessEvent;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Converts audit events into deterministic CSV output.
 */
final class AuditCsvFormatter {
    private AuditCsvFormatter() {
    }

    @NonNull
    static String toCsv(@NonNull List<AccessEvent> events, @NonNull String timezoneId) {
        StringBuilder builder = new StringBuilder();
        builder.append("id,eventType,schemaVersion,entityType,entityId,requestId,actorUid,actorRole,source,outcome,reasonCode,gateLabel,description,createdAt,metadata\n");
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        formatter.setTimeZone(TimeZone.getTimeZone(timezoneId));
        for (AccessEvent event : events) {
            builder.append(escape(event.getId())).append(',')
                    .append(escape(event.getEventType())).append(',')
                    .append(event.getSchemaVersion()).append(',')
                    .append(escape(event.getEntityType())).append(',')
                    .append(escape(event.getEntityId())).append(',')
                    .append(escape(event.getRequestId())).append(',')
                    .append(escape(event.getActorUid())).append(',')
                    .append(escape(event.getActorRole())).append(',')
                    .append(escape(event.getSource())).append(',')
                    .append(escape(event.getOutcome())).append(',')
                    .append(escape(event.getReasonCode())).append(',')
                    .append(escape(event.getGateLabel())).append(',')
                    .append(escape(event.getDescription())).append(',');
            if (event.getCreatedAt() != null) {
                builder.append(escape(formatter.format(event.getCreatedAt().toDate())));
            }
            builder.append(',').append(escape(event.getMetadata().toString())).append('\n');
        }
        return builder.toString();
    }

    @NonNull
    static String escape(@NonNull String value) {
        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\n")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }
}
