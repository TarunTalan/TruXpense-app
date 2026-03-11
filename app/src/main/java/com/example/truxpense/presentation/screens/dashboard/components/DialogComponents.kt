package com.example.truxpense.presentation.screens.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.truxpense.R
import com.example.truxpense.presentation.theme.AppDialogTheme
import com.example.truxpense.presentation.theme.DashboardDimens

/**
 * App-wide themed DatePickerDialog.
 * OK  → primary colour   |   Cancel → onSurfaceVariant
 * Consistent across light and dark themes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDatePickerDialog(
    state: DatePickerState,
    onDismiss: () -> Unit,
    onConfirm: (selectedMillis: Long?) -> Unit,
) {
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(state.selectedDateMillis) }) {
                Text(
                    "OK",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    "Cancel",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    ) {
        DatePicker(state = state)
    }
}

/**
 * App-wide themed TimePickerDialog (AlertDialog wrapping Material3 TimePicker).
 * Dial colours, input-box colours, AM/PM colours all follow the app theme.
 * OK  → primary colour   |   Cancel → onSurfaceVariant
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTimePickerDialog(
    state: TimePickerState,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    title: String = "Select time",
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                title,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            TimePicker(
                state = state,
                colors = TimePickerDefaults.colors(
                    // clock dial face background
                    clockDialColor = MaterialTheme.colorScheme.surfaceContainer,
                    // selected number on dial
                    clockDialSelectedContentColor = MaterialTheme.colorScheme.onPrimary,
                    // unselected numbers on dial
                    clockDialUnselectedContentColor = MaterialTheme.colorScheme.onBackground,
                    // sweep arc + centre dot
                    selectorColor = MaterialTheme.colorScheme.primary,
                    // AM / PM toggle
                    periodSelectorBorderColor = MaterialTheme.colorScheme.outline,
                    periodSelectorSelectedContainerColor = MaterialTheme.colorScheme.primary,
                    periodSelectorUnselectedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    periodSelectorSelectedContentColor = MaterialTheme.colorScheme.onPrimary,
                    periodSelectorUnselectedContentColor = MaterialTheme.colorScheme.onBackground,
                    // hour / minute input boxes
                    timeSelectorSelectedContainerColor = MaterialTheme.colorScheme.primary,
                    timeSelectorUnselectedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    timeSelectorSelectedContentColor = MaterialTheme.colorScheme.onPrimary,
                    timeSelectorUnselectedContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    "OK",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    "Cancel",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    )
}

/**
 * App-wide confirm/delete dialog matching the BudgetDetailScreen delete dialog style.
 *
 * @param iconRes       Optional drawable resource for the icon (defaults to delete icon).
 *                      Pass null to hide the icon.
 * @param title         Dialog title text.
 * @param message       Dialog body text.
 * @param confirmLabel  Label for the destructive confirm button (default "Delete").
 * @param cancelLabel   Label for the dismiss button (default "Cancel").
 * @param isDestructive When true the confirm button uses error colour; false uses primary.
 * @param onConfirm     Called when user taps the confirm button.
 * @param onDismiss     Called when user taps Cancel or dismisses.
 */
@Composable
fun AppConfirmDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmLabel: String = "Delete",
    cancelLabel: String = "Cancel",
    isDestructive: Boolean = true,
    iconRes: Int? = R.drawable.delete,
) {
    AppDialogTheme {
        AlertDialog(
            onDismissRequest = onDismiss,
            icon = if (iconRes != null) {
                {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(iconRes),
                            contentDescription = null,
                            tint = if (isDestructive) MaterialTheme.colorScheme.onErrorContainer
                                   else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(DashboardDimens.iconLg),
                        )
                    }
                }
            } else null,
            title = {
                Text(
                    text = title,
                    fontWeight = FontWeight.SemiBold,
                )
            },
            text = {
                Text(text = message)
            },
            confirmButton = {
                Button(
                    onClick = onConfirm,
                    modifier = Modifier.height(DashboardDimens.buttonHeight),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDestructive) MaterialTheme.colorScheme.error
                                         else MaterialTheme.colorScheme.primary,
                        contentColor = if (isDestructive) MaterialTheme.colorScheme.onError
                                       else MaterialTheme.colorScheme.onPrimary,
                    ),
                    elevation = ButtonDefaults.buttonElevation(0.dp),
                ) {
                    Text(text = confirmLabel, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.height(DashboardDimens.buttonHeight),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Text(text = cancelLabel, fontWeight = FontWeight.Medium)
                }
            },
        )
    }
}
