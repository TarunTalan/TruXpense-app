package com.example.truxpense.notification

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject


@HiltViewModel
class NotificationDeepLinkViewModel @Inject constructor(
    val manager: NotificationDeepLinkManager,
) : ViewModel() {

    val pendingDeepLink get() = manager.pendingDeepLink

    fun consume() = manager.consume()
}