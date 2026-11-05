package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.MoneyEngine
import com.example.model.BudgetStateStatus
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberOnContainer
import com.example.ui.theme.CreditGreen
import com.example.ui.theme.CreditGreenLight
import com.example.ui.theme.DebtRed
import com.example.ui.theme.DebtRedLight
import com.example.ui.theme.Forest600
import com.example.ui.theme.Forest700
import com.example.ui.theme.Forest800
import com.example.ui.theme.Forest850
import com.example.ui.theme.Forest900
import com.example.ui.theme.HearthGlassBorder
import com.example.ui.theme.HearthGlassSurface
import com.example.ui.theme.HearthTextDark
import com.example.ui.theme.HearthTextMuted
import com.example.ui.theme.Sage200
import com.example.ui.theme.Sage300
import com.example.ui.theme.TealPrimary
import com.example.ui.theme.ZeniOatMid
import com.example.ui.theme.ZeniOliveDockBg
import com.example.ui.theme.ZeniSageBottom
import com.example.ui.theme.ZeniSageTop
import com.example.ui.theme.ZeniSwitcherTrack
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs

private val wholeFormatter = DecimalFormat("#,##0", DecimalFormatSymbols(Locale.US))

/**
 * Zeni + Luminous Sage Glass full-screen atmospheric canvas with ambient light orbs.
 */
@Composable
fun HearthAtmosphericBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.0f to ZeniSageTop,
                        0.38f to ZeniOatMid,
                        0.76f to ZeniSageBottom,
                        1.0f to ZeniOliveDockBg
                    )
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Top-left soft mint glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.45f), Color.Transparent),
                    center = Offset(size.width * 0.18f, size.height * 0.10f),
                    radius = size.minDimension * 0.65f
                ),
                center = Offset(size.width * 0.18f, size.height * 0.10f),
                radius = size.minDimension * 0.65f
            )
            // Mid-right warm cream glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFBF8EE).copy(alpha = 0.55f), Color.Transparent),
                    center = Offset(size.width * 0.85f, size.height * 0.45f),
                    radius = size.minDimension * 0.70f
                ),
                center = Offset(size.width * 0.85f, size.height * 0.45f),
                radius = size.minDimension * 0.70f
            )
            // Bottom-left deep forest ambient tint for floating dock contrast
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Forest700.copy(alpha = 0.18f), Color.Transparent),
                    center = Offset(size.width * 0.25f, size.height * 0.92f),
                    radius = size.minDimension * 0.60f
                ),
                center = Offset(size.width * 0.25f, size.height * 0.92f),
                radius = size.minDimension * 0.60f
            )
        }
        content()
    }
}

/**
 * Responsive screen container that adapts margins for compact (<360dp), standard, and tablet (>600dp) viewports.
 */
@Composable
fun ResponsiveScreenContainer(
    modifier: Modifier = Modifier,
    scrollable: Boolean = true,
    bottomSafePadding: Dp = 104.dp,
    content: @Composable ColumnScope.(isCompact: Boolean) -> Unit
) {
    HearthAtmosphericBackground(modifier = modifier) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            val isCompact = maxWidth < 360.dp
            val horizontalPad = if (isCompact) 14.dp else 20.dp
            val scrollMod = if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier

            Column(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxSize()
                    .then(scrollMod)
                    .padding(start = horizontalPad, end = horizontalPad, top = 12.dp, bottom = bottomSafePadding),
                verticalArrangement = Arrangement.spacedBy(if (isCompact) 12.dp else 16.dp)
            ) {
                content(isCompact)
            }
        }
    }
}

/**
 * Zeni-style top segmented pill switcher (e.g. Overview | Budget, Pending | All | Bought, Ledger | Activity).
 */
