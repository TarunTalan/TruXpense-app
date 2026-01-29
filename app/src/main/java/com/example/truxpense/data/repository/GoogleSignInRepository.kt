package com.example.truxpense.data.repository

import android.content.Context
import android.content.Intent
import com.example.truxpense.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions

class GoogleSignInRepository(private val context: Context) {
    fun getClient(): GoogleSignInClient {
        val webClientId = context.getString(R.string.default_web_client_id)
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestIdToken(webClientId)
            .build()
        return GoogleSignIn.getClient(context, gso)
    }

    fun getSignInIntent(): Intent {
        return getClient().signInIntent
    }
}