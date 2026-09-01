package com.glowup.ai.feature.auth

import android.app.Activity
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.OAuthProvider
import kotlinx.coroutines.CancellationException

/** Safe boundary around Firebase Auth. Missing google-services.json is a supported runtime state. */
object FirebaseAuthGateway {
    fun instanceOrNull(): FirebaseAuth? =
        try {
            FirebaseAuth.getInstance()
        } catch (_: Exception) {
            null
        }

    fun currentUser(): FirebaseUser? = instanceOrNull()?.currentUser

    fun signOut() {
        instanceOrNull()?.signOut()
    }

    suspend fun signInWithGoogle(activity: Activity): Result<FirebaseUser> {
        val auth = instanceOrNull() ?: return Result.failure(FirebaseUnavailableException())
        if (activity.isFinishing || activity.isDestroyed) return Result.failure(AuthActivityUnavailableException())
        return try {
            val provider =
                OAuthProvider
                    .newBuilder("google.com")
                    .addCustomParameter("prompt", "select_account")
                    .build()
            val task: Task<AuthResult> =
                auth.pendingAuthResult
                    ?: auth.startActivityForSignInWithProvider(activity, provider)
            requireUser(task.await())
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Throwable) {
            Result.failure(failure)
        }
    }

    suspend fun signInWithEmail(
        email: String,
        password: String,
    ): Result<FirebaseUser> {
        val auth = instanceOrNull() ?: return Result.failure(FirebaseUnavailableException())
        validateCredentials(email, password)?.let { return Result.failure(it) }
        return try {
            requireUser(auth.signInWithEmailAndPassword(email, password).await())
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Throwable) {
            Result.failure(failure)
        }
    }

    suspend fun createAccountWithEmail(
        email: String,
        password: String,
    ): Result<FirebaseUser> {
        val auth = instanceOrNull() ?: return Result.failure(FirebaseUnavailableException())
        validateCredentials(email, password)?.let { return Result.failure(it) }
        return try {
            requireUser(auth.createUserWithEmailAndPassword(email, password).await())
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Throwable) {
            Result.failure(failure)
        }
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> {
        val auth = instanceOrNull() ?: return Result.failure(FirebaseUnavailableException())
        if (email.isBlank()) return Result.failure(InvalidAuthInputException("Enter your email address first."))
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Throwable) {
            Result.failure(failure)
        }
    }

    private fun validateCredentials(
        email: String,
        password: String,
    ): Throwable? =
        when {
            email.isBlank() ||
                !android.util.Patterns.EMAIL_ADDRESS
                    .matcher(email)
                    .matches()
            -> {
                InvalidAuthInputException("Enter a valid email address.")
            }

            password.length < 6 -> {
                InvalidAuthInputException("Choose a password with at least 6 characters.")
            }

            else -> {
                null
            }
        }

    private fun requireUser(result: AuthResult): Result<FirebaseUser> =
        result.user?.let(Result.Companion::success)
            ?: Result.failure(IllegalStateException("Firebase returned no signed-in user"))

    fun friendlyMessage(t: Throwable): String =
        when (t) {
            is FirebaseUnavailableException -> {
                "Sign-in isn't available on this build yet. Please try again later."
            }

            is AuthActivityUnavailableException -> {
                "Sign-in isn't available right now. Please try again."
            }

            is InvalidAuthInputException -> {
                t.userMessage
            }

            is FirebaseAuthWeakPasswordException -> {
                "Choose a password with at least 6 characters."
            }

            is FirebaseAuthInvalidCredentialsException -> {
                "That email or password looks incorrect."
            }

            is FirebaseAuthUserCollisionException -> {
                "An account already exists for that email. Try signing in instead."
            }

            is FirebaseAuthInvalidUserException -> {
                "We couldn't find an account for that email."
            }

            is FirebaseNetworkException -> {
                "No connection. Check your network and try again."
            }

            is FirebaseAuthException -> {
                when (t.errorCode) {
                    "ERROR_TOO_MANY_REQUESTS" -> "Too many attempts. Please wait a moment and try again."
                    "ERROR_OPERATION_NOT_ALLOWED" -> "This sign-in method is not enabled yet."
                    else -> "That sign-in didn't work. Please check your details and try again."
                }
            }

            else -> {
                "Something went wrong. Please try again."
            }
        }
}

class FirebaseUnavailableException : IllegalStateException("Firebase is not configured")

class AuthActivityUnavailableException : IllegalStateException("Sign-in activity is unavailable")

class InvalidAuthInputException(
    val userMessage: String,
) : IllegalArgumentException(userMessage)
