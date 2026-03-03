package com.example.truxpense.presentation.utils

import com.google.android.gms.tasks.Task
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

// Small suspend extension to await a Google Task without depending on external libraries
suspend fun <T> Task<T>.awaitTask(): T? = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { result ->
        cont.resume(result)
    }
    addOnFailureListener { exception ->
        cont.resumeWithException(exception)
    }
    addOnCanceledListener {
        cont.cancel()
    }
}
