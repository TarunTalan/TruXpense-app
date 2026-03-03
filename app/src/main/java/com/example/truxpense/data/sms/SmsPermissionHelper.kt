package com.example.truxpense.data.sms

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

object SmsPermissionHelper {

    val RECEIVE_PERMISSIONS = arrayOf(Manifest.permission.RECEIVE_SMS)
    val READ_PERMISSIONS = arrayOf(Manifest.permission.READ_SMS)

    val ALL_SMS_PERMISSIONS = arrayOf(
        Manifest.permission.RECEIVE_SMS,
        Manifest.permission.READ_SMS
    )

    fun hasReceiveSms(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) ==
                PackageManager.PERMISSION_GRANTED

    fun hasReadSms(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) ==
                PackageManager.PERMISSION_GRANTED

    fun hasAllSmsPermissions(context: Context): Boolean =
        hasReceiveSms(context) && hasReadSms(context)

    const val RATIONALE = "TruXpense reads your bank SMS messages to automatically detect and categorise your expenses. Your messages never leave your device."
}

