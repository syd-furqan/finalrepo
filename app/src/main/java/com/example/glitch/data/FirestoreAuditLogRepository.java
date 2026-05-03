package com.example.glitch.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.glitch.model.AccessEvent;
import com.example.glitch.model.AuditExportFile;
import com.example.glitch.model.AuditLogFilter;
import com.example.glitch.model.GuestPassTimePolicy;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Firestore audit log repository with server-filtered paging + export.
 */
public class FirestoreAuditLogRepository implements AuditLogRepository {
    private static final int EXPORT_PAGE_SIZE = 200;

    private final FirebaseFirestore firestore;
    private final ExecutorService exportExecutor = Executors.newSingleThreadExecutor();
    private ListenerRegistration registration;

    public FirestoreAuditLogRepository() {
        this(FirebaseFirestore.getInstance());
    }

    FirestoreAuditLogRepository(@NonNull FirebaseFirestore firestore) {
        this.firestore = firestore;
    }

    @Override
    public void listenFirstPage(
            @NonNull AuditLogFilter filter,
            int pageSize,
            @NonNull AuditPageListener listener
    ) {
        removeListeners();
        int safePageSize = Math.max(10, pageSize);
        QueryPlan plan = buildQueryPlan(filter);
        registration = plan.query
                .limit(safePageSize)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        listener.onError(error);
                        return;
                    }
                    List<AccessEvent> filteredEvents = readFilteredEvents(snapshot, filter);
                    DocumentSnapshot lastDocument = lastDocument(snapshot);
                    boolean hasMore = snapshot != null
                            && snapshot.getDocuments().size() >= safePageSize
                            && lastDocument != null;
                    AuditPageCursor cursor = AuditPageCursor.of(lastDocument, !hasMore);
                    listener.onPage(filteredEvents, cursor, hasMore);
                });
    }

    @Override
    public void loadMore(
            @NonNull AuditLogFilter filter,
            @NonNull AuditPageCursor cursor,
            int pageSize,
            @NonNull AuditPageListener listener
    ) {
        DocumentSnapshot lastDocument = cursor.getLastDocument();
        if (cursor.isExhausted() || lastDocument == null) {
            listener.onPage(new ArrayList<>(), cursor, false);
            return;
        }

        int safePageSize = Math.max(10, pageSize);
        QueryPlan plan = buildQueryPlan(filter);
        plan.query
                .startAfter(lastDocument)
                .limit(safePageSize)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<AccessEvent> filteredEvents = readFilteredEvents(snapshot, filter);
                    DocumentSnapshot nextLastDocument = lastDocument(snapshot);
                    boolean hasMore = snapshot.getDocuments().size() >= safePageSize
                            && nextLastDocument != null;
                    AuditPageCursor nextCursor = AuditPageCursor.of(nextLastDocument, !hasMore);
                    listener.onPage(filteredEvents, nextCursor, hasMore);
                })
                .addOnFailureListener(listener::onError);
    }

    @Override
    public void exportCsv(@NonNull AuditLogFilter filter, @NonNull ExportCallback callback) {
        exportExecutor.execute(() -> {
            try {
                List<AccessEvent> events = fetchAllFilteredEvents(filter);
                String csvContent = AuditCsvFormatter.toCsv(events, GuestPassTimePolicy.CAMPUS_TIME_ZONE_ID);
                String fileName = "audit_logs_" + timestampToken() + ".csv";
                AuditExportFile file = new AuditExportFile(
                        fileName,
                        "text/csv",
                        csvContent.getBytes(StandardCharsets.UTF_8)
                );
                callback.onComplete(true, file, null);
            } catch (Exception exception) {
                callback.onComplete(false, null, exception);
            }
        });
    }

    @Override
    public void exportPdf(
            @NonNull AuditLogFilter filter,
            @NonNull String generatedByUid,
            @NonNull String generatedByRole,
            @NonNull String timezoneId,
            @NonNull ExportCallback callback
    ) {
        exportExecutor.execute(() -> {
            try {
                List<AccessEvent> events = fetchAllFilteredEvents(filter);
                byte[] content = AuditPdfBuilder.build(events, filter, generatedByUid, generatedByRole, timezoneId);
                String fileName = "audit_logs_" + timestampToken() + ".pdf";
                AuditExportFile file = new AuditExportFile(fileName, "application/pdf", content);
                callback.onComplete(true, file, null);
            } catch (IOException ioException) {
                callback.onComplete(false, null, ioException);
            } catch (Exception exception) {
                callback.onComplete(false, null, exception);
            }
        });
    }

    @NonNull
    private List<AccessEvent> fetchAllFilteredEvents(@NonNull AuditLogFilter filter) throws Exception {
        List<AccessEvent> events = new ArrayList<>();
        QueryPlan plan = buildQueryPlan(filter);
        Query query = plan.query.limit(EXPORT_PAGE_SIZE);
        DocumentSnapshot cursor = null;
        boolean hasMore = true;

        while (hasMore) {
            if (cursor != null) {
                query = plan.query.startAfter(cursor).limit(EXPORT_PAGE_SIZE);
            }
            QuerySnapshot snapshot = Tasks.await(query.get());
            List<DocumentSnapshot> docs = snapshot.getDocuments();
            for (DocumentSnapshot doc : docs) {
                AccessEvent event = AccessEvent.fromMap(doc.getId(), doc.getData());
                if (matchesFilter(event, filter)) {
                    events.add(event);
                }
            }
            if (docs.isEmpty()) {
                hasMore = false;
            } else {
                cursor = docs.get(docs.size() - 1);
                hasMore = docs.size() >= EXPORT_PAGE_SIZE;
            }
        }
        return events;
    }

    @NonNull
    private List<AccessEvent> readFilteredEvents(
            @Nullable QuerySnapshot snapshot,
            @NonNull AuditLogFilter filter
    ) {
        List<AccessEvent> result = new ArrayList<>();
        if (snapshot == null) {
            return result;
        }
        for (DocumentSnapshot document : snapshot.getDocuments()) {
            AccessEvent event = AccessEvent.fromMap(document.getId(), document.getData());
            if (matchesFilter(event, filter)) {
                result.add(event);
            }
        }
        return result;
    }

    private boolean matchesFilter(@NonNull AccessEvent event, @NonNull AuditLogFilter filter) {
        long createdMillis = event.getCreatedAt() == null
                ? 0L
                : event.getCreatedAt().toDate().getTime();
        if (createdMillis > 0L && !filter.isDateInRange(createdMillis)) {
            return false;
        }
        if (!filter.matchesEventType(event.getEventType())) {
            return false;
        }
        if (!filter.matchesActorRole(event.getActorRole())) {
            return false;
        }
        return filter.matchesText(event);
    }

    @NonNull
    private QueryPlan buildQueryPlan(@NonNull AuditLogFilter filter) {
        Query query = firestore.collection("access_events");
        query = query.whereGreaterThanOrEqualTo("createdAt", new Timestamp(new Date(filter.getFromInclusiveMillis())));
        query = query.whereLessThanOrEqualTo("createdAt", new Timestamp(new Date(filter.getToInclusiveMillis())));

        boolean disjunctiveFilterUsed = false;
        List<String> eventTypes = filter.getEventTypes();
        if (eventTypes.size() == 1) {
            query = query.whereEqualTo("eventType", eventTypes.get(0));
        } else if (eventTypes.size() > 1 && eventTypes.size() <= 10) {
            query = query.whereIn("eventType", eventTypes);
            disjunctiveFilterUsed = true;
        }

        List<String> actorRoles = filter.getActorRoles();
        if (actorRoles.size() == 1) {
            query = query.whereEqualTo("actorRole", actorRoles.get(0));
        } else if (actorRoles.size() > 1 && actorRoles.size() <= 10 && !disjunctiveFilterUsed) {
            query = query.whereIn("actorRole", actorRoles);
            disjunctiveFilterUsed = true;
        }

        String token = filter.getSearchToken();
        if (!token.isEmpty() && !disjunctiveFilterUsed) {
            query = query.whereArrayContains("searchKeywords", token);
        }

        query = query.orderBy("createdAt", Query.Direction.DESCENDING);
        return new QueryPlan(query);
    }

    @Nullable
    private DocumentSnapshot lastDocument(@Nullable QuerySnapshot snapshot) {
        if (snapshot == null || snapshot.getDocuments().isEmpty()) {
            return null;
        }
        List<DocumentSnapshot> docs = snapshot.getDocuments();
        return docs.get(docs.size() - 1);
    }

    @NonNull
    private String timestampToken() {
        return new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
    }

    private static final class QueryPlan {
        private final Query query;

        private QueryPlan(@NonNull Query query) {
            this.query = query;
        }
    }

    @Override
    public void removeListeners() {
        if (registration != null) {
            registration.remove();
            registration = null;
        }
    }
}
