package com.glowup.ai.feature.capture

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.glowup.ai.domain.model.CapturePose
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs
import kotlin.math.tan

/** Real ML Kit face pose preflight. It is coaching only; the server remains quality authority. */
class FaceQualityAnalyzer(private val onResult: (LiveFaceQuality) -> Unit) : ImageAnalysis.Analyzer {
    private val closed = AtomicBoolean(false)
    private val detector: FaceDetector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            .enableTracking()
            .build(),
    )

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        if (closed.get()) { imageProxy.close(); return }
        val mediaImage = imageProxy.image
        if (mediaImage == null) { imageProxy.close(); return }
        val rotation = imageProxy.imageInfo.rotationDegrees
        val input = runCatching { InputImage.fromMediaImage(mediaImage, rotation) }.getOrElse {
            imageProxy.close()
            return
        }
        detector.process(input)
            .addOnSuccessListener { faces ->
                if (!closed.get()) onResult(toLiveQuality(faces, input.width, input.height, rotation))
            }
            .addOnFailureListener {
                if (!closed.get()) onResult(LiveFaceQuality.noFace())
            }
            .addOnCompleteListener { imageProxy.close() }
    }

    fun close() {
        if (closed.compareAndSet(false, true)) detector.close()
    }

    private fun toLiveQuality(faces: List<Face>, imageWidth: Int, imageHeight: Int, rotation: Int): LiveFaceQuality {
        val uprightWidth = if (rotation == 90 || rotation == 270) imageHeight else imageWidth
        val face = faces.maxByOrNull { it.boundingBox.width().toLong() * it.boundingBox.height().toLong() }
            ?: return LiveFaceQuality.noFace()
        val faceWidth = face.boundingBox.width().coerceAtLeast(1)
        val focalLength = (uprightWidth / 2.0) / tan(Math.toRadians(ASSUMED_HORIZONTAL_FOV_DEGREES / 2.0))
        val distanceCm = (ASSUMED_FACE_WIDTH_CM * focalLength / faceWidth).coerceIn(1.0, 300.0)
        val smiling = face.smilingProbability
        val leftEye = face.leftEyeOpenProbability
        val rightEye = face.rightEyeOpenProbability
        val neutral = (smiling == null || smiling < 0.4f) &&
            (leftEye == null || leftEye > 0.4f) && (rightEye == null || rightEye > 0.4f)
        return LiveFaceQuality(
            pose = CapturePose(
                facePresent = true,
                yawDegrees = face.headEulerAngleY.toDouble(),
                pitchDegrees = face.headEulerAngleX.toDouble(),
                distanceCm = distanceCm,
                expressionNeutral = neutral,
            ),
            faceFillFraction = (faceWidth.toFloat() / uprightWidth.coerceAtLeast(1)).coerceIn(0f, 1f),
        )
    }

    companion object {
        private const val ASSUMED_FACE_WIDTH_CM = 14.0
        private const val ASSUMED_HORIZONTAL_FOV_DEGREES = 70.0
        const val MAX_YAW_DEGREES = 12.0
        const val MAX_PITCH_DEGREES = 12.0
        const val MIN_DISTANCE_CM = 25.0
        const val MAX_DISTANCE_CM = 70.0
    }
}

data class LiveFaceQuality(val pose: CapturePose, val faceFillFraction: Float) {
    val isFramingGood: Boolean
        get() = pose.facePresent &&
            abs(pose.yawDegrees) <= FaceQualityAnalyzer.MAX_YAW_DEGREES &&
            abs(pose.pitchDegrees) <= FaceQualityAnalyzer.MAX_PITCH_DEGREES &&
            pose.distanceCm in FaceQualityAnalyzer.MIN_DISTANCE_CM..FaceQualityAnalyzer.MAX_DISTANCE_CM &&
            pose.expressionNeutral

    companion object { fun noFace() = LiveFaceQuality(CapturePose(false, 0.0, 0.0, 0.0, false), 0f) }
}