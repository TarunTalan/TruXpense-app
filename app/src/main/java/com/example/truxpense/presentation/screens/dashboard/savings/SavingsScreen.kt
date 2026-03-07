package com.example.truxpense.presentation.screens.dashboard.savings

import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toDrawable
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.truxpense.presentation.theme.DashboardDimens
import com.example.truxpense.presentation.theme.TruXpenseTheme
import java.text.NumberFormat
import java.util.*

private val SavingsBlue = Color(0xFF1976D2)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsScreen(
    onBack: () -> Unit = {},
    currencyCode: String = "INR",
    vm: SavingsViewModel = hiltViewModel(),
) {
    val monthGroups by vm.monthGroups.collectAsState()
    val totalSavings by vm.totalSavings.collectAsState()
    val isLoaded by vm.isLoaded.collectAsState()
    val isEditMode by vm.isEditMode.collectAsState()
    var showAddSheet by remember { mutableStateOf(false) }
    var deleteTargetId by remember { mutableStateOf<String?>(null) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val fmt = remember(currencyCode) {
        try { NumberFormat.getCurrencyInstance().apply { currency = Currency.getInstance(currencyCode) } }
        catch (_: Exception) { NumberFormat.getCurrencyInstance() }
    }

    // Open sheet when entering edit mode
    LaunchedEffect(isEditMode) { if (isEditMode) showAddSheet = true }
    LaunchedEffect(Unit) { vm.saveComplete.collect { showAddSheet = false } }

    AnimatedVisibility(visible = isLoaded, enter = fadeIn(tween(220))) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Savings", fontWeight = FontWeight.SemiBold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { vm.resetForm(); showAddSheet = true }) {
                            Icon(Icons.Default.Add, "Add Saving")
                        }
                    },
                )
            },
        ) { pad ->
            if (monthGroups.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🏦", fontSize = 48.sp)
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "No savings recorded yet",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = { vm.resetForm(); showAddSheet = true },
                            colors = ButtonDefaults.buttonColors(containerColor = SavingsBlue),
                        ) { Text("Add first saving", color = Color.White) }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(pad),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // Summary card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(DashboardDimens.cornerCard),
                            colors = CardDefaults.cardColors(containerColor = SavingsBlue.copy(alpha = 0.10f)),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column {
                                    Text("Total saved", fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = fmt.format(totalSavings),
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SavingsBlue,
                                    )
                                }
                                Text(
                                    text = "${monthGroups.sumOf { it.items.size }} entries",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }

                    monthGroups.forEach { group ->
                        item(key = "hdr_${group.monthLabel}") {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(group.monthLabel, fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(fmt.format(group.total), fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold, color = SavingsBlue)
                            }
                        }
                        items(group.items, key = { it.id }) { item ->
                            SavingsRowCard(
                                item = item,
                                onEdit = { vm.loadForEdit(item.id) },
                                onDelete = { deleteTargetId = item.id },
                                fmt = fmt,
                            )
                        }
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }

    // Delete confirmation
    deleteTargetId?.let { id ->
        AlertDialog(
            onDismissRequest = { deleteTargetId = null },
            title = { Text("Delete saving?") },
            text = { Text("This will permanently remove this savings entry.") },
            confirmButton = {
                TextButton(onClick = { vm.delete(id); deleteTargetId = null }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTargetId = null }) { Text("Cancel") }
            },
        )
    }

    // Add / Edit sheet
    if (showAddSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAddSheet = false; vm.resetForm() },
            sheetState = sheetState,
        ) {
            AddSavingsSheetContent(vm = vm, onDismiss = { showAddSheet = false; vm.resetForm() })
        }
    }
}

