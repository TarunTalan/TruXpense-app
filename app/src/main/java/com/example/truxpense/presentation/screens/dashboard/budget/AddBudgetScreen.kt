package com.example.truxpense.presentation.screens.dashboard.budget


import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.truxpense.R
import com.example.truxpense.presentation.components.NumberField
import com.example.truxpense.presentation.screens.auth.components.AuthTextField
import com.example.truxpense.presentation.utils.clearFocusOnTap

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBudgetScreen(
    onBack: () -> Unit = {},
    onSave: () -> Unit = {},
) {
    // Use BudgetViewModel for categories and addBudget
    val vm: BudgetViewModel = hiltViewModel()
    val categories by vm.categories.collectAsState()

    var amountInput by remember { mutableStateOf("") }
    var query by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var pressedItem by remember { mutableStateOf<String?>(null) }
    var selected by remember { mutableStateOf<String?>(null) }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Add Expense", style = TextStyle(
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Medium,
                            fontSize = 16.sp
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onBack() }) {
                        Icon(
                            painterResource(id = R.drawable.back_icon),
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        bottomBar = {
            if (!expanded) {
                Column(
                    modifier = Modifier.padding(top = 10.dp, start = 16.dp, end = 16.dp, bottom = 34.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val formValid = amountInput.isNotBlank() && selected != null

                    Button(
                        onClick = {
                            val cat = selected ?: return@Button
                            val amt = amountInput.toDoubleOrNull() ?: 0.0
                            vm.addBudget(cat, amt)
                            onSave()
                        },
                        enabled = formValid,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(0.5f)
                        )
                    ) {
                        Text(
                            text = "Create budget",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    ) { contentPadding ->
        val bottomPad = if (expanded) 0.dp else contentPadding.calculateBottomPadding()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .clearFocusOnTap()
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = contentPadding.calculateTopPadding(),
                    bottom = bottomPad
                ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {

            // Category Section (selectable dropdown, mirrors CurrencyScreen behavior)
            SectionCard {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Category",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    Spacer(Modifier.height(8.dp))

                    // Make the whole input tappable to open dropdown (wrap AuthTextField in clickable Box)
                    val rotation by animateFloatAsState(targetValue = if (expanded) 180f else 0f)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .clickable {
                                keyboardController?.hide()
                                focusManager.clearFocus()
                                expanded = true
                            }
                    ) {
                        AuthTextField(
                            bgColor = MaterialTheme.colorScheme.background,
                            label = null,
                            placeholder = "Select or search category",
                            error = null,
                            value = query.ifEmpty { selected ?: "" },
                            onValueChange = { v ->
                                selected = null
                                query = v
                                expanded = true
                            },
                            contentPadding = 12,
                            trailing = {
                                IconButton(onClick = {
                                    keyboardController?.hide()
                                    focusManager.clearFocus()
                                    expanded = !expanded
                                }) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.drop_down_icon),
                                        contentDescription = if (expanded) "Close dropdown" else "Open dropdown",
                                        modifier = Modifier.rotate(rotation)
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Filtered results (computed once)
            val filtered = remember(query, categories) {
                val q = query.trim().lowercase()
                if (q.isEmpty()) categories else categories.filter { it.lowercase().contains(q) }
            }

            // When expanded show a full-height dropdown similar to CurrencyScreen
            BackHandler(enabled = expanded) {
                expanded = false
                focusManager.clearFocus()
                keyboardController?.hide()
            }

            if (expanded) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = MaterialTheme.shapes.medium
                        )
                        .padding(bottom = 10.dp)
                ) {
                    if (filtered.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(MaterialTheme.shapes.medium),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No categories found", color = MaterialTheme.colorScheme.onBackground)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(MaterialTheme.shapes.medium)
                                .padding(horizontal = 8.dp, vertical = 12.dp)
                        ) {
                            itemsIndexed(filtered) { _, item ->
                                val isPressed = pressedItem == item
                                val isSelected = selected == item

                                // containerColor: selected visually overrides pressed
                                val containerColor = when {
                                    isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
                                    isPressed -> MaterialTheme.colorScheme.onPrimaryContainer
                                    else -> MaterialTheme.colorScheme.primaryContainer
                                }

                                val iconRes = when (item.trim().lowercase()) {
                                    "food" -> R.drawable.food
                                    "transport" -> R.drawable.transport
                                    "bills" -> R.drawable.bills
                                    "shopping" -> R.drawable.shopping
                                    "travel" -> R.drawable.category_icon
                                    "health" -> R.drawable.health
                                    "education" -> R.drawable.category_icon
                                    "entertainment" -> R.drawable.entertainment
                                    "groceries" -> R.drawable.groceries
                                    else -> R.drawable.category_icon
                                }

                                ListItem(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(MaterialTheme.shapes.medium)
                                        .clickable(enabled = true, onClickLabel = null, role = null) {
                                            selected = item
                                            query = item
                                            expanded = false
                                            focusManager.clearFocus()
                                            keyboardController?.hide()
                                        },
                                    colors = ListItemDefaults.colors(containerColor = containerColor),
                                    trailingContent = {
                                        Icon(
                                            painter = painterResource(id = iconRes),
                                            contentDescription = "$item icon",
                                            tint = MaterialTheme.colorScheme.onBackground,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    },
                                    headlineContent = {
                                        Text(text = item, color = MaterialTheme.colorScheme.onBackground)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // If dropdown is expanded, hide the rest of the content so dropdown covers full area
            if (!expanded) {
                // ── Monthly Budget Limit Section ──────────────────────────────────
                SectionCard {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Monthly Budget limit",
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.height(12.dp))

                        // Amount input label
                        Text(
                            text = "Enter amount",
                            color = MaterialTheme.colorScheme.secondary,
                            fontSize = 12.sp,
                        )
                        Spacer(Modifier.height(4.dp))

                        // Amount text field (reused component extracted from this screen)
                        NumberField(
                            value = amountInput,
                            onValueChange = { amountInput = it },
                            leadingIcon = {
                                Text(
                                    text = "₹",
                                    color = MaterialTheme.colorScheme.onBackground,
                                    fontSize = 15.sp,
                                )
                            },
                            placeholder = "0",
                            borderColor = MaterialTheme.colorScheme.onSecondary,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "This resets every month",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.sp,
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Column {
                    // Budget period
                    Text(
                        text = "Budget period",
                        color = MaterialTheme.colorScheme.secondary,
                        fontSize = 12.sp,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Monthly (resets on the 1st)",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                Spacer(Modifier.height(12.dp))

                //  Helper text
                Text(
                    text = "Budget help you stay of your spending. You can edit or remove them anytime.",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                )

                Spacer(Modifier.weight(1f))
            }
        }
    }
}


//  Section Card
@Composable
private fun SectionCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
    ) {
        content()
    }
}

// ── Preview ───────────────────────────────────────────────────────────────────
@Preview(
    showBackground = true,
    widthDp = 360,
    heightDp = 800,
    backgroundColor = 0xFFFFFFFF,
)
@Composable
fun AddBudgetScreenPreview() {
    AddBudgetScreen()
}