package com.altankoc.nidado.ui.components.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.altankoc.nidado.ui.theme.*

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 28.dp,
    isAllCompleted: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val backgroundBrush = if (isDark) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF0F3D52),
                Color(0xFF071E28),
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF134E66),
                Color(0xFF0A2E3D),
            )
        )
    }

    val borderBrush = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.62f),
            VibrantTeal.copy(alpha = 0.88f),
            SoftTeal.copy(alpha = 0.22f),
            Color.Transparent
        ),
        start = Offset(0f, 0f),
        end   = Offset(750f, 750f)
    )

    val shadowElevation  = if (isAllCompleted) 6.dp else if (isDark) 20.dp else 14.dp
    val shadowAmbient    = if (isDark) VibrantTeal.copy(alpha = 0.30f) else Color.Black.copy(alpha = 0.18f)
    val shadowSpot       = if (isDark) VibrantTeal.copy(alpha = 0.18f) else Color.Black.copy(alpha = 0.28f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation    = shadowElevation,
                shape        = RoundedCornerShape(cornerRadius),
                ambientColor = shadowAmbient,
                spotColor    = shadowSpot
            )
            .clip(RoundedCornerShape(cornerRadius))
            .background(brush = backgroundBrush)
            .drawWithContent {
                val r = cornerRadius.toPx()

                val topPath = Path().apply {
                    moveTo(r, 0f)
                    lineTo(size.width - r, 0f)
                    quadraticTo(size.width, 0f, size.width, r)
                }
                drawPath(
                    path  = topPath,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.48f),
                            Color.White.copy(alpha = 0.18f),
                            Color.Transparent
                        )
                    ),
                    style = Stroke(width = 2.dp.toPx())
                )

                val leftPath = Path().apply {
                    moveTo(0f, r)
                    lineTo(0f, size.height * 0.42f)
                }
                drawPath(
                    path  = leftPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.White.copy(alpha = 0.22f), Color.Transparent)
                    ),
                    style = Stroke(width = 1.5.dp.toPx())
                )

                drawContent()
            }
            .border(
                width  = 1.dp,
                brush  = borderBrush,
                shape  = RoundedCornerShape(cornerRadius)
            )
    ) {
        content()

        // Tamamlandı rozeti — smooth scale + fade ile gelir/gider
        AnimatedVisibility(
            visible = isAllCompleted,
            enter = scaleIn(
                initialScale = 0.7f,
                animationSpec = tween(400, easing = FastOutSlowInEasing)
            ) + fadeIn(tween(400)),
            exit = scaleOut(
                targetScale = 0.7f,
                animationSpec = tween(200)
            ) + fadeOut(tween(200)),
            modifier = Modifier
                .align(Alignment.Center)
                .padding(16.dp)
        ) {
            Surface(
                color = VibrantTeal.copy(alpha = 0.92f),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Tamamlandı",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

internal fun Color.luminance(): Float {
    return 0.2126f * red + 0.7152f * green + 0.0722f * blue
}