@Composable
private fun SavingsRowCard(
    item: SavingsListItem,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    fmt: NumberFormat,
) {
    var showMenu by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onEdit() },
        shape = RoundedCornerShape(DashboardDimens.cornerCard),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier.size(42.dp).clip(CircleShape).background(SavingsBlue.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(item.goalName.take(1).uppercase(), fontSize = 16.sp,
                    fontWeight = FontWeight.Bold, color = SavingsBlue)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(item.goalName, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground)
                if (item.notes.isNotBlank()) {
                    Text(item.notes, fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(item.dateLabel, fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("+${fmt.format(item.amount)}", fontSize = 15.sp,
                    fontWeight = FontWeight.Bold, color = SavingsBlue)
                Box {
                    IconButton(onClick = { showMenu = true }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.MoreVert, null, tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp))
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            leadingIcon = { Icon(Icons.Default.Edit, null) },
                            onClick = { showMenu = false; onEdit() },
                        )
                        DropdownMenuItem(
                            text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                            onClick = { showMenu = false; onDelete() },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddSavingsSheetContent(vm: SavingsViewModel, onDismiss: () -> Unit) {
    val rawAmount    by vm.rawAmount.collectAsState()
    val formatted    by vm.formattedAmount.collectAsState()
    val selectedGoal by vm.selectedGoal.collectAsState()
    val notes        by vm.notes.collectAsState()
    val selectedDate by vm.selectedDate.collectAsState()
    val isFormValid  by vm.isFormValid.collectAsState()
    val isSaving     by vm.isSaving.collectAsState()
    val isEditMode   by vm.isEditMode.collectAsState()

    val context = LocalContext.current
    val surfaceArgb      = MaterialTheme.colorScheme.surface.toArgb()
    val onPrimaryArgb    = MaterialTheme.colorScheme.onPrimary.toArgb()
    val onBackgroundArgb = MaterialTheme.colorScheme.onBackground.toArgb()

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (isEditMode) "Edit saving" else "Add saving",
                fontSize = 18.sp, fontWeight = FontWeight.Bold,
            )
            IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Close") }
        }

        // Amount display
        Box(
            modifier = Modifier.fillMaxWidth().background(SavingsBlue.copy(alpha = 0.08f), RoundedCornerShape(12.dp)).padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(formatted, fontSize = 32.sp, fontWeight = FontWeight.Black, color = SavingsBlue)
        }

        // Amount input
        OutlinedTextField(
            value = rawAmount, onValueChange = { vm.setRawAmount(it) },
            label = { Text("Amount") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true,
        )

        // Goal dropdown
        var expanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = selectedGoal, onValueChange = {}, readOnly = true,
                label = { Text("Savings goal") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                shape = RoundedCornerShape(12.dp),
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                vm.goalOptions.forEach { opt ->
                    DropdownMenuItem(text = { Text(opt) }, onClick = { vm.setGoal(opt); expanded = false })
                }
            }
        }

        // Date picker
        OutlinedTextField(
            value = selectedDate, onValueChange = {}, readOnly = true,
            label = { Text("Date") },
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
            trailingIcon = {
                TextButton(onClick = {
                    val now = Calendar.getInstance()
                    DatePickerDialog(
                        context,
                        { _, y, m, d ->
                            val cal = Calendar.getInstance().apply { set(y, m, d) }
                            vm.setDateFromTimestamp(cal.timeInMillis)
                        },
                        now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH),
                    ).apply {
                        setOnShowListener {
                            try {
                                window?.setBackgroundDrawable(surfaceArgb.toDrawable())
                                getButton(DatePickerDialog.BUTTON_POSITIVE)?.setTextColor(onPrimaryArgb)
                                getButton(DatePickerDialog.BUTTON_NEGATIVE)?.setTextColor(onBackgroundArgb)
                            } catch (_: Exception) {}
                        }
                    }.show()
                }) { Text("Change") }
            },
        )

        // Notes
        OutlinedTextField(
            value = notes, onValueChange = { vm.setNotes(it) },
            label = { Text("Notes (optional)") },
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), maxLines = 3,
        )

        Button(
            onClick = { vm.save() },
            enabled = isFormValid && !isSaving,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(DashboardDimens.cornerCard),
            colors = ButtonDefaults.buttonColors(containerColor = SavingsBlue),
        ) {
            if (isSaving) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
            } else {
                Text(
                    if (isEditMode) "Save changes" else "Add saving",
                    fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color.White,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SavingsRowPreview() {
    TruXpenseTheme {
        SavingsRowCard(
            item = SavingsListItem("1", "Emergency Fund", 10000.0, "3 months fund",
                "Mar 1", 0L),
            onEdit = {}, onDelete = {},
            fmt = NumberFormat.getCurrencyInstance(),
        )
    }
}

