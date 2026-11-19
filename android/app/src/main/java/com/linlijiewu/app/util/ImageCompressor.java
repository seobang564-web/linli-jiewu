package com.linlijiewu.app.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import androidx.exifinterface.media.ExifInterface;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 发布页图片压缩：相册原图 → EXIF 方向矫正 → 采样缩放（最长边 {@link #MAX_DIMENSION}）→ JPEG 重编码。
 * 全部在后台线程调用，禁止在主线程执行。
 */
public final class ImageCompressor {

    public static final int MAX_DIMENSION = 1280;
    public static final int JPEG_QUALITY = 85;
    private static final long MAX_OUTPUT_BYTES = 900 * 1024;

    public static final class Result {
        public final byte[] jpeg;
        public final int width;
        public final int height;

        Result(byte[] jpeg, int width, int height) {
            this.jpeg = jpeg;
            this.width = width;
            this.height = height;
        }
    }

    private ImageCompressor() {
    }

    public static Result compress(Context context, Uri sourceUri) throws IOException {
        Bitmap bitmap = decodeSampled(context, sourceUri);
        bitmap = rotateByExif(context, sourceUri, bitmap);
        try {
            return new Result(encodeJpeg(bitmap), bitmap.getWidth(), bitmap.getHeight());
        } finally {
            bitmap.recycle();
        }
    }

    private static Bitmap decodeSampled(Context context, Uri sourceUri) throws IOException {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream in = context.getContentResolver().openInputStream(sourceUri)) {
            BitmapFactory.decodeStream(in, null, bounds);
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw new IOException("无法读取所选图片");
        }
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight);
        Bitmap sampled;
        try (InputStream in = context.getContentResolver().openInputStream(sourceUri)) {
            sampled = BitmapFactory.decodeStream(in, null, opts);
        }
        if (sampled == null) {
            throw new IOException("图片解码失败");
        }
        int maxEdge = Math.max(sampled.getWidth(), sampled.getHeight());
        if (maxEdge <= MAX_DIMENSION) {
            return sampled;
        }
        float scale = MAX_DIMENSION / (float) maxEdge;
        Bitmap scaled = Bitmap.createScaledBitmap(sampled,
                Math.round(sampled.getWidth() * scale), Math.round(sampled.getHeight() * scale), true);
        sampled.recycle();
        return scaled;
    }

    static int calculateInSampleSize(int width, int height) {
        int sample = 1;
        int maxEdge = Math.max(width, height);
        while (maxEdge / (sample * 2) >= MAX_DIMENSION) {
            sample *= 2;
        }
        return sample;
    }

    private static Bitmap rotateByExif(Context context, Uri sourceUri, Bitmap bitmap) throws IOException {
        int orientation;
        try (InputStream in = context.getContentResolver().openInputStream(sourceUri)) {
            if (in == null) {
                return bitmap;
            }
            orientation = new ExifInterface(in).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
        }
        int degrees;
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                degrees = 90;
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                degrees = 180;
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                degrees = 270;
                break;
            default:
                return bitmap;
        }
        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        Bitmap rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        bitmap.recycle();
        return rotated;
    }

    private static byte[] encodeJpeg(Bitmap bitmap) {
        int quality = JPEG_QUALITY;
        while (true) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out);
            byte[] bytes = out.toByteArray();
            if (bytes.length <= MAX_OUTPUT_BYTES || quality <= 40) {
                return bytes;
            }
            quality -= 15;
        }
    }
}
