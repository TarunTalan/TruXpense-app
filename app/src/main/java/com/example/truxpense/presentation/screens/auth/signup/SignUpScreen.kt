package com.example.truxpense.presentation.screens.auth.signup

import android.graphics.Color.rgb
import android.util.Patterns
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.truxpense.presentation.screens.auth.components.AuthButton
import com.example.truxpense.presentation.screens.auth.components.AuthTextField
import com.example.truxpense.presentation.screens.auth.components.OAuthButton
import com.example.truxpense.presentation.utils.clearFocusOnTap
import com.example.truxpense.data.repository.GoogleSignInRepository
import com.example.truxpense.data.remote.api.TokenResponse

@Composable
fun SignUpScreen(
    onBack: (() -> Unit)? = null,
    onSignUp: ((String) -> Unit)? = null,
    onLoginNavigate: (() -> Unit)? = null,
    onOtpNavigate: (() -> Unit)? = null,
    viewModel: SignUpViewModel = hiltViewModel()
) {
    val email by viewModel.email.collectAsState()
    val checked by viewModel.agreeTnc.collectAsState()
    var showTncDialog by remember { mutableStateOf(false) }
    val isEmailValid = remember(email) { Patterns.EMAIL_ADDRESS.matcher(email).matches() }

    val context = LocalContext.current
    val googleSignInRepository = remember { GoogleSignInRepository(context) }
    val signInIntent = remember { googleSignInRepository.getSignInIntent() }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            viewModel.handleGoogleSignInResult(result.data, onSuccess = { resp: TokenResponse -> resp.accessToken?.let { onSignUp?.invoke(it) } }, onError = { err -> println("Google sign-up error: $err") })
        } else {
            println("Google sign-up canceled")
        }
    }

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
                    onClick = {
                        if (isEmailValid && checked) {
                            onSignUp?.invoke(email)
                            onOtpNavigate?.invoke()
                        }
                    },
                    text = "Sign Up",
                    enabled = checked && isEmailValid,
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Already have an account?",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp,
                        fontSize = 13.sp
                    )
                    Text(
                        text = " Login",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { onLoginNavigate?.invoke() }.padding(4.dp)
                    )
                }
            }
        },
        content = { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clearFocusOnTap()
                    .then(if (showTncDialog) Modifier.blur(16.dp) else Modifier)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Sign Up",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                    Text(
                        text = "Create your account",
                        fontSize = 16.sp,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 20.sp,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    AuthTextField(
                        bgColor = MaterialTheme.colorScheme.background,
                        label = "Email Address",
                        placeholder = "Example@xyz.com",
                        bottomLabel = "We'll send a verification code to this email",
                        value = email,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        onValueChange = { viewModel.onEmailChanged(it) },
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
                        onClick = { launcher.launch(signInIntent) },
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
                            onCheckedChange = { viewModel.onAgreeTncChanged(it) },
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
                        Text(
                            text = annotatedString,
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface),
                            modifier = Modifier
                                .padding(0.dp)
                                .clickable {
                                    showTncDialog = true
                                }
                        )
                    }
                }
            }
            if (showTncDialog) {
                AlertDialog(
                    onDismissRequest = { showTncDialog = false },
                    title = { Text("Terms and Conditions") },
                    text = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .padding(8.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                text = "TruXpense reads only the data necessary to help you track your expenses automatically.\n",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Start
                            )

                            Text(
                                text = "What we access:",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Start
                            )
                            Text(text = "• Bank transaction SMS messages.", style = MaterialTheme.typography.bodySmall, textAlign = androidx.compose.ui.text.style.TextAlign.Start)
                            Text(text = "• App usage data for product improvements.", style = MaterialTheme.typography.bodySmall, textAlign = androidx.compose.ui.text.style.TextAlign.Start)

                            Text(
                                text = "\nWhat we don’t access:",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Start
                            )
                            Text(text = "• Personal messages.", style = MaterialTheme.typography.bodySmall, textAlign = androidx.compose.ui.text.style.TextAlign.Start)
                            Text(text = "• OTPs from other apps.", style = MaterialTheme.typography.bodySmall, textAlign = androidx.compose.ui.text.style.TextAlign.Start)
                            Text(text = "• Contacts or call logs.", style = MaterialTheme.typography.bodySmall, textAlign = androidx.compose.ui.text.style.TextAlign.Start)
                            Text(text = "• Any other unrelated personal data.", style = MaterialTheme.typography.bodySmall, textAlign = androidx.compose.ui.text.style.TextAlign.Start)

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "By continuing, you agree to allow TruXpense to access the data listed above for the purpose of automatically categorizing and tracking your expenses.",
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Start,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showTncDialog = false }) {
                            Text("Close")
                        }
                    }
                )
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun SignUpScreenPreview() {
    SignUpScreen(onBack = {})
}
