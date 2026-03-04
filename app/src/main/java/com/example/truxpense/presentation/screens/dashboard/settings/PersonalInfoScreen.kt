package com.example.truxpense.presentation.screens.dashboard.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.truxpense.presentation.screens.dashboard.components.ScreenTopBar
import com.example.truxpense.presentation.utils.clearFocusOnTap

@Composable
fun PersonalInfoScreen(
    initialUsername: String = "",
    initialPhone: String = "",
    isSaving: Boolean = false,
    onBack: () -> Unit = {},
    onSave: (name: String, phone: String) -> Unit = { _, _ -> },
) {
    var name  by remember { mutableStateOf(initialUsername) }
    var phone by remember { mutableStateOf(initialPhone) }
    var nameError  by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }

    fun validate(): Boolean {
        nameError  = if (name.isBlank()) "Name cannot be empty" else null
        phoneError = when {
            phone.isBlank() -> "Phone cannot be empty"
            !phone.matches(Regex("^[+]?[0-9]{10,13}$")) -> "Enter a valid phone number"
            else -> null
        }
        return nameError == null && phoneError == null
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { ScreenTopBar(headerTitle = "Personal Information", showBack = true, onBack = onBack) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .clearFocusOnTap(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Update your profile details",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it; nameError = null },
                label = { Text("Full Name") },
                singleLine = true,
                isError = nameError != null,
                supportingText = nameError?.let { err -> { Text(err) } },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            )

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it; phoneError = null },
                label = { Text("Phone Number") },
                singleLine = true,
                isError = phoneError != null,
                supportingText = phoneError?.let { err -> { Text(err) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            )

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = { if (validate()) onSave(name.trim(), phone.trim()) },
                enabled = !isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Save Changes", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PersonalInfoPreview() {
    MaterialTheme {
        PersonalInfoScreen(initialUsername = "Tushar Talan", initialPhone = "+91 9876543210")
    }
}
