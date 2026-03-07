package com.example.truxpense.presentation.screens.dashboard.settings

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.truxpense.R
import com.example.truxpense.presentation.screens.auth.components.AuthButton
import com.example.truxpense.presentation.screens.auth.components.AuthTextField
import com.example.truxpense.presentation.screens.auth.otp.OtpDigitBox
import com.example.truxpense.presentation.screens.dashboard.components.ScreenTopBar
import com.example.truxpense.presentation.utils.clearFocusOnTap
import kotlinx.coroutines.delay

private const val OTP_LENGTH = 6

@Composable
fun ChangeContactOtpScreen(
    type: ContactType,
    currentContact: String = "",
    newContactInitial: String = "",
    isSendingOtp: Boolean = false,
    otpSent: Boolean = false,
    isVerifying: Boolean = false,
    otpError: String? = null,
    onSendOtp: (newContact: String) -> Unit = {},
    onResendOtp: () -> Unit = {},
    onVerifyOtp: (otp: String) -> Unit = {},
    onBack: () -> Unit = {},
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val colorScheme = MaterialTheme.colorScheme
    val bgColor = colorScheme.background

    // ── State ─────────────────────────────────────────────────────────────────
    var newContact by remember { mutableStateOf(newContactInitial) }
    var contactError by remember { mutableStateOf<String?>(null) }

    val digits = remember { mutableStateListOf(*Array(OTP_LENGTH) { "" }) }
    val focusRequesters = remember { List(OTP_LENGTH) { FocusRequester() } }
    val otpFull by remember { derivedStateOf { digits.all { it.isNotEmpty() } } }

    var resendSeconds by remember { mutableIntStateOf(0) }
    var resendTrigger by remember { mutableIntStateOf(0) }

    LaunchedEffect(otpSent, resendTrigger) {
        if (otpSent) {
            resendSeconds = 60
            while (resendSeconds > 0) {
                delay(1000L); resendSeconds--
            }
        }
    }
    LaunchedEffect(otpSent) {
        if (otpSent) try {
            focusRequesters[0].requestFocus()
        } catch (_: Exception) {
        }
    }

    fun validateContact(): Boolean {
        contactError = when {
            newContact.isBlank() -> if (type == ContactType.EMAIL) "Please enter an email address"
            else "Please enter a phone number"

            newContact.trim().equals(
                currentContact.trim(), ignoreCase = type == ContactType.EMAIL
            ) -> if (type == ContactType.EMAIL) "This is already your current email address"
            else "This is already your current phone number"

            type == ContactType.EMAIL && !android.util.Patterns.EMAIL_ADDRESS.matcher(newContact.trim())
                .matches() -> "Enter a valid email address"

            type == ContactType.PHONE && !newContact.trim()
                .matches(Regex("^[+]?[0-9]{10,13}$")) -> "Enter a valid phone number"

            else -> null
        }
        return contactError == null
    }

    val masked = remember(newContact) {
        if (type == ContactType.EMAIL) {
            val at = newContact.indexOf('@')
            if (at < 2 || newContact.isBlank()) newContact
            else "${newContact[0]}***${newContact.substring(at)}"
        } else {
            if (newContact.length < 6) newContact
            else "${newContact.take(3)}****${newContact.takeLast(3)}"
        }
    }

    val icon: ImageVector = if (type == ContactType.EMAIL) Icons.Outlined.Email else Icons.Outlined.Phone

    Scaffold(
        containerColor = bgColor,
        topBar = {
            ScreenTopBar(
                headerTitle = if (type == ContactType.EMAIL) "Change Email" else "Change Phone",
                showBack = true,
                onBack = onBack,
            )
        },
    ) { scaffoldPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(scaffoldPadding).verticalScroll(rememberScrollState())
                .imePadding()                       // scroll content rises above keyboard
                .navigationBarsPadding().clearFocusOnTap().padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

            Spacer(Modifier.height(32.dp))

            // ── Icon badge ─────────────────────────────────────────────────────
            Box(
                modifier = Modifier.size(80.dp).shadow(
                    elevation = 4.dp,
                    shape = RoundedCornerShape(24.dp),
                    ambientColor = colorScheme.primary.copy(alpha = 0.25f),
                    spotColor = colorScheme.primary.copy(alpha = 0.35f),
                ).clip(RoundedCornerShape(24.dp)).background(
                    brush = Brush.linearGradient(
                        listOf(
                            colorScheme.primary.copy(alpha = 0.15f),
                            colorScheme.primary.copy(alpha = 0.08f),
                        )
                    )
                ).border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        listOf(
                            colorScheme.primary.copy(alpha = 0.4f),
                            colorScheme.primary.copy(alpha = 0.1f),
                        )
                    ),
                    shape = RoundedCornerShape(24.dp),
                ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.email_icon),
                    contentDescription = null,
                    tint = colorScheme.primary,
                    modifier = Modifier.size(36.dp),
                )
            }

            Spacer(Modifier.height(20.dp))

            // ── Title & subtitle (crossfade on step change) ────────────────────
            AnimatedContent(
                targetState = otpSent,
                transitionSpec = {
                    (fadeIn(tween(300)) + slideInVertically { it / 4 }).togetherWith(fadeOut(tween(200)))
                },
                label = "titleAnim",
            ) { isSent ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = if (isSent) "Enter Verification Code"
                        else if (type == ContactType.EMAIL) "Enter New Email"
                        else "Enter New Phone",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colorScheme.onBackground,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            // ── Step content (slides horizontally on transition) ───────────────
            AnimatedContent(
                targetState = otpSent,
                transitionSpec = {
                    if (targetState) {
                        (fadeIn(
                            tween(
                                350, 100
                            )
                        ) + slideInHorizontally { it / 3 }).togetherWith(fadeOut(tween(200)) + slideOutHorizontally { -it / 3 })
                    } else {
                        (fadeIn(
                            tween(
                                350, 100
                            )
                        ) + slideInHorizontally { -it / 3 }).togetherWith(fadeOut(tween(200)) + slideOutHorizontally { it / 3 })
                    }
                },
                label = "stepContent",
            ) { isSent ->
                if (!isSent) {
                    // ── Step 1 ────────────────────────────────────────────────
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        AuthTextField(
                            bgColor = bgColor,
                            label = if (type == ContactType.EMAIL) "New Email Address"
                            else "New Phone Number",
                            placeholder = if (type == ContactType.EMAIL) "example@xyz.com"
                            else "+91 9876543210",
                            value = newContact,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = if (type == ContactType.EMAIL) KeyboardType.Email
                                else KeyboardType.Phone,
                            ),
                            onValueChange = { newContact = it; contactError = null },
                            contentPadding = 4,
                            modifier = Modifier.fillMaxWidth(),
                            error = contactError,
                        )

                        AuthButton(
                            onClick = {
                                if (validateContact()) {
                                    keyboardController?.hide()
                                    onSendOtp(newContact.trim())
                                }
                            },
                            text = if (type == ContactType.EMAIL) "Send OTP" else "Save Phone",
                            enabled = !isSendingOtp,
                            isLoading = isSendingOtp,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                        )

                        InfoPill(
                            text = if (type == ContactType.EMAIL) "A one-time code will be sent to this address"
                            else "Your number will be updated after confirmation",
                        )
                    }
                } else {
                    // ── Step 2 ────────────────────────────────────────────────
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        val hasError = !otpError.isNullOrEmpty()

                        // OTP digit boxes
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                for (i in 0 until OTP_LENGTH) {
                                    val focusedState = remember { mutableStateOf(false) }
                                    OtpDigitBox(
                                        value = digits[i],
                                        index = i,
                                        otpLength = OTP_LENGTH,
                                        isFocused = focusedState.value,
                                        hasError = hasError,
                                        focusRequester = focusRequesters[i],
                                        modifier = Modifier.weight(1f),
                                        onValueChange = { newVal ->
                                            when {
                                                newVal.length > 1 -> {
                                                    val only = newVal.filter(Char::isDigit)
                                                    only.take(OTP_LENGTH - i).forEachIndexed { off, ch ->
                                                        digits[i + off] = ch.toString()
                                                    }
                                                    val last = minOf(i + only.length - 1, OTP_LENGTH - 1)
                                                    if (last < OTP_LENGTH - 1) focusRequesters[last + 1].requestFocus()
                                                    else keyboardController?.hide()
                                                }

                                                newVal.length == 1 && newVal.all(Char::isDigit) -> {
                                                    digits[i] = newVal
                                                    if (i < OTP_LENGTH - 1) focusRequesters[i + 1].requestFocus()
                                                    else keyboardController?.hide()
                                                }

                                                newVal.isEmpty() -> digits[i] = ""
                                            }
                                        },
                                        onBackspace = {
                                            if (digits[i].isEmpty() && i > 0) {
                                                focusRequesters[i - 1].requestFocus()
                                                digits[i - 1] = ""
                                            } else {
                                                digits[i] = ""
                                            }
                                        },
                                        onFocusChanged = { focusedState.value = it },
                                    )
                                }
                            }

                            // Inline error label directly below boxes
                            if (hasError) {
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = otpError!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colorScheme.error,
                                    modifier = Modifier.fillMaxWidth().padding(start = 4.dp),
                                )
                            }
                        }

                        AuthButton(
                            onClick = { if (otpFull) onVerifyOtp(digits.joinToString("")) },
                            text = "Verify & Update",
                            enabled = otpFull && !isVerifying,
                            isLoading = isVerifying,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                        )

                        ResendRow(
                            resendSeconds = resendSeconds,
                            isVerifying = isVerifying,
                            onResend = {
                                if (resendSeconds == 0) {
                                    digits.fill("")
                                    resendTrigger++
                                    onResendOtp()
                                }
                            },
                        )

                        InfoPill(text = "Code expires in 10 minutes. Didn't receive it? Check spam.")
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ── Resend Row ────────────────────────────────────────────────────────────────

