package com.example.glitch.ui;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

import androidx.annotation.NonNull;

import com.example.glitch.model.GatePolicy;
import com.example.glitch.model.GuestPass;

import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Renders a full guest-pass card image for sharing.
 */
public final class PassCardImageHelper {
    private PassCardImageHelper() {
    }

    @NonNull
    public static Bitmap render(@NonNull GuestPass pass) throws Exception {
        int width = 1080;
        int height = 1580;
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.parseColor("#F4F6F8"));

        Paint cardPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        cardPaint.setColor(Color.WHITE);
        RectF card = new RectF(60, 60, width - 60, height - 60);
        canvas.drawRoundRect(card, 36f, 36f, cardPaint);

        Paint titlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        titlePaint.setColor(Color.parseColor("#0F172A"));
        titlePaint.setTextSize(56f);
        titlePaint.setFakeBoldText(true);
        canvas.drawText("Campus Guest Pass", 110, 165, titlePaint);

        Paint subPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        subPaint.setColor(Color.parseColor("#475569"));
        subPaint.setTextSize(32f);
        canvas.drawText("Access Number (Pass Code): " + pass.getPassCode(), 110, 225, subPaint);

        Bitmap qr = QrCodeHelper.generate(pass.getPassCode(), 560);
        canvas.drawBitmap(qr, (width - qr.getWidth()) / 2f, 280, null);

        Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        labelPaint.setColor(Color.parseColor("#64748B"));
        labelPaint.setTextSize(28f);
        labelPaint.setFakeBoldText(true);

        Paint valuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        valuePaint.setColor(Color.parseColor("#111827"));
        valuePaint.setTextSize(34f);

        int rowY = 920;
        rowY = drawRow(canvas, labelPaint, valuePaint, rowY, "Guest", pass.getGuestName());
        rowY = drawRow(
                canvas,
                labelPaint,
                valuePaint,
                rowY,
                "Gate",
                GatePolicy.toDisplayLabel(pass.getGateLabel())
        );
        rowY = drawRow(canvas, labelPaint, valuePaint, rowY, "Status", pass.getStatus().toUpperCase(Locale.getDefault()));
        rowY = drawRow(canvas, labelPaint, valuePaint, rowY, "Expires", formatTimestamp(pass, true));
        rowY = drawRow(canvas, labelPaint, valuePaint, rowY, "Entry Request", pass.getEntryRequestId());
        drawRow(canvas, labelPaint, valuePaint, rowY, "Host", pass.getSponsorName());

        Paint footer = new Paint(Paint.ANTI_ALIAS_FLAG);
        footer.setColor(Color.parseColor("#6B7280"));
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
    private static String formatTimestamp(@NonNull GuestPass pass, boolean expiry) {
        if (expiry) {
            if (pass.getExpiresAt() == null) {
                return "--";
            }
            return new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(pass.getExpiresAt().toDate());
        }
        if (pass.getAdmittedAt() == null) {
            return "--";
        }
        return new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(pass.getAdmittedAt().toDate());
    }
}
