package com.glowup.ai.data.remote

import okhttp3.Interceptor
import okhttp3.Response
import okio.Buffer
import java.nio.charset.Charset

/**
 * Debug-only logger that never materializes large image/export bodies and
 * redacts identity and credential fields before they reach Logcat.
 */
class RedactingLoggingInterceptor(
    private val logger: (String) -> Unit,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        logger("--> ${request.method} ${redactUrl(request.url.toString())}")
        request.body?.let { logger(redactedRequestBody(it)) }

        val response = chain.proceed(request)
        logger("<-- ${response.code} ${redactUrl(request.url.toString())}")
        response.body?.let { body ->
            // peekBody leaves the real response stream untouched and caps the
            // amount copied into memory for exports and future image routes.
            val preview = response.peekBody(MAX_LOG_BYTES).string()
            logger(redactBody(preview, truncated = body.contentLength() > MAX_LOG_BYTES))
        }
        return response
    }

    private fun redactUrl(url: String): String = url.replace(Regex("(?i)(user_id|userid|firebase_uid|token)=[^&]+"), "$1=<redacted>")

    private fun redactedRequestBody(body: okhttp3.RequestBody): String {
        val length = body.contentLength()
        if (length > MAX_LOG_BYTES) return "<redacted body: $length bytes>"
        // Unknown-length bodies may be streaming. Never materialize one just
        // for debug logging.
        if (length < 0) return "<redacted body: streaming>"
        val buffer = Buffer()
        body.writeTo(buffer)
        val charset = body.contentType()?.charset(Charset.forName("UTF-8")) ?: Charset.forName("UTF-8")
        return redactBody(buffer.readString(charset))
    }

    private fun redactBody(
        body: String,
        truncated: Boolean = false,
    ): String {
        var redacted = body
        redacted =
            redacted.replace(
                Regex("\"([a-zA-Z_]*base64)\"\\\\s*:\\\\s*\"[^\"]*\"", RegexOption.IGNORE_CASE),
                "\"$1\":\"<redacted-image-bytes>\"",
            )
        redacted = redacted.replace(Regex("(?i)bearer\\\\s+[A-Za-z0-9\\\\-_.=]+"), "Bearer <redacted>")
        redacted =
            redacted.replace(
                Regex("\"(id_?token|access_?token|user_?id|firebase_?uid|authorization)\"\\\\s*:\\\\s*\"[^\"]*\"", RegexOption.IGNORE_CASE),
                "\"$1\":\"<redacted>\"",
            )
        return if (truncated) "$redacted…<truncated>" else redacted
    }

    private companion object {
        const val MAX_LOG_BYTES = 16L * 1024L
    }
}
