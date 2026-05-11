package com.example.glitch.ui;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.example.glitch.data.CnicOcrService;
import com.example.glitch.model.CnicScanResult;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Handles camera capture, gallery pick, and background Tesseract OCR for CNIC fields.
 * Register in Fragment.onCreate(), call bindButtons() in onViewCreated().
 */
public final class CnicOcrHelper {

    public interface Callback {
        void onResult(@NonNull CnicScanResult result);
        void onScanStarted();
    }

    private static final String FILE_PROVIDER_AUTHORITY = "com.example.glitch.fileprovider";
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private final Fragment fragment;
    private final Callback callback;

    private ActivityResultLauncher<Uri> cameraLauncher;
    private ActivityResultLauncher<String> galleryLauncher;
    private ActivityResultLauncher<String> permissionLauncher;

    private Uri pendingCameraUri;
    private boolean pendingCameraAfterPermission = false;

    public CnicOcrHelper(@NonNull Fragment fragment, @NonNull Callback callback) {
        this.fragment = fragment;
        this.callback = callback;
    }

    /**
     * Must be called from Fragment.onCreate() or before onStart().
     */
    public void register() {
        cameraLauncher = fragment.registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                success -> {
                    if (Boolean.TRUE.equals(success) && pendingCameraUri != null) {
                        runOcr(pendingCameraUri);
                    } else if (Boolean.FALSE.equals(success)) {
                        // User cancelled or camera failed — no-op, don't show error
                    }
                }
        );

        galleryLauncher = fragment.registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        runOcr(uri);
                    }
                }
        );

        permissionLauncher = fragment.registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (granted && pendingCameraAfterPermission) {
                        pendingCameraAfterPermission = false;
                        launchCamera();
                    }
                }
        );

        // Init tessdata off main thread once at registration time
        Context appContext = fragment.requireContext().getApplicationContext();
        EXECUTOR.execute(() -> CnicOcrService.initTessData(appContext));
    }

    /** Call from button click to launch camera. */
    public void launchCameraOrRequestPermission() {
        if (ContextCompat.checkSelfPermission(fragment.requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            launchCamera();
        } else {
            pendingCameraAfterPermission = true;
            permissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    /** Call from button click to open gallery. */
    public void launchGallery() {
        galleryLauncher.launch("image/*");
    }

    private void launchCamera() {
        Context ctx = fragment.requireContext();
        File cacheDir = ctx.getCacheDir();
        if (!cacheDir.exists()) cacheDir.mkdirs();
        File photoFile = new File(cacheDir, "cnic_capture_" + System.currentTimeMillis() + ".jpg");
        try {
            // File must exist before FileProvider can grant a URI for it
            photoFile.createNewFile();
        } catch (java.io.IOException ignored) {}
        pendingCameraUri = FileProvider.getUriForFile(ctx, FILE_PROVIDER_AUTHORITY, photoFile);
        cameraLauncher.launch(pendingCameraUri);
    }

    private void runOcr(@NonNull Uri imageUri) {
        callback.onScanStarted();
        Context appContext = fragment.requireContext().getApplicationContext();
        EXECUTOR.execute(() -> {
            Bitmap bitmap = decodeBitmap(appContext, imageUri);
            CnicScanResult result;
            if (bitmap == null) {
                result = CnicScanResult.failure("", "Could not load image.");
            } else {
                result = CnicOcrService.run(appContext, bitmap);
                bitmap.recycle();
            }
            MAIN.post(() -> {
                if (fragment.isAdded()) {
                    callback.onResult(result);
                }
            });
        });
    }

    private static Bitmap decodeBitmap(@NonNull Context context, @NonNull Uri uri) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.Source source = ImageDecoder.createSource(context.getContentResolver(), uri);
                return ImageDecoder.decodeBitmap(source, (decoder, info, src) -> {
                    decoder.setMutableRequired(true);
                    // Scale down very large images (>3000px) to keep memory reasonable
                    int w = info.getSize().getWidth();
                    int h = info.getSize().getHeight();
                    int maxDim = 3000;
                    if (w > maxDim || h > maxDim) {
                        float scale = maxDim / (float) Math.max(w, h);
                        decoder.setTargetSize((int) (w * scale), (int) (h * scale));
                    }
                });
            } else {
                try (InputStream in = context.getContentResolver().openInputStream(uri)) {
                    if (in == null) return null;
                    BitmapFactory.Options opts = new BitmapFactory.Options();
                    opts.inMutable = true;
                    return BitmapFactory.decodeStream(in, null, opts);
                }
            }
        } catch (IOException e) {
            return null;
        }
    }
}
