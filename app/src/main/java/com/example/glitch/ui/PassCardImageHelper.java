package com.example.glitch.ui;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

import androidx.annotation.NonNull;

import com.example.glitch.model.GuestPass;

import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Renders a full guest-pass card image for sharing.
 */
public final class PassCardImageHelper {
    private static final int COLOR_CANVAS_BG = 0xFFF7FAFF;
    private static final int COLOR_CARD_BG = 0xFFFFFFFF;
    private static final int COLOR_TITLE = 0xFF0F172A;
    private static final int COLOR_SUBTITLE = 0xFF4B556C;
    private static final int COLOR_LABEL = 0xFF64748B;
    private static final int COLOR_VALUE = 0xFF111827;
    private static final int COLOR_FOOTER = 0xFF6B7280;

    private PassCardImageHelper() {
    }

    @NonNull
    public static Bitmap render(@NonNull GuestPass pass) throws Exception {
        int width = 1080;
        int height = 1980;
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(COLOR_CANVAS_BG);

        Paint cardPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        cardPaint.setColor(COLOR_CARD_BG);
        RectF card = new RectF(60, 60, width - 60, height - 60);
        canvas.drawRoundRect(card, 36f, 36f, cardPaint);

        Paint titlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        titlePaint.setColor(COLOR_TITLE);
        titlePaint.setTextSize(56f);
        titlePaint.setFakeBoldText(true);
        canvas.drawText("Campus Guest Pass", 110, 165, titlePaint);

        Paint subPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        subPaint.setColor(COLOR_SUBTITLE);
        subPaint.setTextSize(32f);
        canvas.drawText("Pass Code: " + pass.getPassCode(), 110, 225, subPaint);

        Bitmap qr = QrCodeHelper.generate(pass.getPassCode(), 560);
        canvas.drawBitmap(qr, (width - qr.getWidth()) / 2f, 280, null);

        Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        labelPaint.setColor(COLOR_LABEL);
        labelPaint.setTextSize(28f);
        labelPaint.setFakeBoldText(true);

        Paint valuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        valuePaint.setColor(COLOR_VALUE);
        valuePaint.setTextSize(34f);

        int rowY = 920;
        rowY = drawRow(canvas, labelPaint, valuePaint, rowY, "CNIC", pass.getGuestIdNumber());
        rowY = drawRow(canvas, labelPaint, valuePaint, rowY, "Phone", pass.getGuestPhone());
        rowY = drawRow(canvas, labelPaint, valuePaint, rowY, "Has Vehicle", pass.hasVehicle() ? "Yes" : "No");
        rowY = drawRow(canvas, labelPaint, valuePaint, rowY, "Created At", formatCreatedTimestamp(pass));
        rowY = drawRow(canvas, labelPaint, valuePaint, rowY, "Sponsor", pass.getSponsorName());
        rowY = drawRow(canvas, labelPaint, valuePaint, rowY, "Sponsor Email", pass.getSponsorEmail());
        drawRow(canvas, labelPaint, valuePaint, rowY, "Sponsor Type", formatRole(pass.getSponsorRole()));

        Paint footer = new Paint(Paint.ANTI_ALIAS_FLAG);
        footer.setColor(COLOR_FOOTER);
        footer.setTextSize(24f);
        canvas.drawText("Use pass code if QR scan is unavailable.", 110, height - 120, footer);
        return bitmap;
    }

    private static int drawRow(
            @NonNull Canvas canvas,
            @NonNull Paint labelPaint,
            @NonNull Paint valuePaint,
            int rowY,
            @NonNull String label,
            @NonNull String value
    ) {
        canvas.drawText(label, 110, rowY, labelPaint);
        canvas.drawText(value == null || value.trim().isEmpty() ? "--" : value, 330, rowY, valuePaint);
        return rowY + 90;
    }

    @NonNull
    private static String formatCreatedTimestamp(@NonNull GuestPass pass) {
        if (pass.getCreatedAt() == null) {
            return "--";
        }
        return new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                .format(pass.getCreatedAt().toDate());
    }

    @NonNull
    private static String formatRole(@NonNull String role) {
        String normalized = role.trim().toLowerCase(Locale.getDefault());
        if (normalized.isEmpty()) {
            return "--";
        }
        if (normalized.length() == 1) {
            return normalized.toUpperCase(Locale.getDefault());
        }
        return normalized.substring(0, 1).toUpperCase(Locale.getDefault())
                + normalized.substring(1);
    }
}
