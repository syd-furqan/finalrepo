package com.example.glitch.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.glitch.R;
import com.example.glitch.model.NotificationItem;
import com.example.glitch.model.VehicleRequestRecord;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;

/**
 * Instrumentation coverage for US-06/US-07/US-08 UI additions.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class FacultyStaffUiFlowTest {

    @Test
    public void facultyAccessRequestLayout_exposesExpiryHoursInput() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        View root = LayoutInflater.from(context).inflate(
                R.layout.fragment_faculty_access_request,
                new FrameLayout(context),
                false
        );

        View expiryInput = root.findViewById(R.id.input_request_expiry_hours);
        assertNotNull(expiryInput);
        int inputType = ((com.google.android.material.textfield.TextInputEditText) expiryInput).getInputType();
        assertTrue((inputType & InputType.TYPE_CLASS_NUMBER) != 0);
    }

    @Test
    public void notificationAdapter_clickInvokesSelectionCallback() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        final String[] selectedId = new String[1];
        NotificationAdapter adapter = new NotificationAdapter(item -> selectedId[0] = item.getId());

        NotificationItem item = new NotificationItem(
                "notif-1",
                "Request approved",
                "Your guest was approved.",
                "approval",
                false,
                null
        );

        adapter.submitList(Collections.singletonList(item));
        FrameLayout parent = new FrameLayout(context);
        NotificationAdapter.NotificationViewHolder holder = adapter.onCreateViewHolder(parent, 0);
        adapter.onBindViewHolder(holder, 0);

        holder.itemView.performClick();

        assertEquals("notif-1", selectedId[0]);
        assertEquals(1f, holder.itemView.getAlpha(), 0.01f);
    }

    @Test
    public void notificationAdapter_readItemUsesDimmedVisualState() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        NotificationAdapter adapter = new NotificationAdapter();

        NotificationItem item = new NotificationItem(
                "notif-2",
                "Request denied",
                "Reason shared by guard.",
                "denial",
                true,
                null
        );

        adapter.submitList(Collections.singletonList(item));
        FrameLayout parent = new FrameLayout(context);
        NotificationAdapter.NotificationViewHolder holder = adapter.onCreateViewHolder(parent, 0);
        adapter.onBindViewHolder(holder, 0);

        assertEquals(0.8f, holder.itemView.getAlpha(), 0.01f);
    }

    @Test
    public void vehicleRequestAdapter_clickInvokesSelectionCallback() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        final String[] selectedId = new String[1];
        VehicleRequestAdapter adapter = new VehicleRequestAdapter(record -> selectedId[0] = record.getId());

        VehicleRequestRecord record = new VehicleRequestRecord(
                "vehicle-1",
                "uid-1",
                "LEA-123",
                "Toyota Corolla",
                "pending",
                null
        );

        adapter.submitList(Collections.singletonList(record));
        FrameLayout parent = new FrameLayout(context);
        VehicleRequestAdapter.VehicleRequestViewHolder holder = adapter.onCreateViewHolder(parent, 0);
        adapter.onBindViewHolder(holder, 0);

        holder.itemView.performClick();

        assertEquals("vehicle-1", selectedId[0]);
        assertEquals(
                context.getString(R.string.vehicle_edit_pending_hint),
                holder.textHint.getText().toString()
        );
    }
}
