package com.example.truxpense.presentation.screens.auth.login

import android.graphics.Color.rgb
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.truxpense.presentation.screens.auth.components.AuthButton
import com.example.truxpense.presentation.screens.auth.components.AuthTextField
import com.example.truxpense.presentation.screens.auth.components.OAuthButton

@Composable
fun LoginScreen(
    onBack: (() -> Unit)? = null,
    onGoogleAuth: (() -> Unit)? = null,
    onLogin: ((String) -> Unit)? = null,
    onSignUpNavigate: (() -> Unit)? = null
) {
    var email by remember { mutableStateOf("") }
    var checked by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 30.dp, start = 5.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                IconButton(
                    onClick = { onBack?.invoke() },
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                    )
                }
            }
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AuthButton(
                    onClick = {},
                    text = "Verify Email",
                    enabled = checked,
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "New to Spendsense?",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp,
                        fontSize = 13.sp
                    )
                    Text(
                        text = " Sign up",
                        style = MaterialTheme.typography.labelMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { onSignUpNavigate?.invoke() }.padding(4.dp)
                    )
                }
            }
        },
        content = { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Login",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(5.dp))
                Text(
                    text = "Welcome back!",
                    fontSize = 16.sp,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 20.sp,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(20.dp))
                AuthTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = "Email Address",
                    bottomLabel = "We'll send a verification code to this email",
                    placeholder = "Example@xyz.com",
                    bgColor = MaterialTheme.colorScheme.background,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        thickness = DividerDefaults.Thickness,
                        color = Color(rgb(193, 199, 205))
                    )
                    Text(
                        text = "  or  ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        thickness = DividerDefaults.Thickness,
                        color = Color(rgb(193, 199, 205))
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                OAuthButton(
                    modifier = Modifier,
                    text = "Continue with Google",
                    onClick = {},
                    isGoogle = true
                )
                Spacer(modifier = Modifier.height(16.dp))
                OAuthButton(
                    modifier = Modifier,
                    text = "Continue with Facebook",
                    onClick = {},
                    isGoogle = false
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = checked,
                        onCheckedChange = { checked = it },
                        modifier = Modifier.padding(start = 4.dp, end = 12.dp).size(10.dp),
                        colors = CheckboxDefaults.colors(
                            uncheckedColor = MaterialTheme.colorScheme.outline,
                            checkmarkColor = MaterialTheme.colorScheme.background,
                            checkedColor = MaterialTheme.colorScheme.primary,
                            disabledUncheckedColor = MaterialTheme.colorScheme.outline
                        )
                    )
                    val isDarkTheme = !isSystemInDarkTheme()
                    val tncColor = if (isDarkTheme) Color(rgb(59, 120, 194)) else Color(rgb(127, 169, 217))
                    val annotatedString = buildAnnotatedString {
                        append("I agree to the ")
                        pushStringAnnotation(tag = "TNC", annotation = "https://your-terms-url.com")
                        withStyle(
                            style = SpanStyle(
                                color = tncColor,
                                fontWeight = FontWeight.Medium,
                                textDecoration = TextDecoration.Underline
                            )
                        ) {
                            append("Terms and Conditions")
                        }
                        pop()
                    }
                    ClickableText(
                        text = annotatedString,
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface),
                        onClick = { offset ->
                            annotatedString.getStringAnnotations(tag = "TNC", start = offset, end = offset)
                                .firstOrNull()?.let {
                                    // TODO: Handle Terms and Conditions click
                                }
                        },
                        modifier = Modifier.padding(0.dp)
                    )
                }
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    LoginScreen(onBack = {})
}