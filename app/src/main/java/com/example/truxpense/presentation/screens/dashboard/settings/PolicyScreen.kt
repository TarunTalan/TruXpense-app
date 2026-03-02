package com.example.truxpense.presentation.screens.dashboard.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.truxpense.presentation.screens.dashboard.components.ScreenTopBar

// ─── Shared composable for scrollable static content ─────────────────────────

@Composable
private fun StaticContentScreen(
    title: String,
    sections: List<Pair<String, String>>,   // header → body
    lastUpdated: String,
    onBack: () -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { ScreenTopBar(headerTitle = title, showBack = true, onBack = onBack) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "Last updated: $lastUpdated",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
            )

            sections.forEach { (header, body) ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (header.isNotBlank()) {
                        Text(
                            text = header,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                    Text(
                        text = body,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        lineHeight = MaterialTheme.typography.bodySmall.fontSize * 1.6
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(0.4f))
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

// ─── Privacy Policy ───────────────────────────────────────────────────────────

@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit = {}) {
    StaticContentScreen(
        title = "Privacy Policy",
        lastUpdated = "1 March 2025",
        onBack = onBack,
        sections = listOf(
            "Introduction" to
                """TruXpense ("we", "us", "our") is committed to protecting your personal data. This policy explains what information we collect, how we use it, and your rights.""",

            "Information We Collect" to
                "• SMS data: We read bank SMS messages solely to extract transaction details. Raw SMS content is never stored on our servers.\n" +
                "• Profile data: Name and phone number you provide during sign-up.\n" +
                "• Usage data: Anonymised analytics to improve the app (crash reports, screen views).",

            "How We Use Your Data" to
                "• Auto-categorise transactions using on-device ML.\n" +
                "• Generate spending insights and budget alerts personalised to you.\n" +
                "• Send reminders for bills and subscriptions you have enabled.\n" +
                "We do not sell your data to any third party.",

            "Data Storage & Security" to
                "All data is encrypted in transit (TLS 1.3) and at rest (AES-256). Transaction data is stored on secure cloud servers in India. We retain your data for as long as your account is active.",

            "Your Rights" to
                "You may request a copy of your data, correct inaccuracies, or permanently delete your account and all associated data at any time via Settings → Delete account.",

            "Contact" to
                "For privacy concerns, email us at privacy@truxpense.app. We aim to respond within 72 hours.",
        )
    )
}


// ─── Terms of Service ─────────────────────────────────────────────────────────

@Composable
fun TermsScreen(onBack: () -> Unit = {}) {
    StaticContentScreen(
        title = "Terms of Service",
        lastUpdated = "1 March 2025",
        onBack = onBack,
        sections = listOf(
            "Acceptance of Terms" to
                "By using TruXpense you agree to these Terms. If you do not agree, please uninstall the app.",

            "Use of the App" to
                "TruXpense is a personal finance tool intended for individual, non-commercial use. You agree not to reverse-engineer, redistribute, or misuse the service.",

            "Account Responsibility" to
                "You are responsible for maintaining the confidentiality of your account credentials. Notify us immediately at security@truxpense.app if you suspect unauthorised access.",

            "SMS Permission" to
                "Granting READ_SMS permission is required for auto-tracking. You may revoke the permission at any time via Android Settings; manual transaction entry will still be available.",

            "Disclaimer" to
                "TruXpense provides financial information for awareness purposes only. It is not a licensed financial advisor. Always consult a qualified professional before making financial decisions.",

            "Changes to Terms" to
                "We may update these Terms from time to time. Continued use of the app after changes constitutes acceptance of the new Terms.",

            "Contact" to
                "Questions? Reach us at legal@truxpense.app.",
        )
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun TermsPreview() {
    MaterialTheme { TermsScreen() }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PrivacyPolicyPreview() {
    MaterialTheme { PrivacyPolicyScreen() }
}