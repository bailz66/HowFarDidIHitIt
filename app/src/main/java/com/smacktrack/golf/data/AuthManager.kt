package com.smacktrack.golf.data

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

class AuthManager(private val activity: Activity) {

    private val firebaseAuth = FirebaseAuth.getInstance()
    private val credentialManager = CredentialManager.create(activity)

    private val _currentUser = MutableStateFlow(firebaseAuth.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _signInError = MutableStateFlow<String?>(null)
    val signInError: StateFlow<String?> = _signInError.asStateFlow()

    val isSignedIn: Boolean get() = firebaseAuth.currentUser != null

    private val authStateListener = FirebaseAuth.AuthStateListener { auth ->
        _currentUser.value = auth.currentUser
    }

    init {
        firebaseAuth.addAuthStateListener(authStateListener)
    }

    fun cleanup() {
        firebaseAuth.removeAuthStateListener(authStateListener)
    }

    fun clearError() {
        _signInError.value = null
    }

    suspend fun signInWithGoogle(webClientId: String): Result<FirebaseUser> {
        _signInError.value = null
        return try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(webClientId)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(activity, request)
            val credential = result.credential

            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val firebaseCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                val authResult = firebaseAuth.signInWithCredential(firebaseCredential).await()
                val user = authResult.user
                    ?: return Result.failure(Exception("Sign-in succeeded but user is null"))
                Result.success(user)
            } else {
                val err = "Unexpected credential type"
                _signInError.value = err
                Result.failure(Exception(err))
            }
        } catch (e: androidx.credentials.exceptions.NoCredentialException) {
            val msg = "No Google account found. Add a Google account in device Settings first."
            Log.e("AuthManager", msg, e)
            _signInError.value = msg
            Result.failure(e)
        } catch (e: Exception) {
            Log.e("AuthManager", "Google sign-in failed", e)
            _signInError.value = "Sign-in failed: ${e.message}"
            Result.failure(e)
        }
    }

    suspend fun signOut() {
        try {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
            Log.e("AuthManager", "Failed to clear credential state", e)
        }
        firebaseAuth.signOut()
    }
}
