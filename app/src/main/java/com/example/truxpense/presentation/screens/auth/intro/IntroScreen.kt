package com.example.truxpense.presentation.screens.auth.intro

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.truxpense.R
import com.example.truxpense.presentation.screens.auth.components.AuthButton
import com.example.truxpense.presentation.utils.clearFocusOnTap
import kotlinx.coroutines.delay
import kotlin.math.absoluteValue



data class IntroPage(
    val imageRes: Int,
    val title: String,
    val subtitle: String
)

@OptIn(ExperimentalFoundationApi::class)
@Suppress("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun IntroScreen(
    onGetStarted: () -> Unit = {},
    onLogin: () -> Unit = {}
) {
    val pages = remember { introPages }
    val pagerState = rememberPagerState { pages.size }

    LaunchedEffect(pagerState.isScrollInProgress) {
        if (!pagerState.isScrollInProgress) {
            delay(4000)
            pagerState.animateScrollToPage(
                (pagerState.currentPage + 1) % pages.size
            )
        }
    }

    Scaffold(
        bottomBar = {
            IntroBottomActions(
                onGetStarted = onGetStarted,
                onLogin = onLogin
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
                    pageSpacing = 16.dp
                ) { index ->
                    IntroPageContent(
                        page = pages[index],
                        pagerState = pagerState,
                        pageIndex = index,
                        pagerHeight = pagerHeight
                    )
                }

                Spacer(Modifier.height(12.dp))

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
        val imageScale = 1f - (clamped * 0.03f)

        Image(
            painter = painterResource(page.imageRes),
            contentDescription = page.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .height(imageHeight)
                .fillMaxWidth()
                .graphicsLayer {
                    alpha = imageAlpha
                    scaleX = imageScale
                    scaleY = imageScale
                }
        )

        // Text block with flexible height so longer text can show fully while keeping baseline alignment
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp, max = textMaxHeight),
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
            val selected = pagerState.currentPage == index

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
    onLogin: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding( vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        AuthButton(
            onClick = onGetStarted,
            text = "Get Started"
        )

        Spacer(Modifier.height(10.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Already have an account?",
                fontSize = 13.sp
            )
            Text(
                text = " Login",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clickable(onClick = onLogin)
                    .padding(start = 4.dp)
            )
        }
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
