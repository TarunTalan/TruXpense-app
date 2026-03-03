package com.example.truxpense.presentation.screens.dashboard.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.truxpense.presentation.theme.DashboardDimens
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDropdown(
    selected: String?,
    accounts: List<String>,
    onSelect: (String) -> Unit,
    iconForAccount: (String) -> Int,
    modifier: Modifier = Modifier,
    placeholder: String = "Select account",
    inline: Boolean = false,
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()

    // Internal expanded state — the component manages its own sheet visibility
    var localExpanded by remember { mutableStateOf(false) }

    val dismissDropdown = {
        localExpanded = false
        focusManager.clearFocus()
        keyboardController?.hide()
    }

    // Content row (shared by inline and card variants)
    @Composable
    fun AccountRowContent(isExpanded: Boolean) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = DashboardDimens.cardPaddingComp).height(48.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val iconRes = if (selected != null) iconForAccount(selected)
            else com.example.truxpense.R.drawable.bills

            Icon(
                painter = painterResource(iconRes),
                contentDescription = "account icon",
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(28.dp).padding(end = 10.dp)
            )

            Text(
                text = selected ?: placeholder,
                color = if (selected.isNullOrEmpty()) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelLarge
            )

            IconButton(onClick = { localExpanded = !localExpanded }) {
                Icon(
                    painter = painterResource(com.example.truxpense.R.drawable.drop_down_icon),
                    contentDescription = if (isExpanded) "Close" else "Open",
                )
            }
        }
    }

    // Render either inline row (no header) or the full card-with-label
    if (!inline) {
        // Full card with header label
        Box(
            modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(DashboardDimens.cornerCard))
                .background(MaterialTheme.colorScheme.primaryContainer),
        ) {
            Column(modifier = Modifier.padding(DashboardDimens.cardPaddingComp)) {
                Text(
                    text = "Account",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = DashboardDimens.textLg,
                    fontWeight = MaterialTheme.typography.labelLarge.fontWeight,
                )

                Spacer(Modifier.height(DashboardDimens.spaceMd))

                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.background,
                    modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).clickable {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                            localExpanded = !localExpanded
                        }) {
                    AccountRowContent(localExpanded)
                }
            }
        }
    } else {
        // Inline compact row matching DetailRow structure exactly
        Row(
            modifier = modifier.fillMaxWidth().height(DashboardDimens.detailRowHeight).clickable {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                    localExpanded = !localExpanded
                }.padding(horizontal = DashboardDimens.screenPaddingH),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DashboardDimens.spaceMd),
        ) {
            // Leading icon slot (matching DetailRow)
            Box(modifier = Modifier.size(DashboardDimens.iconMd), contentAlignment = Alignment.Center) {
                val iconRes = if (selected != null) iconForAccount(selected)
                else com.example.truxpense.R.drawable.bills
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = if (selected != null) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(DashboardDimens.iconMd),
                )
            }

            // Label + value (matching DetailRow structure)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "Account",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(DashboardDimens.spaceXxs))
                Text(
                    text = selected ?: placeholder,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (selected != null) FontWeight.Medium else FontWeight.Normal,
                    color = if (selected.isNullOrEmpty()) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onBackground,
                )
            }

            // Trailing chevron (matching DetailRow optional trailing icon)
            Box(modifier = Modifier.size(DashboardDimens.iconMd), contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(com.example.truxpense.R.drawable.drop_down_icon),
                    contentDescription = "Open account",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(DashboardDimens.iconMd),
                )
            }
        }
    }

    // Dropdown list as a bottom sheet overlay (component-local state)
    BackHandler(enabled = localExpanded) { dismissDropdown() }

    if (localExpanded) {
        // Use ModalBottomSheet from Material3 to show overlay list
        val sheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = true, confirmValueChange = { true })

        ModalBottomSheet(
            onDismissRequest = { dismissDropdown() },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = DashboardDimens.spaceMd)
            ) {
                // Optional handle / title
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(text = "Select account", style = MaterialTheme.typography.titleSmall)
                }
                Spacer(Modifier.height(DashboardDimens.spaceSm))

                LazyColumn(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = DashboardDimens.listPaddingH),
                ) {
                    itemsIndexed(accounts) { _, item ->
                        val isSelected = selected == item
                        val containerColor =
                            if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            else MaterialTheme.colorScheme.surface

                        val iconRes = iconForAccount(item)

                        ListItem(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(DashboardDimens.cornerChip))
                                .clickable {
                                    onSelect(item)
                                    // dismiss the sheet after selecting
                                    scope.launch { sheetState.hide() }
                                    dismissDropdown()
                                },
                            colors = ListItemDefaults.colors(containerColor = containerColor),
                            leadingContent = {
                                Icon(
                                    painter = painterResource(iconRes),
                                    contentDescription = "$item icon",
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.size(DashboardDimens.iconMd),
                                )
                            },
                            headlineContent = {
                                Text(
                                    text = item,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                )
                            },
                        )
                        if (accounts.indexOf(item) < accounts.lastIndex) {
                            Spacer(Modifier.height(4.dp))
                        }
                    }
                }
                Spacer(Modifier.height(DashboardDimens.spaceLg))
            }
        }
    }
}