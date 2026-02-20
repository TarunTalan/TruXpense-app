package com.example.truxpense.presentation.screens.auth.intro

import android.app.Activity
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.truxpense.R
import com.example.truxpense.presentation.screens.auth.components.AuthButton
import com.example.truxpense.presentation.screens.auth.components.OAuthButton
import com.example.truxpense.presentation.utils.blockTouchesWhen
import com.example.truxpense.presentation.utils.clearFocusOnTap
import kotlin.math.absoluteValue

// New animation imports
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat


data class IntroPage(
    val imageRes: Int,
    val title: String,
    val subtitle: String
)

@OptIn(ExperimentalFoundationApi::class)
@Suppress("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun IntroScreen(
    onGetStarted: () -> Unit = {}
) {
    val pages = remember { introPages }

    // Create a large virtual page space so user can swipe indefinitely; keep paging manual (no auto-advance)
    val LOOP_MULTIPLIER = 1000
    val totalPages = pages.size * LOOP_MULTIPLIER
    val initialPage = totalPages / 2 // start near middle to allow both-direction swipes
    val pagerState = rememberPagerState(initialPage = initialPage) { totalPages }

    // Intro handles Google sign-in directly via IntroViewModel
    val viewModel: IntroViewModel = androidx.hilt.navigation.compose.hiltViewModel()
    val state by viewModel.state.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    // Show a toast when an error appears in the view state and clear it afterwards
    LaunchedEffect(state.error) {
        state.error?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    // One Tap IntentSender launcher: start when IntroViewModel.signInIntentSender becomes available
    val intentSenderLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult(),
        onResult = { result ->
            // If user completed flow successfully, forward the resulting Intent to ViewModel.
            if (result.resultCode == Activity.RESULT_OK) {
                viewModel.handleOneTapResult(context, result.data)
            } else {
                // User cancelled or closed the One Tap UI — clear pending intent and stop loading without error
                viewModel.cancelOneTapSignIn()
            }
            // Clear the pending intent in all cases
            viewModel.clearSignInIntentSender()
        }
    )

    Box(modifier = Modifier.fillMaxSize().blockTouchesWhen(state.isLoading)) {
        Scaffold(
            bottomBar = {
                IntroBottomActions(
                    onGetStarted = onGetStarted,
                    onGoogle = {
                        // Start One Tap sign-in flow: ask ViewModel to begin and expose an IntentSender
                        viewModel.startOneTapSignIn(context)
                    },
                    isLoading = state.isLoading
                )
            }
        ) { innerPadding ->
            BoxWithConstraints(modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .clearFocusOnTap()) {
                val availableHeight = this.maxHeight
                // Reserve space for bottom actions (~120.dp) + some padding, then let pager use the rest
                val bottomReserved = 140.dp
                val pagerHeight = (availableHeight - bottomReserved).coerceAtLeast(360.dp)

                Column(
                    modifier = Modifier
                        .fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top
                ) {
                    Spacer(Modifier.height(24.dp))

                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(pagerHeight),
                        pageSpacing = 8.dp,
                        userScrollEnabled = !state.isLoading
                    ) { index ->
                        // map virtual index to real page
                        val page = pages[index % pages.size]
                        IntroPageContent(
                            page = page,
                            pagerState = pagerState,
                            pageIndex = index,
                            pagerHeight = pagerHeight
                        )
                    }

                    Spacer(Modifier.height(20.dp))

                    AnimatedPagerIndicator(
                        pagerState = pagerState,
                        pageCount = pages.size
                    )

                    Spacer(Modifier.height(16.dp))

                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
    // If ViewModel exposed a pending IntentSender, launch it from the ActivityResult launcher
    LaunchedEffect(state.signInIntentSender) {
        val sender = state.signInIntentSender
        if (sender != null) {
            try {
                val intentSenderRequest = androidx.activity.result.IntentSenderRequest.Builder(sender).build()
                intentSenderLauncher.launch(intentSenderRequest)
            } catch (_: Exception) {
                // Surface to viewmodel
                viewModel.handleOneTapResult(context, null)
                viewModel.clearSignInIntentSender()
            }
        }
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun IntroPageContent(
    page: IntroPage,
    pagerState: PagerState,
    pageIndex: Int,
    pagerHeight: androidx.compose.ui.unit.Dp
) {
    val offset =
        ((pagerState.currentPage - pageIndex) +
                pagerState.currentPageOffsetFraction).absoluteValue

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 0.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {

        // Calculate image and text heights based on pagerHeight so text always fits
        val imageHeight = (pagerHeight * 0.6f).coerceAtMost(340.dp)
        val textMaxHeight = (pagerHeight - imageHeight).coerceAtLeast(120.dp)

        val clamped = offset.coerceIn(0f, 1f)
        val imageAlpha = 1f - (clamped * 0.25f)

        // Subtle infinite "bobbing" animation that loops.
        // We modulate the animation amplitude by how centered the page is (so offscreen pages don't animate strongly).
        val visibilityFactor = (1f - clamped).coerceAtLeast(0f).coerceAtMost(1f)
        val infiniteTransition = rememberInfiniteTransition()
        // Reduced amplitude for a more subtle vertical motion (was ±8f).
        val bobDp by infiniteTransition.animateFloat(
            initialValue = -4f,
            targetValue = 4f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            )
        )
        val bobOffset = (bobDp * visibilityFactor).dp

        // Keep layout size consistent and avoid scaling the image view itself so
        // the text block remains vertically aligned across pages. Use ContentScale.Fit
        // so the whole illustration is visible.
        Image(
            painter = painterResource(page.imageRes),
            contentDescription = page.title,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .height(imageHeight)
                .fillMaxWidth()
                .offset(y = bobOffset)
                .graphicsLayer {
                    alpha = imageAlpha
                }
        )

        // Text block with fixed height so longer text can show fully while keeping baseline alignment
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(textMaxHeight),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = page.title,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                maxLines = 2
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = page.subtitle,
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                maxLines = 4
            )
        }
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AnimatedPagerIndicator(
    pagerState: PagerState,
    pageCount: Int
) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pageCount) { index ->
            // derive the selected dot from the virtual current page modulo the real page count
            val currentReal = (pagerState.currentPage % pageCount + pageCount) % pageCount
            val selected = currentReal == index

            val width by animateDpAsState(
                targetValue = if (selected) 10.dp else 8.dp,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy
                ),
                label = ""
            )

            val color by animateColorAsState(
                targetValue = if (selected)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
                label = ""
            )

            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .width(width)
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}


@Composable
private fun IntroBottomActions(
    onGetStarted: () -> Unit,
    onGoogle: () -> Unit,
    isLoading: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding( vertical = 28.dp)
            .blockTouchesWhen(isLoading),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        AuthButton(
            onClick = onGetStarted,
            text = "Get Started",
            enabled = true,
            isLoading = false
        )

        Spacer(Modifier.height(10.dp))

        // OAuth lives on Intro screen; clicking it launches Google sign-in
        OAuthButton(
            text = if (isLoading) "Signing in..." else "Continue with Google",
            onClick = onGoogle,
            isGoogle = true,
            enabled = !isLoading
        )

        Spacer(Modifier.height(8.dp))
    }
}


private val introPages = listOf(
    IntroPage(
        R.drawable.intro_illustration_1,
        "Understand where your money goes",
        "TruXpense automatically tracks your expenses and gives you clear spending insights."
    ),
    IntroPage(
        R.drawable.intro_illustration_2,
        "Expenses tracked automatically",
        "We read bank SMS messages to detect transactions so you don't enter anything manually."
    ),
    IntroPage(
        R.drawable.intro_illustration_3,
        "See your spending clearly",
        "Track categories, habits, and where you spend the most."
    ),
    IntroPage(
        R.drawable.intro_illustration_4,
        "Your data is safe with us",
        "Only transaction messages are read. Personal messages are never accessed."
    )
)


@Preview(showSystemUi = true)
@Composable
private fun IntroScreenPreview() {
    MaterialTheme {
        IntroScreen()
    }
}
