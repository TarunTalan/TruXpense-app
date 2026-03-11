package com.example.truxpense.presentation.screens.dashboard.sms

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.truxpense.data.sms.model.ParsedTransaction
import com.example.truxpense.data.sms.model.TxnType
import com.example.truxpense.presentation.screens.dashboard.components.ScreenTopBar
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PendingTransactionsScreen(
    onBack: () -> Unit,
    vm: PendingTransactionsViewModel = hiltViewModel()
) {
    val transactions by vm.pendingTransactions.collectAsState()
    val count by vm.pendingCount.collectAsState()

    Scaffold(
        topBar = {
            ScreenTopBar(
                headerTitle = if (count > 0) "Pending Transactions, $count awaiting review" else "Pending Transactions",
                showBack = true,
                onBack = onBack,
                actions = {
                    if (transactions.isNotEmpty()) {
                        TextButton(onClick = { vm.confirmAll() }) {
                            Text("Confirm All", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (transactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🎉", fontSize = 48.sp)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "All caught up!",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "No pending transactions to review.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(transactions, key = { it.id }) { txn ->
                    PendingTransactionCard(
                        transaction = txn,
                        onConfirm = { vm.confirm(txn.id) },
                        onReject = { vm.reject(txn.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PendingTransactionCard(
    transaction: ParsedTransaction,
    onConfirm: () -> Unit,
    onReject: () -> Unit
) {
    val isDebit = transaction.type == TxnType.DEBIT
    val amountColor = if (isDebit) MaterialTheme.colorScheme.error
    else MaterialTheme.colorScheme.tertiary
    val amountSign = if (isDebit) "-" else "+"

    val currencyFmt = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-IN")).apply {
        maximumFractionDigits = 2
    }
    val dateFmt = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // ── Header row ────────────────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Category emoji circle
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(transaction.category.emoji, fontSize = 20.sp)
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = transaction.merchant ?: transaction.bank,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${transaction.category.label}  •  ${dateFmt.format(Date(transaction.timestamp))}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = "$amountSign${currencyFmt.format(transaction.amount)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = amountColor
                )
            }

            // ── Bank / confidence row ─────────────────────────────────────
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "🏦 ${transaction.bank}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f)
                )
                val pct = (transaction.confidence * 100).toInt()
                Text(
                    text = "Confidence: $pct%",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (pct >= 80) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.secondary
                )
            }

            // ── Action buttons ────────────────────────────────────────────
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onReject,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Ignore")
                }
                Button(
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Confirm")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PendingTransactionsContent(
    transactions: List<ParsedTransaction>,
    count: Int,
    onBack: () -> Unit,
    onConfirmAll: (() -> Unit)? = null,
    onConfirm: (String) -> Unit,
    onReject: (String) -> Unit,
) {
    Scaffold(
        topBar = {
            ScreenTopBar(
                headerTitle = "Pending Transactions",
                showBack = true,
                onBack = onBack,
                actions = {
                    if (transactions.isNotEmpty() && onConfirmAll != null) {
                        TextButton(onClick = { onConfirmAll() }) {
                            Text("Confirm All", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (transactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🎉", fontSize = 48.sp)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "All caught up!",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "No pending transactions to review.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(transactions, key = { it.id }) { txn ->
                    PendingTransactionCard(
                        transaction = txn,
                        onConfirm = { onConfirm(txn.id) },
                        onReject = { onReject(txn.id) }
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun PendingTransactionsScreenPreview() {
    // Use the stateless content with fake data so preview doesn't try to create a ViewModel
    val sample = listOf(
        ParsedTransaction(
            id = "1",
            amount = 249.0,
            type = TxnType.DEBIT,
            merchant = "Coffee Shop",
            category = com.example.truxpense.data.sms.model.Category.FOOD,
            confidence = 0.95f,
            balance = null,
            accountLast4 = null,
            bank = "HDFC",
            rawSms = "TXN of INR 249 at Coffee Shop",
            timestamp = System.currentTimeMillis()
        ),
        ParsedTransaction(
            id = "2",
            amount = 50000.0,
            type = TxnType.CREDIT,
            merchant = "Salary",
            category = com.example.truxpense.data.sms.model.Category.SALARY,
            confidence = 0.99f,
            balance = null,
            accountLast4 = null,
            bank = "SBI",
            rawSms = "Salary credited",
            timestamp = System.currentTimeMillis() - 86_400_000L,
        )
    )

    PendingTransactionsContent(
        transactions = sample,
        count = sample.size,
        onBack = {},
        onConfirmAll = {},
        onConfirm = {},
        onReject = {}
    )
}
