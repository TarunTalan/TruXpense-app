package com.example.truxpense.presentation.screens.dashboard.budget

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.truxpense.R

@Composable
fun BudgetsEmptyScreen(
    modifier: Modifier = Modifier,
    onAddBudget: () -> Unit = {}
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background, topBar = { BudgetTopBar() }) { _ ->

        Column(
            modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        ) {

            // ── Centered body ─────────────────────────────────────────────
            Column(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.budget_illustration),
                    contentDescription = "Budget illustration",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth().fillMaxHeight(0.45f)
                )

                Spacer(Modifier.height(24.dp))

                Text(
                    text = "Create budgets to stay in control",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "Set monthly limits for categories like Food, Travel, or Bills to track and manage your spending.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            // ── Bottom actions ────────────────────────────────────────────
            Button(
                onClick = onAddBudget,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("+ Add your first budget")
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = "You can edit or remove budgets anytime",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun BudgetsEmptyScreenPreview() {
    MaterialTheme {
        BudgetsEmptyScreen()
    }
}