@Composable
fun GlassSegmentedSwitcher(
    options: List<String>,
    selectedOption: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    optionLabels: Map<String, String> = emptyMap(),
    optionTestTags: Map<String, String> = emptyMap()
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(ZeniSwitcherTrack)
            .border(1.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(50))
            .padding(4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            options.forEach { option ->
                val isSelected = option == selectedOption
                val bgColor by animateColorAsState(
                    targetValue = if (isSelected) Color.White else Color.Transparent,
                    label = "seg_bg"
                )
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) Forest900 else Forest900.copy(alpha = 0.68f),
                    label = "seg_txt"
                )
                val tag = optionTestTags[option]

                Box(
                    modifier = Modifier
                        .then(if (isSelected) Modifier.shadow(4.dp, RoundedCornerShape(50)) else Modifier)
                        .clip(RoundedCornerShape(50))
                        .background(bgColor)
                        .clickable { onSelect(option) }
                        .padding(horizontal = 15.dp, vertical = 7.dp)
                        .then(if (tag != null) Modifier.testTag(tag) else Modifier),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = optionLabels[option] ?: option,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                        color = textColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * Circular profile avatar button with white border and online emerald status dot (matches Zeni top-right avatar).
 */
@Composable
fun ProfileAvatarOrb(
    memberName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = "top_user_profile_chip"
) {
    val initials = memberName.split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercase() }
        .joinToString("")
        .ifEmpty { memberName.take(2).uppercase() }

    Box(
        modifier = modifier
            .size(40.dp)
            .shadow(4.dp, CircleShape)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    colors = listOf(Forest850, Forest600)
                )
            )
            .border(2.dp, Color.White.copy(alpha = 0.9f), CircleShape)
            .clickable { onClick() }
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            color = Color.White,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 13.sp,
            letterSpacing = 0.5.sp
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(10.dp)
                .clip(CircleShape)
                .background(CreditGreen)
                .border(1.5.dp, Color.White, CircleShape)
        )
    }
}

/**
 * Split whole + decimal currency typography matching Zeni ($1,976.00) and Stitch (PKR +250.00).
 */
@Composable
fun SplitDecimalMoneyText(
    paisa: Long,
    modifier: Modifier = Modifier,
    currencyPrefix: String = "PKR ",
    wholeFontSize: TextUnit = 30.sp,
    decimalFontSize: TextUnit = 19.sp,
    prefixFontSize: TextUnit = 14.sp,
    wholeColor: Color = Color.White,
    decimalColor: Color = Color.White.copy(alpha = 0.7f),
    prefixColor: Color = Sage300,
    showPlusSign: Boolean = false
) {
    val sign = when {
        paisa > 0 && showPlusSign -> "+"
        paisa < 0 -> "-"
        else -> ""
    }
    val absPaisa = abs(paisa)
    val rupees = absPaisa / 100
    val remainder = absPaisa % 100
    val wholeFormatted = wholeFormatter.format(rupees)
    val decimalFormatted = String.format(Locale.US, "%02d", remainder)

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Bottom
    ) {
        if (currencyPrefix.isNotEmpty()) {
            Text(
                text = currencyPrefix,
                fontSize = prefixFontSize,
                fontWeight = FontWeight.Bold,
                color = prefixColor,
                modifier = Modifier.padding(bottom = 3.dp)
            )
        }
        Text(
            text = "$sign$wholeFormatted",
            fontSize = wholeFontSize,
            fontWeight = FontWeight.ExtraBold,
            color = wholeColor,
            letterSpacing = (-0.8).sp
        )
        Text(
            text = ".$decimalFormatted",
            fontSize = decimalFontSize,
            fontWeight = FontWeight.Bold,
            color = decimalColor,
            modifier = Modifier.padding(bottom = 2.dp)
        )
    }
}

/**
 * Frosted glass card surface with crisp white border.
 */
@Composable
fun GlassPanelCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    containerColor: Color = HearthGlassSurface,
    borderColor: Color = HearthGlassBorder,
    elevation: Dp = 2.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    Card(
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        modifier = modifier
            .border(1.dp, borderColor, shape)
            .then(if (onClick != null) Modifier.clip(shape).clickable { onClick() } else Modifier)
    ) {
        Column(content = content)
    }
}

/**
 * Deep Forest-Emerald card with optional Zeni halftone dot-matrix pattern drawn on the left side.
 */
