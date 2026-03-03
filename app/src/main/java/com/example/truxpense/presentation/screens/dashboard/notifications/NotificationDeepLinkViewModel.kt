package com.example.truxpense.presentation.screens.dashboard.notifications

import androidx.lifecycle.ViewModel
import com.example.truxpense.notification.deeplink.NotificationDeepLinkManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject


@HiltViewModel
class NotificationDeepLinkViewModel @Inject constructor(
    val manager: NotificationDeepLinkManager,
) : ViewModel() {

    val pendingDeepLink get() = manager.pendingDeepLink

    fun consume() = manager.consume()
}