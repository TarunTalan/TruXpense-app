package com.example.truxpense.presentation.screens.dashboard.settings

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.truxpense.R
import com.example.truxpense.presentation.screens.auth.components.AuthButton
import com.example.truxpense.presentation.screens.auth.components.AuthTextField
import com.example.truxpense.presentation.screens.dashboard.components.ScreenTopBar
import com.example.truxpense.presentation.utils.clearFocusOnTap
import android.graphics.Color as AndroidColor

@Composable
fun PersonalInfoScreen(
    initialUsername: String = "",
    initialEmail: String = "",
    initialPhone: String = "",
    isSaving: Boolean = false,
    saveError: String? = null,
    onBack: () -> Unit = {},
    onSave: (name: String) -> Unit = {},
    onChangeEmail: (currentEmail: String) -> Unit = {},
    onChangePhone: (currentPhone: String) -> Unit = {},
) {
    var name by remember { mutableStateOf(initialUsername) }
    var nameError by remember { mutableStateOf<String?>(null) }
    var displayedName by remember { mutableStateOf(initialUsername) }
    var wasSaving by remember { mutableStateOf(false) }

    LaunchedEffect(initialUsername) {
        name = initialUsername
        if (displayedName.isBlank()) displayedName = initialUsername
    }

    LaunchedEffect(isSaving, saveError) {
        if (wasSaving && !isSaving && saveError == null) {
            displayedName = name.trim().ifBlank { displayedName }
        }
        wasSaving = isSaving
    }

    fun validate(): Boolean {
        nameError = if (name.isBlank()) "Name cannot be empty" else null
        return nameError == null
    }

    val avatarBgColor = remember(displayedName) {
        val hue = (displayedName.hashCode() and 0xFFFF) % 360f
        Color(AndroidColor.HSVToColor(floatArrayOf(hue, 0.55f, 0.85f)))
    }
    val avatarContentColor = if (avatarBgColor.luminance() < 0.5f) Color.White else Color.Black
    val initialChar = displayedName.trim().let { if (it.isNotEmpty()) it[0].uppercaseChar().toString() else "?" }

    val colorScheme = MaterialTheme.colorScheme
    val bgColor = colorScheme.background

    Scaffold(
        containerColor = bgColor,
        topBar = {
            ScreenTopBar(
                headerTitle = "Personal Information",
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

            Spacer(Modifier.height(24.dp))

            // ── Avatar ────────────────────────────────────────────────────────
            Box(
                modifier = Modifier.size(88.dp).clip(CircleShape).background(
                    brush = Brush.linearGradient(
                        listOf(avatarBgColor.copy(alpha = 0.8f), avatarBgColor)
                    ),
                ).border(
                    width = 3.dp,
                    brush = Brush.linearGradient(
                        listOf(avatarBgColor.copy(alpha = 0.4f), avatarBgColor)
                    ),
                    shape = CircleShape,
                ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = initialChar,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = avatarContentColor,
                )
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = displayedName.ifBlank { "Your Name" },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = colorScheme.onBackground,
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = "Edit your profile details below",
                style = MaterialTheme.typography.bodySmall,
                color = colorScheme.onSurface,
            )

            Spacer(Modifier.height(28.dp))

            AuthTextField(
                bgColor = bgColor,
                label = "Full Name",
                placeholder = "e.g. Tarun Talan",
                bottomLabel = "This is how you appear in the app.",
                value = name,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                ),
                onValueChange = { name = it; nameError = null },
                contentPadding = 16,
                modifier = Modifier.fillMaxWidth(),
                error = nameError,
            )

            Spacer(Modifier.height(28.dp))

            LockedContactCard(
                icon = Icons.Outlined.Email,
                label = "Email Address",
                value = initialEmail.ifBlank { "Not set" },
                onChangeClick = { onChangeEmail(initialEmail) },
            )

            Spacer(Modifier.height(10.dp))

            LockedContactCard(
                icon = Icons.Outlined.Phone,
                label = "Phone Number",
                value = initialPhone.ifBlank { "Not set" },
                onChangeClick = { onChangePhone(initialPhone) },
            )

            Spacer(Modifier.height(32.dp))

            // ── Error banner ──────────────────────────────────────────────────
            AnimatedVisibility(
                visible = saveError != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                saveError?.let {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = colorScheme.errorContainer,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("⚠", fontSize = 13.sp, color = colorScheme.onErrorContainer)
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = colorScheme.onErrorContainer,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }

            // ── Save button — inline, directly below form ─────────────────────
            AuthButton(
                onClick = { if (validate()) onSave(name.trim()) },
                text = "Save Changes",
                enabled = !isSaving,
                isLoading = isSaving,
                modifier = Modifier.fillMaxWidth().height(48.dp),
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}


// ── Locked Contact Card ───────────────────────────────────────────────────────

@Composable
private fun LockedContactCard(
    icon: ImageVector,
    label: String,
    value: String,
    onChangeClick: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = colorScheme.surfaceContainer.copy(alpha = 0.8f),
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier.size(40.dp).background(
                    color = colorScheme.primary.copy(alpha = 0.1f),
                    shape = CircleShape,
                ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = colorScheme.secondary,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = colorScheme.onBackground.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                FilledTonalIconButton(
                    onClick = onChangeClick,
                    modifier = Modifier.size(34.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = colorScheme.primary.copy(alpha = 0.12f),
                        contentColor = colorScheme.primary,
                    ),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.right_arrow),
                        contentDescription = "Change $label",
                        modifier = Modifier.size(16.dp),
                    )
                }
                Row(
                    modifier = Modifier.background(
                        color = colorScheme.primary.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(4.dp),
                    ).padding(horizontal = 5.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(9.dp),
                    )
                    Text(
                        text = "OTP",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        color = colorScheme.onBackground.copy(alpha = 0.8f),
                    )
                }
            }
        }
    }
}

// ── Previews ─────────────────────────────────────────────────────────────────

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PersonalInfoPreview() {
    MaterialTheme {
        PersonalInfoScreen(
            initialUsername = "Tushar Talan",
            initialEmail = "tushar@gmail.com",
            initialPhone = "+91 9876543210",
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Empty State")
@Composable
fun PersonalInfoEmptyPreview() {
    MaterialTheme {
        PersonalInfoScreen()
    }
}