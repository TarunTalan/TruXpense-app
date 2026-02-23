package com.example.truxpense.presentation.screens.dashboard.analytic

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
import com.example.truxpense.presentation.screens.dashboard.components.ScreenTopBar

@Composable
fun AnalyticsTopBar() {
    ScreenTopBar(title = "Analytics", showBack = false)
}

@Composable
fun AnalyticsEmptyScreen(
    modifier: Modifier = Modifier, onAddExpense: () -> Unit = {}
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background, topBar = { AnalyticsTopBar() }) { innerPadding ->

        Column(
            modifier = modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp)
        ) {

            // Centered body
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp).weight(1f),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.analytic_illustration),
                    contentDescription = "Analytics illustration",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth().fillMaxHeight(0.45f)
                )

                Spacer(Modifier.height(24.dp))

                Text(
                    text = "Not enough data yet",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(12.dp))

                Text(
                    text = "We'll show you spending insights once we have more transactions. This usually happens after a few days of use.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            // Bottom action - filled CTA to match EmptyHomeContent
            Button(
                onClick = onAddExpense,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("Add transaction", color = MaterialTheme.colorScheme.background)
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun AnalyticsEmptyScreenPreview() {
    MaterialTheme {
        AnalyticsEmptyScreen()
    }
}