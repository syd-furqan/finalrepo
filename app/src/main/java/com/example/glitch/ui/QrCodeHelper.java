package com.example.glitch.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;

import androidx.annotation.NonNull;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.MultiFormatWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Utility helpers for generating and persisting guest-pass QR bitmaps.
 */
public final class QrCodeHelper {
    private QrCodeHelper() {
    }

    @NonNull
    public static Bitmap generate(@NonNull String content, int sizePx) throws WriterException {
        BitMatrix matrix = new MultiFormatWriter().encode(
                content,
                BarcodeFormat.QR_CODE,
                sizePx,
                sizePx
        );
        int width = matrix.getWidth();
        int height = matrix.getHeight();
        int[] pixels = new int[width * height];
        for (int y = 0; y < height; y++) {
            int offset = y * width;
            for (int x = 0; x < width; x++) {
                pixels[offset + x] = matrix.get(x, y) ? Color.BLACK : Color.WHITE;
            }
        }
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        return bitmap;
    }

    @NonNull
    public static File saveToCache(
            @NonNull Context context,
            @NonNull Bitmap bitmap,
            @NonNull String fileName
    ) throws IOException {
        File outFile = new File(context.getCacheDir(), fileName);
        try (FileOutputStream outputStream = new FileOutputStream(outFile)) {
            if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)) {
                throw new IOException("Unable to encode QR image.");
            }
            outputStream.flush();
        }
        return outFile;
    }
}

