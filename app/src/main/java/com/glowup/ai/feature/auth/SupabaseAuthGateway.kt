package com.glowup.ai.feature.auth

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.util.Base64
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.core.net.toUri
import androidx.core.content.edit
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.glowup.ai.BuildConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import java.net.HttpURLConnection
import java.net.URL
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties

/** The small identity shape the rest of the Android app needs from Supabase Auth. */
data class SupabaseUser(
    val id: String,
    val email: String?,
    val displayName: String?,
)

/**
 * Supabase Auth boundary for the Android client.
 *
 * The app only contains the public Supabase URL and publishable/anonymous key. The service-role
 * key, database credentials, and JWT verification secret stay on the backend. Google sign-in
 * uses Android Credential Manager's native account chooser, then exchanges its ID token directly
 * with Supabase. The deep-link callback is retained only to safely consume any legacy OAuth flow.
 */
@Singleton
class SupabaseAuthGateway
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) {
    private val preferences =
        context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val sessionCipher = AuthSessionCipher()
    private val refreshLock = Mutex()
    private val callbackScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val oauthLock = Any()
    private var session: AuthSession? = loadSession()
    private var pendingOAuth: PendingOAuth? = null

    fun currentUser(): SupabaseUser? = synchronized(oauthLock) { session?.user }

    fun hasSession(): Boolean = currentUser() != null

    suspend fun accessToken(forceRefresh: Boolean = false): String? =
        refreshLock.withLock {
            val current = session ?: return@withLock null
            val now = System.currentTimeMillis() / 1000
            if (!forceRefresh && current.expiresAt > now + TOKEN_REFRESH_LEEWAY_SECONDS) {
                return@withLock current.accessToken
            }
            if (current.refreshToken.isBlank()) return@withLock current.accessToken
            try {
                val refreshed =
                    withContext(Dispatchers.IO) {
                        request(
                            method = "POST",
                            path = "/token?grant_type=refresh_token",
                            body = JSONObject().put("refresh_token", current.refreshToken),
                        )
                    }
                saveSession(refreshed, current)
                session?.accessToken
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: SupabaseAuthException) {
                // An invalid refresh token means the local session is no longer recoverable.
                clearSession()
                null
            } catch (_: Exception) {
                // Keep a still-valid access token during a transient network failure.
                current.accessToken.takeIf { current.expiresAt > now }
            }
        }

    suspend fun signInWithEmail(email: String, password: String): Result<SupabaseUser> =
        authenticate {
            request(
                method = "POST",
                path = "/token?grant_type=password",
                body = JSONObject().put("email", email).put("password", password),
            )
        }

    suspend fun createAccountWithEmail(email: String, password: String): Result<SupabaseUser> =
        authenticate {
            request(
                method = "POST",
                path = "/signup",
                body =
                    JSONObject()
                        .put("email", email)
                        .put("password", password)
                        .put("email_redirect_to", BuildConfig.SUPABASE_REDIRECT_URI),
            ).also { response ->
                if (response.optString("access_token").isBlank()) {
                    throw EmailConfirmationRequiredException()
                }
            }
        }

    suspend fun sendPasswordReset(email: String): Result<Unit> =
        runCatching {
            validateConfiguration()
            if (email.isBlank()) throw InvalidAuthInputException("Enter your email address first.")
            withContext(Dispatchers.IO) {
                request(
                    method = "POST",
                    path = "/recover",
                    body =
                        JSONObject()
                            .put("email", email)
                            .put("redirect_to", BuildConfig.SUPABASE_REDIRECT_URI),
                )
            }
            Unit
        }.rethrowCancellation()

    suspend fun updatePassword(password: String): Result<Unit> =
        runCatching {
            if (password.length < 8) throw InvalidAuthInputException("Use at least 8 characters for your new password.")
            val token = accessToken() ?: throw SupabaseAuthException(401, "missing_session", "Password reset link is invalid or expired")
            withContext(Dispatchers.IO) {
                request(
                    method = "PUT",
                    path = "/user",
                    body = JSONObject().put("password", password),
                    bearerToken = token,
                )
            }
            // Recovery sessions should not silently become long-lived app sessions.
            clearSession()
            Unit
        }.rethrowCancellation()

    /** Shows Android's native Google account chooser and exchanges the selected account's ID token. */
    suspend fun signInWithGoogle(activity: Activity): Result<SupabaseUser> =
        try {
            signInWithGoogleInternal(activity)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Throwable) {
            Result.failure(failure)
        }

    private suspend fun signInWithGoogleInternal(activity: Activity): Result<SupabaseUser> {
        validateConfiguration()
        if (activity.isFinishing || activity.isDestroyed) {
            return Result.failure(AuthActivityUnavailableException())
        }
        val webClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID.trim()
        if (webClientId.isBlank()) return Result.failure(GoogleClientIdUnavailableException())

        return try {
            val googleIdOption =
                GetGoogleIdOption.Builder()
                    // Show every Google account on the device on first use, not just prior users.
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(webClientId)
                    .setAutoSelectEnabled(false)
                    .build()
            val result =
                CredentialManager.create(activity).getCredential(
                    context = activity,
                    request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build(),
                )
            val credential = result.credential
            if (credential !is CustomCredential || credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                return Result.failure(UnexpectedGoogleCredentialException())
            }
            val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
            authenticate {
                request(
                    method = "POST",
                    path = "/token?grant_type=id_token",
                    body =
                        JSONObject()
                            .put("provider", "google")
                            .put("id_token", idToken),
                )
            }
        } catch (_: GetCredentialCancellationException) {
            Result.failure(GoogleSignInCancelledException())
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Throwable) {
            Result.failure(failure)
        }
    }

    /** Consumes a callback delivered by MainActivity. Returns false for unrelated app links. */
    fun handleAuthCallback(uri: Uri?): Boolean {
        if (
            uri == null ||
            uri.scheme != CALLBACK_SCHEME ||
            uri.host != CALLBACK_HOST ||
            uri.path?.trimEnd('/') != CALLBACK_PATH
        ) {
            return false
        }
        val callback =
            if (!uri.fragment.isNullOrBlank()) {
                "https://callback.invalid/?${uri.fragment}".toUri()
            } else {
                uri
            }
        val recoveryAccessToken = callback.getQueryParameter("access_token")
        if (callback.getQueryParameter("type") == "recovery" && !recoveryAccessToken.isNullOrBlank()) {
            val response =
                JSONObject()
                    .put("access_token", recoveryAccessToken)
                    .put("refresh_token", callback.getQueryParameter("refresh_token") ?: "")
                    .put("expires_in", callback.getQueryParameter("expires_in")?.toLongOrNull() ?: 3600)
            // Supabase implicit recovery callbacks do not include a user object. Decode only the
            // public JWT payload locally so the recovery session can be represented; the server
            // and Supabase still validate the token before any protected operation succeeds.
            val userId = jwtSubject(recoveryAccessToken)
            if (!userId.isNullOrBlank()) response.put("user", JSONObject().put("id", userId))
            runCatching { saveSession(response, null) }.onFailure { clearSession() }
            return true
        }

        // Email confirmation uses Supabase's implicit callback and returns a signed access token
        // with type=signup. Accept only that explicit callback type; the backend still validates
        // the token before creating the app's authenticated profile/session.
        val signupAccessToken = callback.getQueryParameter("access_token")
        if (callback.getQueryParameter("type") == "signup" && !signupAccessToken.isNullOrBlank()) {
            val userId = jwtSubject(signupAccessToken)
            if (userId.isNullOrBlank()) {
                clearSession()
                return true
            }
            val response =
                JSONObject()
                    .put("access_token", signupAccessToken)
                    .put("refresh_token", callback.getQueryParameter("refresh_token") ?: "")
                    .put("expires_in", callback.getQueryParameter("expires_in")?.toLongOrNull() ?: 3600)
                    .put("user", JSONObject().put("id", userId))
            runCatching { saveSession(response, null) }.onFailure { clearSession() }
            return true
        }
        val pending = synchronized(oauthLock) { pendingOAuth } ?: loadPendingOAuth() ?: return true
        callback.getQueryParameter("error_description")?.let { message ->
            completePending(pending, Result.failure(SupabaseAuthException(400, "oauth_error", message)))
            return true
        }
        callback.getQueryParameter("error")?.let { code ->
            completePending(pending, Result.failure(SupabaseAuthException(400, code, "Google sign-in was cancelled")))
            return true
        }

        val code = callback.getQueryParameter("code")
        if (code != null) {
            callbackScope.launch {
                val result =
                    runCatching {
                        val response =
                            request(
                                method = "POST",
                                path = "/token?grant_type=pkce",
                                body =
                                    JSONObject()
                                        .put("auth_code", code)
                                        .put("code_verifier", pending.verifier),
                            )
                        saveSession(response, null)
                        requireUser()
                    }.rethrowCancellation()
                completePending(pending, result)
            }
            return true
        }

        // Supports Supabase projects configured for the legacy implicit OAuth response as well.
        val accessToken = callback.getQueryParameter("access_token")
        if (!accessToken.isNullOrBlank()) {
            callbackScope.launch {
                val result =
                    runCatching {
                        val response =
                            JSONObject()
                                .put("access_token", accessToken)
                                .put("refresh_token", callback.getQueryParameter("refresh_token") ?: "")
                                .put("expires_in", callback.getQueryParameter("expires_in")?.toLongOrNull() ?: 3600)
                        saveSession(response, null)
                        requireUser()
                    }.rethrowCancellation()
                completePending(pending, result)
            }
        } else {
            completePending(pending, Result.failure(SupabaseAuthException(400, "missing_code", "Google sign-in did not return a session")))
        }
        return true
    }

    fun signOut() {
        clearSession()
    }

    fun friendlyMessage(t: Throwable): String =
        when (t) {
            is SupabaseUnavailableException -> "Sign-in isn't available on this build yet. Please try again later."
            is AuthActivityUnavailableException -> "Sign-in isn't available right now. Please try again."
            is GoogleClientIdUnavailableException -> "Google sign-in isn't configured for this build yet."
            is GoogleSignInCancelledException -> "Google sign-in was cancelled."
            is UnexpectedGoogleCredentialException -> "We couldn't use that Google account. Please try again."
            is InvalidAuthInputException -> t.userMessage
            is EmailConfirmationRequiredException -> "Check your email to confirm your account, then sign in."
            is SupabaseAuthException ->
                when (t.code) {
                    "invalid_credentials" -> "That email or password looks incorrect."
                    "user_already_exists" -> "An account already exists for that email. Try signing in instead."
                    "email_not_confirmed" -> "Check your email to confirm your account, then sign in."
                    "missing_session", "expired_token" -> "That password reset link is invalid or expired. Request a new one."
                    "over_request_rate_limit", "429" -> "Too many sign-up emails were requested. Wait about an hour, then try again."
                    else -> "That sign-in didn't work. Please check your details and try again."
                }
            else -> "Something went wrong. Please try again."
        }

    private suspend fun authenticate(requestCall: suspend () -> JSONObject): Result<SupabaseUser> =
        runCatching {
            validateConfiguration()
            val response = withContext(Dispatchers.IO) { requestCall() }
            saveSession(response, null)
            requireUser()
        }.rethrowCancellation()

    private fun validateConfiguration() {
        val url = BuildConfig.SUPABASE_URL.trim()
        if (url.isBlank() || !url.startsWith("https://") || BuildConfig.SUPABASE_ANON_KEY.isBlank()) {
            throw SupabaseUnavailableException()
        }
    }

    private fun request(
        method: String,
        path: String,
        body: JSONObject? = null,
        bearerToken: String? = null,
    ): JSONObject {
        val connection =
            (URL("${BuildConfig.SUPABASE_URL.trimEnd('/')}/auth/v1$path").openConnection() as HttpURLConnection)
        try {
            connection.requestMethod = method
            connection.connectTimeout = 10_000
            connection.readTimeout = 20_000
            connection.setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY)
            connection.setRequestProperty("Accept", "application/json")
            if (!bearerToken.isNullOrBlank()) connection.setRequestProperty("Authorization", "Bearer $bearerToken")
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (status !in 200..299) {
                val error = runCatching { JSONObject(text) }.getOrDefault(JSONObject())
                throw SupabaseAuthException(
                    status,
                    error.optString("code").ifBlank { error.optString("error") }.ifBlank { status.toString() },
                    error.optString("msg").ifBlank { error.optString("message") }.ifBlank { "Authentication request failed" },
                )
            }
            return if (text.isBlank()) JSONObject() else JSONObject(text)
        } finally {
            connection.disconnect()
        }
    }

    private fun requireUser(): SupabaseUser =
        session?.user ?: throw SupabaseAuthException(401, "missing_user", "Supabase returned no user")

    private fun saveSession(response: JSONObject, previous: AuthSession?) {
        val accessToken = response.optString("access_token").ifBlank { previous?.accessToken.orEmpty() }
        if (accessToken.isBlank()) throw EmailConfirmationRequiredException()
        val refreshToken = response.optString("refresh_token").ifBlank { previous?.refreshToken.orEmpty() }
        val userJson = response.optJSONObject("user")
        val user =
            SupabaseUser(
                id = userJson?.optString("id").orEmpty().ifBlank { previous?.user?.id.orEmpty() },
                email = userJson?.optNullableString("email") ?: previous?.user?.email,
                displayName = userJson?.displayName() ?: previous?.user?.displayName,
            )
        if (user.id.isBlank()) throw SupabaseAuthException(401, "missing_user", "Supabase returned no user")
        val expiresAt =
            response.optLong("expires_at", 0L).takeIf { it > 0 }
                ?: (System.currentTimeMillis() / 1000 + response.optLong("expires_in", 3600L))
        val saved = AuthSession(user, accessToken, refreshToken, expiresAt)
        synchronized(oauthLock) { session = saved }
        persistSession(saved)
    }

    private fun loadSession(): AuthSession? {
        return runCatching {
            preferences.getString(KEY_ENCRYPTED_SESSION, null)
                ?.takeIf { it.isNotBlank() }
                ?.let(sessionCipher::decrypt)
                ?.let(::sessionFromJson)
                ?: loadLegacySession()?.also(::persistSession)
        }.getOrElse {
            // A restored preference file cannot be decrypted with this install's hardware-backed
            // key. Fail closed and require sign-in instead of retaining unusable credentials.
            preferences.edit { clear() }
            null
        }
    }

    private fun loadLegacySession(): AuthSession? {
        val accessToken = preferences.getString(KEY_ACCESS_TOKEN, null)?.takeIf { it.isNotBlank() } ?: return null
        val userId = preferences.getString(KEY_USER_ID, null)?.takeIf { it.isNotBlank() } ?: return null
        return AuthSession(
            user = SupabaseUser(
                userId,
                preferences.getString(KEY_EMAIL, null),
                preferences.getString(KEY_DISPLAY_NAME, null),
            ),
            accessToken = accessToken,
            refreshToken = preferences.getString(KEY_REFRESH_TOKEN, "").orEmpty(),
            expiresAt = preferences.getLong(KEY_EXPIRES_AT, 0L),
        )
    }

    private fun persistSession(saved: AuthSession) {
        val payload = JSONObject()
            .put("access_token", saved.accessToken)
            .put("refresh_token", saved.refreshToken)
            .put("user_id", saved.user.id)
            .put("expires_at", saved.expiresAt)
        saved.user.email?.let { payload.put("email", it) }
        saved.user.displayName?.let { payload.put("display_name", it) }
        val encrypted = sessionCipher.encrypt(payload.toString())
        preferences.edit {
            putString(KEY_ENCRYPTED_SESSION, encrypted)
            remove(KEY_ACCESS_TOKEN)
            remove(KEY_REFRESH_TOKEN)
            remove(KEY_USER_ID)
            remove(KEY_EMAIL)
            remove(KEY_DISPLAY_NAME)
            remove(KEY_EXPIRES_AT)
        }
    }

    private fun sessionFromJson(raw: String): AuthSession {
        val payload = JSONObject(raw)
        val accessToken = payload.optString("access_token").takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Encrypted auth session has no access token")
        val userId = payload.optString("user_id").takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Encrypted auth session has no user")
        return AuthSession(
            user = SupabaseUser(
                userId,
                payload.optNullableString("email"),
                payload.optNullableString("display_name"),
            ),
            accessToken = accessToken,
            refreshToken = payload.optString("refresh_token"),
            expiresAt = payload.optLong("expires_at"),
        )
    }

    private fun clearSession() {
        synchronized(oauthLock) { session = null }
        preferences.edit { clear() }
    }

    private fun completePending(pending: PendingOAuth, result: Result<SupabaseUser>) {
        synchronized(oauthLock) {
            if (pendingOAuth === pending) pendingOAuth = null
        }
        clearPendingOAuth(pending)
        pending.deferred.complete(result)
    }

    private fun loadPendingOAuth(): PendingOAuth? {
        val verifier = preferences.getString(KEY_OAUTH_VERIFIER, null)?.takeIf { it.isNotBlank() }
            ?: return null
        return PendingOAuth(verifier, CompletableDeferred())
    }

    private fun clearPendingOAuth(pending: PendingOAuth) {
        val storedVerifier = preferences.getString(KEY_OAUTH_VERIFIER, null)
        if (storedVerifier == pending.verifier) {
            preferences.edit {
                remove(KEY_OAUTH_VERIFIER)
            }
        }
    }

    private fun randomUrlToken(bytes: Int): String =
        ByteArray(bytes).also { SecureRandom().nextBytes(it) }.let {
            Base64.encodeToString(it, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        }

    private fun jwtSubject(token: String): String? =
        runCatching {
            val payload = token.split('.').getOrNull(1) ?: return@runCatching null
            val decoded = Base64.decode(payload, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
            JSONObject(decoded.toString(Charsets.UTF_8)).optString("sub").takeIf { it.isNotBlank() }
        }.getOrNull()

    private data class AuthSession(
        val user: SupabaseUser,
        val accessToken: String,
        val refreshToken: String,
        val expiresAt: Long,
    )

    private data class PendingOAuth(
        val verifier: String,
        val deferred: CompletableDeferred<Result<SupabaseUser>>,
    )

    companion object {
        private const val PREFERENCES_NAME = "glowup_supabase_auth"
        private const val KEY_ENCRYPTED_SESSION = "encrypted_session_v1"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_EMAIL = "email"
        private const val KEY_DISPLAY_NAME = "display_name"
        private const val KEY_EXPIRES_AT = "expires_at"
        private const val KEY_OAUTH_VERIFIER = "oauth_verifier"
        private const val TOKEN_REFRESH_LEEWAY_SECONDS = 30L
        private const val OAUTH_TIMEOUT_MS = 5 * 60 * 1000L
        private const val CALLBACK_SCHEME = "glowup"
        private const val CALLBACK_HOST = "auth"
        private const val CALLBACK_PATH = "/callback"
    }
}

/** AES-GCM wrapper whose non-exportable key is owned by Android Keystore. */
private class AuthSessionCipher {
    private val keyStore: KeyStore
        get() = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }

    fun encrypt(plainText: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(cipher.iv, Base64.NO_WRAP) + "." +
            Base64.encodeToString(cipherText, Base64.NO_WRAP)
    }

    fun decrypt(encoded: String): String {
        val parts = encoded.split('.', limit = 2)
        require(parts.size == 2) { "Invalid encrypted session" }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            keyStore.getKey(KEY_ALIAS, null) as? SecretKey
                ?: throw IllegalStateException("Auth encryption key is unavailable"),
            GCMParameterSpec(128, Base64.decode(parts[0], Base64.NO_WRAP)),
        )
        return cipher.doFinal(Base64.decode(parts[1], Base64.NO_WRAP)).toString(Charsets.UTF_8)
    }

    private fun getOrCreateKey(): SecretKey {
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE).run {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build(),
            )
            generateKey()
        }
    }

    private companion object {
        const val ANDROID_KEY_STORE = "AndroidKeyStore"
        const val KEY_ALIAS = "glowup_supabase_session_v1"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}

private fun JSONObject.optNullableString(key: String): String? =
    optString(key).takeIf { it.isNotBlank() }

private fun JSONObject.displayName(): String? {
    val metadata = optJSONObject("user_metadata")
    return metadata?.optNullableString("full_name")
        ?: metadata?.optNullableString("name")
        ?: optNullableString("name")
}

private fun <T> Result<T>.rethrowCancellation(): Result<T> =
    onFailure { if (it is CancellationException) throw it }

class SupabaseUnavailableException : IllegalStateException("Supabase is not configured")

class AuthActivityUnavailableException : IllegalStateException("Sign-in activity is unavailable")

class GoogleClientIdUnavailableException : IllegalStateException("Google Web client ID is unavailable")

class GoogleSignInCancelledException : IllegalStateException("Google sign-in was cancelled")

class UnexpectedGoogleCredentialException : IllegalStateException("Unexpected Google credential")

class InvalidAuthInputException(
    val userMessage: String,
) : IllegalArgumentException(userMessage)

class EmailConfirmationRequiredException : IllegalStateException("Email confirmation is required")

class SupabaseAuthException(
    val status: Int,
    val code: String,
    message: String,
) : IllegalStateException(message)
