package com.example.truxpense.presentation.screens.dashboard.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.truxpense.R
import com.example.truxpense.presentation.screens.dashboard.components.ScreenTopBar
import androidx.compose.ui.tooling.preview.Preview

private data class FaqItem(val question: String, val answer: String)

private val FAQ = listOf(
    FaqItem(
        "How does TruXpense read my bank SMS?",
        "TruXpense uses Android's READ_SMS permission to scan messages from known bank sender IDs. It never stores the raw SMS — only the parsed transaction data."
    ),
    FaqItem(
        "Is my financial data safe?",
        "All data is encrypted at rest and in transit. TruXpense never shares your data with third parties."
    ),
    FaqItem(
        "Why is a transaction categorised incorrectly?",
        "You can manually re-categorise any transaction by tapping it on the Transactions screen. Your correction also trains the model for future SMS."
    ),
    FaqItem(
        "How do I set a budget?",
        "Go to the Budgets tab and tap '+ New Budget'. Choose a category, set an amount, and the app will alert you as you approach the limit."
    ),
    FaqItem(
        "Can I export my data?",
        "Yes — head to Transactions → ⋮ menu → Export CSV. The file lands in your Downloads folder."
    ),
    FaqItem(
        "How do I delete my account?",
        "Go to Settings → Delete account. This permanently removes all your data from our servers."
    ),
)

@Composable
fun HelpSupportScreen(
    onBack: () -> Unit = {},
    onEmailSupport: () -> Unit = {},
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { ScreenTopBar(headerTitle = "Help & Support", showBack = true, onBack = onBack) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // FAQ section
            item {
                Text(
                    text = "Frequently Asked Questions",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                )
            }
            items(FAQ) { faq ->
                FaqCard(faq)
            }

            // Contact
            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Still need help?",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                )
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onEmailSupport)
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.email_icon),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Email Support",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                            Text(
                                text = "support@truxpense.app",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FaqCard(faq: FaqItem) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = faq.question,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    painter = painterResource(R.drawable.drop_down_icon),
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(16.dp)
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    Text(
                        text = faq.answer,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun HelpSupportScreenPreview() {
    MaterialTheme {
        HelpSupportScreen()
    }
}
