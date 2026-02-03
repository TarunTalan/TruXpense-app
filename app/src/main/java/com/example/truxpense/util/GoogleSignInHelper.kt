package com.example.truxpense.util

// Short helper for Google Sign-In

import android.app.Activity
import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.tasks.Task

class GoogleSignInHelper(context: Context, serverClientId: String) {
    private val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken(serverClientId)
        .requestEmail()
        .build()
    private val client: GoogleSignInClient = GoogleSignIn.getClient(context, gso)

    fun getClient(): GoogleSignInClient = client

    fun getSignedInAccountFromIntent(data: android.content.Intent?): Task<GoogleSignInAccount> {
        return GoogleSignIn.getSignedInAccountFromIntent(data)
    }

    fun signOut(activity: Activity, onComplete: () -> Unit) {
        client.signOut().addOnCompleteListener(activity) { onComplete() }
    }
}
