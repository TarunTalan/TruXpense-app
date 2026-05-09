package com.example.truxpense.presentation.theme

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Central dimension / typography tokens used across ALL dashboard screens.
// ⚠️  No screen file should hardcode dp or sp values — reference a token here.
object DashboardDimens {

    // ── Spacing scale ─────────────────────────────────────────────────────────
    /** 2 dp — micro gap */
    val spaceXxs = 2.dp

    /** 4 dp — tight gap */
    val spaceXs = 4.dp

    /** 6 dp — small gap */
    val spaceSm = 6.dp

    /** 8 dp — standard gap */
    val spaceMd = 8.dp

    /** 10 dp — card sub-section gap */
    val spaceMdL = 10.dp

    /** 12 dp — card internal padding */
    val spaceLg = 12.dp

    /** 16 dp — outer horizontal padding / section gap */
    val spaceXl = 16.dp

    /** 24 dp — section separator */
    val spaceXxl = 24.dp

    /** 32 dp — large section gap */
    val spaceXxxl = 32.dp

    // ── Screen / card padding ─────────────────────────────────────────────────
    /** Screen horizontal edge padding */
    val screenPaddingH = spaceXl

    /** Standard internal card padding */
    val cardPadding = spaceXl

    /** Compact card padding */
    val cardPaddingComp = spaceLg

    /** Dropdown list horizontal padding */
    val listPaddingH = spaceMd

    /** Dropdown list vertical padding */
    val listPaddingV = spaceLg

    /** Notes / detail card horizontal padding */
    val detailCardPadH = spaceLg

    /** Notes / detail card vertical padding */
    val detailCardPadV = 14.dp

    /** Row vertical padding (input rows, detail rows) */
    val rowPadV = 15.dp

    // ── Component heights ─────────────────────────────────────────────────────
    /** Standard tappable button height */
    val buttonHeight = 48.dp

    /** Small button height */
    val buttonHeightSm = 32.dp

    /** Bottom navigation bar row height */
    val bottomNavHeight = 68.dp

    /** Progress bar track height */
    val progressBarHeight = 7.dp

    /** Consistent (thinner) progress bar height */
    val progressBarHeight2 = 6.dp

    /** Minimum height for text input / notes field */
    val inputMinHeight = 52.dp

    /** Detail-card form row height — taller than input for label + value layout */
    val detailRowHeight = 64.dp

    /** Chart divider / thin rule height */
    val dividerHeight = 1.dp

    /** Thin row divider (inside cards) */
    val dividerThin = 0.5.dp

    // ── Icon sizes ────────────────────────────────────────────────────────────
    /** Micro icon — detail rows, annotation icons (14 dp) */
    val iconXs = 14.dp

    /** Small icon — nav arrows, row badges (16 dp) */
    val iconSm = 16.dp

    /** Navigator / toolbar icon — (18 dp) */
    val iconNav = 18.dp

    /** Medium icon — toolbar actions, banners (20 dp) */
    val iconMd = 20.dp

    /** Standard icon — bottom nav (24 dp) */
    val iconLg = 24.dp

    // ── Icon button / touch targets ───────────────────────────────────────────
    /** Small icon button touch target (28 dp) */
    val iconButtonSm = 28.dp

    /** Medium icon button touch target — navigators (40 dp) */
    val iconButtonMd = 40.dp

    // ── Avatar / badge ────────────────────────────────────────────────────────
    /** Merchant initial avatar size (transaction rows) */
    val avatarSize = 38.dp

    /** Filter / active-filter badge size */
    val badgeSize = 16.dp

    /** Badge corner offset from parent */
    val badgeOffset = spaceXs

    /** Category color dot */
    val dotSize = 10.dp

    // ── Chart dimensions ──────────────────────────────────────────────────────
    /** Donut chart canvas size */
    val donutChartSize = 200.dp

    /** Donut ring stroke width */
    val donutStrokeWidth = 38.dp

    /** Spending trend chart height */
    val trendChartHeight = 120.dp

    /** Budget detail chart height */
    val budgetChartHeight = 110.dp

    /** Chart internal horizontal padding */
    val chartPadH = spaceMd

    /** Chart internal top / bottom padding */
    val chartPadV = spaceMd

    /** Label lift above a chart data point */
    val chartLabelLift = 16.dp

    /** Normal chart dot radius */
    val chartDotNormal = 3.dp

    /** Highlighted chart dot radius */
    val chartDotHighlight = 5.dp

    /** Chart line stroke width */
    val chartLineStroke = 2.dp

    // ── Corner radii ──────────────────────────────────────────────────────────
    /** Section / large card corners */
    val cornerCard = 16.dp

    /** Chip corners */
    val cornerChip = 8.dp

    /** Badge / tag corners */
    val cornerBadge = 4.dp

    /** Period toggle outer container */
    val cornerToggleOuter = 14.dp

    /** Period toggle inner selected tab */
    val cornerToggleInner = 10.dp

    /** Active filter / dismissible chip corners */
    val cornerFilterChip = 20.dp

    /** Bottom sheet handle pill corner radius */
    val cornerSheetHandle = 2.dp

    // ── Bottom sheet ──────────────────────────────────────────────────────────
    /** Sheet drag-handle bar width */
    val sheetHandleWidth = 36.dp

    /** Sheet drag-handle bar height */
    val sheetHandleHeight = 4.dp

    /** Bottom content padding inside a modal sheet */
    val sheetBottomPadding = spaceXxl

    // ── Borders / elevation ───────────────────────────────────────────────────
    /** Standard border stroke width */
    val borderStroke = 1.dp

    /** Default card drop-shadow elevation */
    val cardElevation = 4.dp

    // ── Special layout helpers ────────────────────────────────────────────────
    /** FAB clearance at bottom of scrollable lists */
    val fabClearance = 72.dp

    /** Hero zone top padding */
    val heroZonePadTop = 24.dp

    /** Hero zone bottom padding */
    val heroZonePadBottom = 28.dp

    /** Chip / pill horizontal content padding */
    val chipPaddingH = 14.dp

    // ── Typography scale ──────────────────────────────────────────────────────
    /** Helper / caption / chart label — 9 sp */
    val textXs = 9.sp

    /** Fine print / badge label — 10 sp */
    val textSm = 10.sp

    /** Label / secondary / hint — 12 sp */
    val textMd = 12.sp

    /** Body copy — 13 sp */
    val textLg = 13.sp

    /** Input field / medium body — 14 sp */
    val textInput = 14.sp

    /** Button label / primary body — 15 sp */
    val textXl = 15.sp

    /** Section title / prominent label — 16 sp */
    val textXxl = 16.sp

    /** Large hero amount — 40 sp */
    val textHero = 40.sp

    // ── Line heights ──────────────────────────────────────────────────────────
    /** Standard helper / hint line-height */
    val lineHeightHelper = 18.sp

    /** Banner / notice line-height */
    val lineHeightBanner = 20.sp

    // ── Letter spacing ────────────────────────────────────────────────────────
    /** Hero amount tight tracking */
    val letterSpacingHero = (-1).sp
}