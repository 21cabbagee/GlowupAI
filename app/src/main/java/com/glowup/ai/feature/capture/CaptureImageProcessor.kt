package com.glowup.ai.feature.capture

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import kotlin.math.max

/** Decodes, orients, bounds, and compresses capture images before network upload. */
object CaptureImageProcessor {
    const val MAX_DIMENSION_PX = 1280
    const val JPEG_QUALITY = 90
    const val MIN_DIMENSION_PX = 160

    fun processCameraJpeg(jpegBytes: ByteArray, rotationDegrees: Int): Bitmap {
        require(jpegBytes.isNotEmpty()) { "Captured image is empty" }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size, bounds)
        requireValidBounds(bounds)
        val decoded = BitmapFactory.decodeByteArray(
            jpegBytes, 0, jpegBytes.size,
            BitmapFactory.Options().apply { inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight) },
        ) ?: error("Could not decode captured image")
        // CameraX OutputFileOptions stores orientation in EXIF. A non-zero explicit rotation is
        // still honoured for callers that provide an ImageProxy-derived JPEG.
        val degrees = if (rotationDegrees != 0) rotationDegrees else readExifRotationDegrees(ByteArrayInputStream(jpegBytes))
        return scaleToMaxDimension(rotateIfNeeded(decoded, degrees), MAX_DIMENSION_PX)
    }

    fun processGalleryUri(context: Context, uri: Uri): Bitmap {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            ?: error("Could not open picked image")
        requireValidBounds(bounds)
        val decoded = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply {
                inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight)
            })
        } ?: error("Could not decode picked image")
        val rotation = resolver.openInputStream(uri)?.use(::readExifRotationDegrees) ?: 0
        return scaleToMaxDimension(rotateIfNeeded(decoded, rotation), MAX_DIMENSION_PX)
    }

    fun encodeToBase64Jpeg(bitmap: Bitmap): String {
        val stream = ByteArrayOutputStream()
        check(bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, stream)) { "JPEG encoding failed" }
        return Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
    }

    fun meetsMinimumDimensions(bitmap: Bitmap): Boolean =
        bitmap.width >= MIN_DIMENSION_PX && bitmap.height >= MIN_DIMENSION_PX

    /** Normalizes a bitmap supplied by a caller that did not use one of the URI/JPEG helpers. */
    fun normalizeBitmap(bitmap: Bitmap): Bitmap = scaleToMaxDimension(bitmap, MAX_DIMENSION_PX)

    private fun requireValidBounds(options: BitmapFactory.Options) {
        require(options.outWidth > 0 && options.outHeight > 0) { "Unsupported or corrupt image" }
    }

    private fun readExifRotationDegrees(input: InputStream): Int = runCatching {
        when (ExifInterface(input).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
    }.getOrDefault(0)

    private fun rotateIfNeeded(bitmap: Bitmap, degrees: Int): Bitmap {
        val normalized = ((degrees % 360) + 360) % 360
        if (normalized == 0) return bitmap
        val rotated = Bitmap.createBitmap(
            bitmap, 0, 0, bitmap.width, bitmap.height,
            Matrix().apply { postRotate(normalized.toFloat()) }, true,
        )
        if (rotated !== bitmap) bitmap.recycle()
        return rotated
    }

    private fun scaleToMaxDimension(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val longest = max(bitmap.width, bitmap.height)
        if (longest <= maxDimension) return bitmap
        val scale = maxDimension.toFloat() / longest
        val scaled = Bitmap.createScaledBitmap(
            bitmap, max(1, (bitmap.width * scale).toInt()), max(1, (bitmap.height * scale).toInt()), true,
        )
        if (scaled !== bitmap) bitmap.recycle()
        return scaled
    }

    /** Decode at no more than roughly 2x the upload target, preventing huge gallery images from OOMing. */
    private fun calculateInSampleSize(width: Int, height: Int): Int {
        val longest = max(width, height)
        var sample = 1
        while (longest / (sample * 2) > MAX_DIMENSION_PX * 2) sample *= 2
        return sample
    }
}
