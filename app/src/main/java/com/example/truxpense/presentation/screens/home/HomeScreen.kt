package com.example.truxpense.presentation.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel


@Composable
fun HomeScreen(onLogout: (() -> Unit)? = null) {
    val vm = hiltViewModel<HomeViewModel>()

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "Home", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = {
                // Clear stored user data and then notify nav host to navigate to Intro
                vm.logout(onComplete = {
                    onLogout?.invoke()
                })
            }) {
                Text(text = "Logout")
            }
        }
    }
}
