package com.tecsup.autosmart.data.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.tecsup.autosmart.BuildConfig


sealed class GoogleResultado {
    data class Token(val idToken: String) : GoogleResultado()
    object Cancelado : GoogleResultado()
    data class Error(val mensaje: String) : GoogleResultado()
}

object GoogleAuthManager {
    suspend fun obtenerIdToken(context: Context): GoogleResultado {
        val manager = CredentialManager.create(context)
        val opcion = GetSignInWithGoogleOption.Builder(BuildConfig.GOOGLE_WEB_CLIENT_ID).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(opcion).build()
        return try {
            val credencial = manager.getCredential(context, request).credential
            if (credencial is CustomCredential &&
                credencial.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                GoogleResultado.Token(GoogleIdTokenCredential.createFrom(credencial.data).idToken)
            } else {
                GoogleResultado.Error("No se pudo leer la cuenta de Google")
            }
        } catch (e: GetCredentialCancellationException) {
            GoogleResultado.Cancelado
        } catch (e: NoCredentialException) {
            GoogleResultado.Error("No hay una cuenta de Google en este dispositivo")
        } catch (e: GetCredentialException) {
            GoogleResultado.Error("No se pudo iniciar sesión con Google")
        }
    }
}