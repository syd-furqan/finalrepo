package com.example.glitch.ui;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertEquals;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.example.glitch.MainActivity;
import com.example.glitch.R;
import com.example.glitch.data.EntryRequestRepository;
import com.example.glitch.data.RepositoryProvider;
import com.example.glitch.model.DashboardState;
import com.example.glitch.model.EntryRequest;
import com.google.firebase.Timestamp;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

/**
 * Basic instrumentation tests for dashboard rendering and action flow.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class DashboardFragmentTest {
    private FakeEntryRequestRepository fakeRepository;

    @Before
    public void setup() {
        fakeRepository = new FakeEntryRequestRepository();
        RepositoryProvider.setOverrideRepository(fakeRepository);
    }

    @After
    public void tearDown() {
        RepositoryProvider.clearOverride();
    }

    @Test
    public void dashboardRendersFromRepositoryData() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            onView(withText("Ahmed Mansoor")).check(matches(isDisplayed()));
            onView(withText("Level 1 - Open")).check(matches(isDisplayed()));
        }
    }

    @Test
    public void detailsAndConfirmExit_callsRepositoryWrite() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            onView(withId(R.id.button_details)).perform(click());
            onView(withId(R.id.text_details_title)).check(matches(isDisplayed()));
            onView(withId(R.id.button_sheet_log_exit)).perform(click());
            onView(withText(R.string.confirm_action)).perform(click());
            assertEquals("request-1", fakeRepository.lastLoggedExitRequestId);
        }
    }

    private static class FakeEntryRequestRepository implements EntryRequestRepository {
        String lastLoggedExitRequestId;

        @Override
        public void listenActiveRequests(@NonNull RequestListListener listener) {
            List<EntryRequest> requests = new ArrayList<>();
            requests.add(new EntryRequest(
                    "request-1",
                    "Ahmed Mansoor",
                    "Guest",
                    "Prof. Salman",
                    "West Wing - 02",
                    "CNIC-12345",
                    new Timestamp(1720400000L, 0),
                    "active",
                    null,
                    "guest"
            ));
            listener.onData(requests);
        }

        @Override
        public void listenDashboardState(@NonNull DashboardStateListener listener) {
            listener.onData(DashboardState.defaultState());
        }

        @Override
        public void searchRequests(@NonNull String query, @NonNull RequestListListener listener) {
            listener.onData(new ArrayList<>());
        }

        @Override
        public void verifyCredential(@NonNull String identifier, @NonNull CredentialListener listener) {
            listener.onData(new com.example.glitch.model.CredentialVerificationResult(
                    false,
                    identifier,
                    "",
                    "not implemented in fake"
            ));
        }

        @Override
        public void createEntryRequest(
                @NonNull String requesterUid,
                @NonNull String requesterRole,
                @NonNull String guestName,
                @NonNull String guestIdNumber,
                @NonNull String gateLabel,
                @NonNull String hostName,
                @Nullable Timestamp expiresAt,
                @NonNull CompletionCallback callback
        ) {
            callback.onComplete(true, "ok", null);
        }

        @Override
        public void logEntry(@NonNull String requestId, @NonNull String gateLabel, @NonNull CompletionCallback callback) {
            callback.onComplete(true, "ok", null);
        }

        @Override
        public void logExit(@NonNull String requestId, @NonNull CompletionCallback callback) {
            lastLoggedExitRequestId = requestId;
            callback.onComplete(true, "ok", null);
        }

        @Override
        public void denyRequest(@NonNull String requestId, @NonNull String reason, @NonNull CompletionCallback callback) {
            callback.onComplete(true, "ok", null);
        }

        @Override
        public void removeListeners() {
            // No-op in fake.
        }
    }
}