@Composable
private fun ResendRow(
    resendSeconds: Int,
    isVerifying: Boolean,
    onResend: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Didn't receive the code? ",
            style = MaterialTheme.typography.bodySmall,
            color = colorScheme.onBackground,
        )
        TextButton(
            onClick = onResend,
            enabled = resendSeconds == 0 && !isVerifying,
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
        ) {
            if (resendSeconds > 0) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = colorScheme.surfaceContainer,
                ) {
                    Text(
                        text = "Resend in ${resendSeconds}s",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = colorScheme.onBackground.copy(alpha = 0.7f),
                    )
                }
            } else {
                Text(
                    text = "Resend OTP",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (!isVerifying) colorScheme.primary
                    else colorScheme.onSurface.copy(alpha = 0.4f),
                )
            }
        }
    }
}

// ── Info Pill ─────────────────────────────────────────────────────────────────

@Composable
private fun InfoPill(text: String) {
    val colorScheme = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = colorScheme.primary.copy(alpha = 0.07f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.info_square),
                contentDescription = null,
                tint = colorScheme.secondary.copy(alpha = 0.8f),
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = text,
                fontSize = 11.sp,
                color = colorScheme.secondary.copy(alpha = 0.8f),
                lineHeight = 18.sp,
            )
        }
    }
}

// ── Previews ─────────────────────────────────────────────────────────────────

@Preview(showBackground = true, showSystemUi = true, name = "Step 1 – Enter new email")
@Composable
private fun ChangeContactStep1Preview() {
    MaterialTheme {
        ChangeContactOtpScreen(type = ContactType.EMAIL, otpSent = false)
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Step 2 – Verify OTP")
@Composable
private fun ChangeContactStep2Preview() {
    MaterialTheme {
        ChangeContactOtpScreen(
            type = ContactType.PHONE,
            newContactInitial = "+919876543210",
            otpSent = true,
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Step 2 – OTP Error")
@Composable
private fun ChangeContactOtpErrorPreview() {
    MaterialTheme {
        ChangeContactOtpScreen(
            type = ContactType.EMAIL,
            newContactInitial = "tushar@gmail.com",
            otpSent = true,
            otpError = "Invalid code. Please try again.",
        )
    }
}