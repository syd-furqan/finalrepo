package com.example.glitch.data;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import com.example.glitch.model.CnicScanResult;
import com.example.glitch.model.GuestIdentityPolicy;
import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CnicOcrService {

    private static final String TESS_DATA_DIR = "tessdata";
    private static final String LANG = "eng";

    // Matches CNIC with or without dashes/spaces between groups: XXXXX-XXXXXXX-X
    private static final Pattern CNIC_PATTERN =
            Pattern.compile("(\\d{5})[\\-\\s]?(\\d{7})[\\-\\s]?(\\d)");

    private CnicOcrService() {}

    public static void initTessData(@NonNull Context context) {
        File tessDir = new File(context.getFilesDir(), TESS_DATA_DIR);
        if (!tessDir.exists()) tessDir.mkdirs();
        File trainedData = new File(tessDir, LANG + ".traineddata");
        if (trainedData.exists()) return;

        try (InputStream in = context.getAssets().open(TESS_DATA_DIR + "/" + LANG + ".traineddata");
             OutputStream out = new FileOutputStream(trainedData)) {
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
        } catch (IOException e) {
            // surfaces as OCR failure
        }
    }

    @WorkerThread
    @NonNull
    public static CnicScanResult run(@NonNull Context context, @NonNull Bitmap source) {
        File tessDir = context.getFilesDir();
        File dataFile = new File(tessDir, TESS_DATA_DIR + "/" + LANG + ".traineddata");
        if (!dataFile.exists()) {
            initTessData(context);
            if (!dataFile.exists()) {
                return CnicScanResult.failure("", "OCR data not initialized. Please try again.");
            }
        }

        // Try multiple preprocessing variants — return first success
        Bitmap[] variants = buildVariants(source);
        String bestRaw = "";
        for (Bitmap variant : variants) {
            String raw = runTesseract(tessDir, variant);
            if (variant != source) variant.recycle();
            if (raw == null) continue;
            if (raw.length() > bestRaw.length()) bestRaw = raw;
            CnicScanResult result = extractCnic(raw);
            if (result.isSuccess()) return result;
        }

        // Last attempt on best raw text collected
        if (!bestRaw.isEmpty()) {
            CnicScanResult result = extractCnic(bestRaw);
            if (result.isSuccess()) return result;
        }

        return CnicScanResult.failure(bestRaw,
                "No CNIC detected. Ensure the CNIC number is clearly visible, well-lit, and in focus.");
    }

    private static String runTesseract(@NonNull File tessDir, @NonNull Bitmap bitmap) {
        TessBaseAPI tess = new TessBaseAPI();
        try {
            if (!tess.init(tessDir.getAbsolutePath(), LANG)) return null;
            // No whitelist — let Tesseract read freely, we fix confusions in post
            tess.setPageSegMode(TessBaseAPI.PageSegMode.PSM_AUTO);
            tess.setImage(bitmap);
            String text = tess.getUTF8Text();
            return text == null ? "" : text;
        } finally {
            tess.recycle();
        }
    }

    /** Build several image variants to maximize OCR hit rate. */
    @NonNull
    private static Bitmap[] buildVariants(@NonNull Bitmap src) {
        // Ensure the image is large enough for Tesseract (min 1000px wide)
        Bitmap scaled = ensureMinWidth(src, 1200);
        Bitmap gray = toGrayscale(scaled);
        Bitmap highContrast = boostContrast(gray, 2.0f);
        Bitmap medContrast = boostContrast(gray, 1.5f);

        if (scaled == src) {
            // didn't scale, return processed variants only
            return new Bitmap[]{highContrast, medContrast, gray};
        }
        return new Bitmap[]{highContrast, medContrast, gray, scaled};
    }

    @NonNull
    private static CnicScanResult extractCnic(@NonNull String rawText) {
        // Fix common OCR character confusions
        String corrected = rawText
                .replace("O", "0").replace("o", "0")
                .replace("I", "1").replace("l", "1").replace("|", "1")
                .replace("S", "5").replace("s", "5")
                .replace("B", "8")
                .replace("G", "6").replace("g", "6")
                .replace("Z", "2").replace("z", "2")
                .replace("A", "4")
                .replace(" ", "");

        // Try with the pattern directly
        Matcher matcher = CNIC_PATTERN.matcher(corrected);
        while (matcher.find()) {
            String candidate = matcher.group(1) + "-" + matcher.group(2) + "-" + matcher.group(3);
            String normalized = GuestIdentityPolicy.normalizeCnic(candidate);
            if (normalized != null) {
                return CnicScanResult.success(rawText, candidate, normalized);
            }
        }

        // Fallback: strip everything except digits, check if we have exactly 13
        String digitsOnly = corrected.replaceAll("[^0-9]", "");
        if (digitsOnly.length() == 13) {
            String normalized = GuestIdentityPolicy.normalizeCnic(digitsOnly);
            if (normalized != null) {
                return CnicScanResult.success(rawText, digitsOnly, normalized);
            }
        }

        // Sliding window: find any 13-digit sequence inside a longer string
        if (digitsOnly.length() > 13) {
            for (int i = 0; i <= digitsOnly.length() - 13; i++) {
                String candidate = digitsOnly.substring(i, i + 13);
                String normalized = GuestIdentityPolicy.normalizeCnic(candidate);
                if (normalized != null) {
                    return CnicScanResult.success(rawText, candidate, normalized);
                }
            }
        }

        return CnicScanResult.failure(rawText,
                "No CNIC found in image. Make sure the CNIC number is clearly visible.");
    }

    @NonNull
    private static Bitmap ensureMinWidth(@NonNull Bitmap src, int minWidth) {
        if (src.getWidth() >= minWidth) return src;
        float scale = (float) minWidth / src.getWidth();
        int newW = minWidth;
        int newH = Math.round(src.getHeight() * scale);
        return Bitmap.createScaledBitmap(src, newW, newH, true);
    }

    @NonNull
    private static Bitmap toGrayscale(@NonNull Bitmap src) {
        Bitmap out = Bitmap.createBitmap(src.getWidth(), src.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(out);
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0);
        Paint paint = new Paint();
        paint.setColorFilter(new ColorMatrixColorFilter(cm));
        canvas.drawBitmap(src, 0, 0, paint);
        return out;
    }

    @NonNull
    private static Bitmap boostContrast(@NonNull Bitmap src, float contrast) {
        float translate = (-0.5f * contrast + 0.5f) * 255f;
        float[] matrix = {
                contrast, 0, 0, 0, translate,
                0, contrast, 0, 0, translate,
                0, 0, contrast, 0, translate,
                0, 0, 0, 1, 0
        };
        Bitmap out = Bitmap.createBitmap(src.getWidth(), src.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(out);
        Paint paint = new Paint();
        paint.setColorFilter(new ColorMatrixColorFilter(new ColorMatrix(matrix)));
        canvas.drawBitmap(src, 0, 0, paint);
        return out;
    }
}
