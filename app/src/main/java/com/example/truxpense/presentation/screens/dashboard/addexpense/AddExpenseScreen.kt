package com.example.truxpense.presentation.screens.dashboard.addexpense

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.truxpense.R


private val ColorSurface = Color(0xFF1E2128)
private val ColorDivider = Color(0xFF2A2E38)
private val ColorTextPrimary = Color(0xFFF0F2F5)
private val ColorTextSecondary = Color(0xFF7A8599)
private val ColorTextMuted = Color(0xFF4A5568)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreen(
    amount: String = "₹5000",
    category: String = "Food",
    merchant: String = "Swiggy",
    account: String = "HDFC Bank",
    notes: String = "",
    onNotesChange: (String) -> Unit = {},
    onSave: () -> Unit = {},
    onCancel: () -> Unit = {},
    onBack: () -> Unit = {},
    onDatePick: () -> Unit = {},
) {
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
        }
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = 10.dp,
                    end = 10.dp,
                    top = contentPadding.calculateTopPadding(),
                    bottom = contentPadding.calculateBottomPadding()
                )
        ) {
            // ── Amount ───────────────────────────────────────────────────────────
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, bottom = 28.dp)
            ) {
                Text(
                    text = "Amount",
                    color = MaterialTheme.colorScheme.secondary,
                    fontSize = 13.sp,
                    letterSpacing = 0.4.sp,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = amount,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-1).sp,
                )
            }

            // ── Details card ─────────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
            ) {
                DetailsRow(label = "Category", value = category)
                RowDivider()
                DetailsRow(label = "Merchant", value = merchant)
                RowDivider()
                DetailsRow(label = "Account", value = account)
                RowDivider()
                DetailsRow(
                    label = "Date",
                    value = null,
                    trailingContent = {
                        Icon(
                            painter = painterResource(id = R.drawable.calender),
                            contentDescription = "Pick date",
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier
                                .size(20.dp)
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() }
                                ) { onDatePick() }
                        )
                    }
                )
            }

            Spacer(Modifier.height(12.dp))

            // ── Notes ─────────────────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 12.dp, vertical = 14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(id = R.drawable.add_notes_icon),
                        contentDescription = "Notes",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Notes",
                        color = MaterialTheme.colorScheme.secondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
                Spacer(Modifier.height(10.dp))
                BasicTextField(
                    value = notes,
                    onValueChange = onNotesChange,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = LocalTextStyle.current.copy(
                        color = ColorTextPrimary,
                        fontSize = 14.sp,
                    ),
                    decorationBox = { inner ->
                        if (notes.isEmpty()) {
                            Text(
                                "Add a few notes to help you later",
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontSize = 13.sp,
                            )
                        }
                        inner()
                    }
                )
            }

            Spacer(Modifier.weight(1f))

            // ── Action buttons ────────────────────────────────────────────────────
            Column(
                modifier = Modifier,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Save
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(color = MaterialTheme.colorScheme.primary)
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { onSave() }
                ) {
                    Text(
                        text = "Save expense",
                        color = MaterialTheme.colorScheme.background,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                // Cancel
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, ColorDivider, RoundedCornerShape(14.dp))
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { onCancel() }
                ) {
                    Text(
                        text = "Cancel",
                        color = ColorTextSecondary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

        } // end root Column (innerPadding applied)
    }
}


@Composable
private fun DetailsRow(
    label: String,
    value: String?,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {}
            .padding(horizontal = 12.dp, vertical = 16.dp)
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.secondary,
            fontSize = 13.sp,
            modifier = Modifier.weight(1f)
        )
        if (value != null) {
            Text(
                text = value,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.End,
            )
        }
        trailingContent?.invoke()
    }
}

@Composable
private fun RowDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 12.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.onBackground.copy(0.15f),
    )
}


@Preview(showBackground = true)
@Composable
fun AddExpensePreview() {
    AddExpenseScreen()
}