@Composable
fun DarkForestHeroCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    showDotMatrix: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(Forest900, Forest850, Forest700)
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.14f), shape)
    ) {
        if (showDotMatrix) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val cols = 14
                val rows = 9
                val spacingX = (size.width * 0.45f) / cols
                val spacingY = size.height / rows
                for (c in 0 until cols) {
                    for (r in 0 until rows) {
                        val alphaFactor = (1f - (c.toFloat() / cols)) * 0.12f
                        if ((c + r) % 2 == 0) {
                            drawCircle(
                                color = Sage300.copy(alpha = alphaFactor),
                                radius = 1.6.dp.toPx(),
                                center = Offset(14.dp.toPx() + c * spacingX, 14.dp.toPx() + r * spacingY)
                            )
                        }
                    }
                }
            }
        }
        content()
    }
}

@Composable
fun MoneyText(
    paisa: Long,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 16.sp,
    fontWeight: FontWeight = FontWeight.Bold,
    isNetBalance: Boolean = false,
    includePrefix: Boolean = true
) {
    val color = when {
        isNetBalance && paisa > 0 -> CreditGreen
        isNetBalance && paisa < 0 -> DebtRed
        else -> HearthTextDark
    }

    Text(
        text = MoneyEngine.formatPkr(paisa, includePrefix = includePrefix),
        color = color,
        fontSize = fontSize,
        fontWeight = fontWeight,
        modifier = modifier
    )
}

@Composable
fun StatusChip(
    status: BudgetStateStatus,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, label, icon) = when (status) {
        BudgetStateStatus.ON_TRACK -> Quad(
            CreditGreenLight,
            CreditGreen,
            "On Track",
            Icons.Default.CheckCircle
        )
        BudgetStateStatus.NEAR_LIMIT -> Quad(
            AmberContainer,
            AmberOnContainer,
            "Near Limit",
            Icons.Default.Warning
        )
        BudgetStateStatus.OVER_BUDGET -> Quad(
            DebtRedLight,
            DebtRed,
            "Over Budget",
            Icons.Default.ErrorOutline
        )
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(bgColor)
            .border(1.dp, textColor.copy(alpha = 0.25f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = textColor,
            modifier = Modifier.size(13.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
fun MemberAvatar(
    name: String,
    memberId: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    isSelected: Boolean = false
) {
    val initials = name.split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercase() }
        .joinToString("")
        .ifEmpty { memberId }

    val bgTint = when (memberId) {
        "M01" -> Forest800
        "M02" -> AmberAccent
        "M03" -> CreditGreen
        else -> TealPrimary
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(bgTint)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) Forest900 else Color.White.copy(alpha = 0.8f),
                shape = CircleShape
            )
    ) {
        Text(
            text = initials,
            color = Color.White,
            fontWeight = FontWeight.ExtraBold,
            fontSize = (size.value * 0.38f).sp
        )
    }
}

@Composable
fun LoadingButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    enabled: Boolean = true,
    testTag: String = "loading_button"
) {
    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(
            containerColor = Forest900,
            contentColor = Color.White,
            disabledContainerColor = Forest900.copy(alpha = 0.4f),
            disabledContentColor = Color.White.copy(alpha = 0.7f)
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
        modifier = modifier
            .height(52.dp)
            .testTag(testTag)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = Color.White,
                strokeWidth = 2.dp,
                modifier = Modifier.size(22.dp)
            )
        } else {
            Text(
                text = text,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                letterSpacing = 0.2.sp
            )
        }
    }
}

@Composable
fun ErrorBanner(
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = DebtRedLight.copy(alpha = 0.9f)),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, DebtRed.copy(alpha = 0.3f), RoundedCornerShape(18.dp))
            .padding(vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = "Error",
                tint = DebtRed,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = message,
                color = DebtRed,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            if (onRetry != null) {
                OutlinedButton(
                    onClick = onRetry,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text("Retry", fontSize = 12.sp, color = DebtRed, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun EmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionButtonText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    GlassPanelCard(
        cornerRadius = 26.dp,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(Sage200.copy(alpha = 0.7f))
                    .border(1.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = Forest800,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = HearthTextDark
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = HearthTextMuted,
                textAlign = TextAlign.Center
            )
            if (actionButtonText != null && onActionClick != null) {
                Spacer(modifier = Modifier.height(18.dp))
                Button(
                    onClick = onActionClick,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Forest900,
                        contentColor = Color.White
                    )
                ) {
                    Text(actionButtonText, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
