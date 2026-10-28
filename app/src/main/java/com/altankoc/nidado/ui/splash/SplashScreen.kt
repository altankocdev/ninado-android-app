package com.altankoc.nidado.ui.splash

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.altankoc.nidado.R
import com.altankoc.nidado.ui.theme.*
import com.altankoc.nidado.util.UiEvent
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.ui.focus.onFocusChanged

// ════════════════════════════════════════════════════════════
//  NinaDo · Splash
//  Tasarım katmanı (UI only). Veri / domain / state akışına
//  dokunulmamıştır.
// ════════════════════════════════════════════════════════════

@Composable
fun SplashScreen(
    navController: NavController,
    viewModel: SplashViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val nameInput by viewModel.nameInput.collectAsStateWithLifecycle()
    val nameError by viewModel.nameError.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val density = LocalDensity.current
    val isKeyboardOpen = WindowInsets.ime.getBottom(density) > 0

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is UiEvent.Navigate -> navController.navigate(event.route) {
                    popUpTo(0) { inclusive = true }
                }
                else -> Unit
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(
                    colorStops = arrayOf(
                        0.0f to SoftTeal,
                        0.28f to VibrantTeal,
                        0.66f to DeepOcean,
                        1.0f to DeepOcean
                    ),
                    start = Offset.Zero,
                    end = Offset.Infinite
                )
            )
    ) {
        // ── Arka plan katmanları ──────────────────────────────
        AuroraBackground()
        FloatingDecor()
        StarDust()
        VignetteOverlay()

        // ── İçerik ────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding(),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = state,
                transitionSpec = {
                    (
                            fadeIn(tween(700, easing = FastOutSlowInEasing)) +
                                    scaleIn(
                                        initialScale = 0.92f,
                                        animationSpec = tween(760, easing = FastOutSlowInEasing)
                                    ) +
                                    slideInVertically(
                                        animationSpec = tween(760, easing = FastOutSlowInEasing)
                                    ) { it / 12 }
                            ) togetherWith (
                            fadeOut(tween(440, easing = FastOutSlowInEasing)) +
                                    scaleOut(
                                        targetScale = 1.05f,
                                        animationSpec = tween(540, easing = FastOutSlowInEasing)
                                    )
                            ) using SizeTransform(clip = false)
                },
                label = "splash_content",
                contentAlignment = Alignment.Center
            ) { currentState ->
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    when (currentState) {
                        is SplashState.Background -> {}
                        is SplashState.WelcomeIntro -> BigTextContent(
                            text = "NinaDo'ya\nHoşgeldiniz",
                            showLogo = true
                        )
                        is SplashState.Greeting -> BigTextContent(
                            text = "Merhaba\n${currentState.name}",
                            showLogo = true
                        )
                        is SplashState.NamePrompt -> BigTextContent("Lütfen isminizi\ngiriniz")
                        is SplashState.NameInput -> NameInputContent(
                            nameInput = nameInput,
                            nameError = nameError,
                            onNameChanged = viewModel::onNameChanged,
                            onConfirm = {
                                focusManager.clearFocus()
                                viewModel.onNameConfirmed()
                            }
                        )
                        is SplashState.Loading -> LoadingContent()
                        is SplashState.Welcome -> BigTextContent(
                            text = "Hoşgeldin\n${currentState.name}",
                            showLogo = true
                        )
                    }
                }
            }
        }

        // ── Alt marka yazısı ──────────────────────────────────
        AnimatedVisibility(
            visible = !isKeyboardOpen,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            BrandFooter(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp)
            )
        }
    }
}

// ════════════════════════════════════════════════════════════
//  AURORA ARKA PLAN
//  Yavaşça süzülen, nefes alan ışık kümeleri + üstten parlama.
// ════════════════════════════════════════════════════════════
@Composable
private fun AuroraBackground() {
    val transition = rememberInfiniteTransition(label = "aurora")

    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "drift"
    )
    val drift2 by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(27000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "drift2"
    )
    val breathe by transition.animateFloat(
        initialValue = 0.80f,
        targetValue = 1.20f,
        animationSpec = infiniteRepeatable(
            animation = tween(6500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathe"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Üstten gelen geniş, yumuşak parlama
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(FrostBlue.copy(alpha = 0.18f), Color.Transparent),
                center = Offset(w * 0.5f, h * -0.08f),
                radius = w * 1.2f
            ),
            size = size
        )

        // Sol üst köşe vurgu ışığı
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(White.copy(alpha = 0.13f), Color.Transparent),
                center = Offset(w * 0.06f, h * 0.04f),
                radius = w * 0.85f
            ),
            size = size
        )

        // Aurora kümeleri — birden çok katman, farklı hız ve yön
        drawBlob(
            color = SoftTeal.copy(alpha = 0.34f),
            center = Offset(
                w * (0.24f + 0.13f * sin(drift)),
                h * (0.22f + 0.08f * cos(drift * 0.9f))
            ),
            radius = w * 0.60f * breathe
        )
        drawBlob(
            color = FrostBlue.copy(alpha = 0.16f),
            center = Offset(
                w * (0.86f + 0.10f * cos(drift * 1.1f)),
                h * (0.26f + 0.09f * sin(drift))
            ),
            radius = w * 0.50f * (2f - breathe)
        )
        drawBlob(
            color = VibrantTeal.copy(alpha = 0.32f),
            center = Offset(
                w * (0.74f + 0.12f * sin(drift2 * 0.8f)),
                h * (0.80f + 0.07f * cos(drift2))
            ),
            radius = w * 0.66f * breathe
        )
        drawBlob(
            color = DeepOcean.copy(alpha = 0.30f),
            center = Offset(
                w * (0.14f + 0.09f * cos(drift2)),
                h * (0.90f + 0.05f * sin(drift2 * 1.2f))
            ),
            radius = w * 0.54f * (2f - breathe)
        )
        // Merkeze yakın, ince bir sıcak nokta
        drawBlob(
            color = SoftTeal.copy(alpha = 0.14f),
            center = Offset(
                w * (0.5f + 0.06f * sin(drift * 1.3f)),
                h * (0.52f + 0.05f * cos(drift2 * 1.1f))
            ),
            radius = w * 0.40f * breathe
        )
    }
}

private fun DrawScope.drawBlob(color: Color, center: Offset, radius: Float) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color, Color.Transparent),
            center = center,
            radius = radius
        ),
        radius = radius,
        center = center
    )
}

// ════════════════════════════════════════════════════════════
//  YÜZEN DEKOR — todo temalı, çok düşük opaklıkta yumuşak
//  onay-çemberleri arka planda usulca süzülür.
// ════════════════════════════════════════════════════════════
@Composable
private fun FloatingDecor() {
    val transition = rememberInfiniteTransition(label = "decor")

    val d1 by transition.driftFloat(14000, 0)
    val d2 by transition.driftFloat(18000, 3000)
    val d3 by transition.driftFloat(16000, 6000)
    val d4 by transition.driftFloat(20000, 1500)

    val sway by transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(7000, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "sway"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        data class Decor(val x: Float, val y: Float, val r: Float, val a: Float)

        val items = listOf(
            Decor(0.18f, d1, 26.dp.toPx(), 0.06f),
            Decor(0.82f, d2, 34.dp.toPx(), 0.05f),
            Decor(0.30f, d3, 18.dp.toPx(), 0.07f),
            Decor(0.70f, d4, 22.dp.toPx(), 0.05f)
        )

        items.forEach { item ->
            val cx = w * (item.x + 0.02f * sway)
            val cy = h * item.y
            // İnce çember
            drawCircle(
                color = White.copy(alpha = item.a),
                radius = item.r,
                center = Offset(cx, cy),
                style = Stroke(width = 1.6.dp.toPx())
            )
            // İçinde minik onay işareti
            val s = item.r * 0.9f
            val path = Path().apply {
                moveTo(cx - s * 0.42f, cy + s * 0.02f)
                lineTo(cx - s * 0.10f, cy + s * 0.34f)
                lineTo(cx + s * 0.46f, cy - s * 0.34f)
            }
            drawPath(
                path = path,
                color = White.copy(alpha = item.a * 1.3f),
                style = Stroke(
                    width = 1.6.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
    }
}

// ════════════════════════════════════════════════════════════
//  YILDIZ TOZU — yavaşça yükselen ve sönüp yanan parçacıklar.
// ════════════════════════════════════════════════════════════
@Composable
private fun StarDust() {
    val transition = rememberInfiniteTransition(label = "stardust")

    val p1 by transition.particleFloat(6000, 0)
    val p2 by transition.particleFloat(8000, 2000)
    val p3 by transition.particleFloat(7000, 1000)
    val p4 by transition.particleFloat(9000, 3500)
    val p5 by transition.particleFloat(7500, 800)
    val p6 by transition.particleFloat(8500, 2600)
    val p7 by transition.particleFloat(6800, 4200)
    val p8 by transition.particleFloat(9500, 1200)

    val twinkle by transition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(2200, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "twinkle"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        data class P(val x: Float, val y: Float, val r: Float, val tw: Boolean)

        val particles = listOf(
            P(0.14f, p1, 3.4.dp.toPx(), true),
            P(0.32f, p2, 2.4.dp.toPx(), false),
            P(0.50f, p3, 4.0.dp.toPx(), true),
            P(0.66f, p4, 2.8.dp.toPx(), false),
            P(0.80f, p5, 2.4.dp.toPx(), true),
            P(0.92f, p6, 3.2.dp.toPx(), false),
            P(0.22f, p7, 2.0.dp.toPx(), true),
            P(0.58f, p8, 2.6.dp.toPx(), false)
        )

        particles.forEachIndexed { index, p ->
            val edgeFade = when {
                p.y < 0.12f -> p.y * 8f
                p.y > 0.88f -> (1f - p.y) * 8f
                else -> 1f
            }.coerceIn(0f, 1f)

            val twinkleFactor = if (p.tw) twinkle else 1f
            val a = (0.42f * edgeFade * twinkleFactor).coerceIn(0f, 0.5f)
            val color = if (index % 2 == 0) White else FrostBlue

            // Yumuşak hale
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color.copy(alpha = a * 0.6f), Color.Transparent),
                    center = Offset(w * p.x, h * p.y),
                    radius = p.r * 3.2f
                ),
                radius = p.r * 3.2f,
                center = Offset(w * p.x, h * p.y)
            )
            // Çekirdek
            drawCircle(
                color = color.copy(alpha = a),
                radius = p.r,
                center = Offset(w * p.x, h * p.y)
            )
        }
    }
}

// ════════════════════════════════════════════════════════════
//  VIGNETTE — üst/alt köşelerde hafif kararma ile derinlik.
// ════════════════════════════════════════════════════════════
@Composable
private fun VignetteOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.0f to DeepOcean.copy(alpha = 0.28f),
                        0.28f to Color.Transparent,
                        0.74f to Color.Transparent,
                        1.0f to DeepOcean.copy(alpha = 0.38f)
                    )
                )
            )
    )
}

// ── Animasyon yardımcıları ──────────────────────────────────
@Composable
private fun InfiniteTransition.particleFloat(durationMillis: Int, delayMillis: Int) =
    animateFloat(
        initialValue = 1.12f,
        targetValue = -0.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, delayMillis, LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "particle"
    )

@Composable
private fun InfiniteTransition.driftFloat(durationMillis: Int, delayMillis: Int) =
    animateFloat(
        initialValue = 1.15f,
        targetValue = -0.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, delayMillis, LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "drift_decor"
    )

// ════════════════════════════════════════════════════════════
//  LOGO — kutusuz / karesiz, serbest.
//  • Hale çok katmanlı radial gradient (blur YOK → kare yok,
//    her API seviyesinde dairesel ve yumuşak).
//  • Hale `scale` ile büyür → layout'ta yer kaplamaz, logoyu
//    küçültmez ve yazıyla arayı açmaz.
//  • Etrafında nefes alan ince halka + yörüngede dönen
//    minik parçacıklar.
// ════════════════════════════════════════════════════════════
@Composable
private fun LogoMark(size: Dp = 150.dp) {
    val infinite = rememberInfiniteTransition(label = "logo")

    // Sadece çok hafif süzülme — tek animasyon, ekranı yormaz
    val floatY by infinite.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            tween(4200, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "logo_float"
    )

    // Girişte yumuşak büyüme + fade
    val enter = remember { Animatable(0.85f) }
    val enterAlpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        enterAlpha.animateTo(1f, tween(550, easing = FastOutSlowInEasing))
    }
    LaunchedEffect(Unit) {
        enter.animateTo(
            targetValue = 1f,
            animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessLow)
        )
    }

    Box(
        modifier = Modifier
            .offset(y = floatY.dp)
            .size(size)
            .alpha(enterAlpha.value)
            .scale(enter.value),
        contentAlignment = Alignment.Center
    ) {
        // Beyaz hale / halka / parçacık YOK — sade ve net logo.
        Image(
            painter = painterResource(id = R.drawable.splash_icon),
            contentDescription = "NinaDo",
            modifier = Modifier.fillMaxSize()
        )
    }
}

// ════════════════════════════════════════════════════════════
//  BÜYÜK YAZI — logo + başlık sıkı bir grup.
//  Başlık satır satır belirir; dikey gradient + gölge ile
//  derinlik; arkasında nefes alan ışık süzmesi.
// ════════════════════════════════════════════════════════════
@Composable
private fun BigTextContent(text: String, showLogo: Boolean = false) {
    val infiniteTransition = rememberInfiniteTransition(label = "text_glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.14f,
        targetValue = 0.30f,
        animationSpec = infiniteRepeatable(
            tween(3200, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        // Logo ile yazı arası iyice sıkıldı
        verticalArrangement = Arrangement.spacedBy(if (showLogo) (-12).dp else 0.dp)
    ) {
        if (showLogo) {
            LogoMark(size = 158.dp)
        }

        Box(contentAlignment = Alignment.Center) {
            // Yazının arkasındaki yumuşak ışık süzmesi — yazıyı itmemesi için
            // küçültüldü ve matchParentSize ile yazının boyutuna sarıldı
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .scale(1.6f)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(White.copy(alpha = glowAlpha), Color.Transparent)
                        )
                    )
            )

            AnimatedTitle(text = text)
        }
    }
}

// Başlığı satır satır, hafif yukarı kayarak belirten yardımcı.
@Composable
private fun AnimatedTitle(text: String) {
    val lines = remember(text) { text.split("\n") }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 32.dp)
    ) {
        lines.forEachIndexed { index, line ->
            val appear = remember(text, index) { Animatable(0f) }
            LaunchedEffect(text, index) {
                appear.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = 620,
                        delayMillis = 120 + index * 150,
                        easing = FastOutSlowInEasing
                    )
                )
            }

            Text(
                text = line,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .alpha(appear.value)
                    .offset(y = ((1f - appear.value) * 18f).dp),
                style = TextStyle(
                    brush = Brush.verticalGradient(
                        colors = listOf(White, FrostBlue.copy(alpha = 0.90f))
                    ),
                    fontSize = 46.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 56.sp,
                    shadow = Shadow(
                        color = DeepOcean.copy(alpha = 0.45f),
                        offset = Offset(0f, 8f),
                        blurRadius = 26f
                    )
                )
            )
        }
    }
}

// ════════════════════════════════════════════════════════════
//  İSİM GİRİŞ — büyük serbest logo, sıkı başlık, odaklanınca
//  parlayan cam kart, shimmer'lı buton.
// ════════════════════════════════════════════════════════════
@Composable
private fun NameInputContent(
    nameInput: String,
    nameError: String?,
    onNameChanged: (String) -> Unit,
    onConfirm: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val focusRequester = remember { FocusRequester() }
    val density = LocalDensity.current
    val isKeyboardOpen = WindowInsets.ime.getBottom(density) > 0

    // Otomatik odaklanma ve kaydırma yönetimi
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    LaunchedEffect(isKeyboardOpen, scrollState.maxValue) {
        if (isKeyboardOpen) {
            scrollState.animateScrollTo(
                value = scrollState.maxValue,
                animationSpec = tween(500, easing = FastOutSlowInEasing)
            )
        }
    }

    val focusGlow by animateFloatAsState(
        targetValue = if (isFocused) 1f else 0f,
        animationSpec = tween(450, easing = FastOutSlowInEasing),
        label = "focus_glow"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(horizontal = 28.dp)
            .padding(bottom = 24.dp)
            .animateContentSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        AnimatedVisibility(
            visible = !isKeyboardOpen,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            LogoMark(size = 120.dp)
        }

        Text(
            text = "İsminiz nedir?",
            textAlign = TextAlign.Center,
            style = TextStyle(
                brush = Brush.verticalGradient(
                    colors = listOf(White, FrostBlue.copy(alpha = 0.90f))
                ),
                fontSize = if (isKeyboardOpen) 28.sp else 32.sp,
                fontWeight = FontWeight.SemiBold,
                shadow = Shadow(DeepOcean.copy(alpha = 0.40f), Offset(0f, 6f), 18f)
            )
        )

        AnimatedVisibility(
            visible = !isKeyboardOpen,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Text(
                text = "Sana özel bir deneyim için seni tanıyalım",
                textAlign = TextAlign.Center,
                color = OffWhite.copy(alpha = 0.62f),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 2.dp)
            )
        }

        // ── Cam kart ──────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(30.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            White.copy(alpha = 0.18f),
                            White.copy(alpha = 0.06f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            White.copy(alpha = 0.30f + 0.30f * focusGlow),
                            White.copy(alpha = 0.10f + 0.15f * focusGlow)
                        )
                    ),
                    shape = RoundedCornerShape(30.dp)
                )
                .drawBehind {
                    if (focusGlow > 0f) {
                        drawRect(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    White.copy(alpha = 0.10f * focusGlow),
                                    Color.Transparent
                                ),
                                center = Offset(size.width / 2f, 0f),
                                radius = size.width * 0.9f
                            )
                        )
                    }
                }
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            OutlinedTextField(
                value = nameInput,
                onValueChange = onNameChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onFocusChanged { isFocused = it.isFocused },
                placeholder = {
                    Text(
                        text = "Nida",
                        color = OffWhite.copy(alpha = 0.4f),
                        style = MaterialTheme.typography.bodyLarge
                    )
                },
                singleLine = true,
                isError = nameError != null,
                supportingText = nameError?.let { error ->
                    { Text(error, color = ErrorRedDark) }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onConfirm() }),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = OffWhite,
                    unfocusedTextColor = OffWhite,
                    focusedBorderColor = OffWhite.copy(alpha = 0.85f),
                    unfocusedBorderColor = OffWhite.copy(alpha = 0.30f),
                    cursorColor = White,
                    focusedContainerColor = White.copy(alpha = 0.10f),
                    unfocusedContainerColor = White.copy(alpha = 0.05f)
                )
            )

            ShimmerButton(onClick = onConfirm)
        }
    }
}



// ── Shimmer'lı "Devam Et" butonu ────────────────────────────
@Composable
private fun ShimmerButton(onClick: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val shimmerX by transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, delayMillis = 600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_x"
    )

    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .drawBehind {
                // Buton üzerinde sağa kayan ince parlama bandı
                val bandW = size.width * 0.35f
                val cx = size.width * shimmerX
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            White.copy(alpha = 0.35f),
                            Color.Transparent
                        ),
                        startX = cx - bandW,
                        endX = cx + bandW
                    )
                )
            },
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = OffWhite,
            contentColor = DeepOcean
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Devam Et",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Canvas(modifier = Modifier.size(18.dp)) {
                val path = Path().apply {
                    moveTo(size.width * 0.35f, size.height * 0.24f)
                    lineTo(size.width * 0.66f, size.height * 0.50f)
                    lineTo(size.width * 0.35f, size.height * 0.76f)
                }
                drawPath(
                    path = path,
                    color = DeepOcean,
                    style = Stroke(
                        width = 2.6.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
        }
    }
}

// ════════════════════════════════════════════════════════════
//  YÜKLENİYOR — özel çift halkalı, nefes alan loader.
// ════════════════════════════════════════════════════════════
@Composable
private fun LoadingContent() {
    val transition = rememberInfiniteTransition(label = "loading")

    val glow by transition.animateFloat(
        initialValue = 0.18f,
        targetValue = 0.42f,
        animationSpec = infiniteRepeatable(
            tween(1500, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "loading_glow"
    )
    val rotateOuter by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            tween(1400, easing = LinearEasing),
            RepeatMode.Restart
        ),
        label = "rotate_outer"
    )
    val rotateInner by transition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            tween(2000, easing = LinearEasing),
            RepeatMode.Restart
        ),
        label = "rotate_inner"
    )

    Box(contentAlignment = Alignment.Center) {
        // Arka hale
        Box(
            modifier = Modifier
                .size(140.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(White.copy(alpha = glow), Color.Transparent)
                    )
                )
        )

        // Dış halka — bir yöne döner
        Canvas(
            modifier = Modifier
                .size(64.dp)
                .rotate(rotateOuter)
        ) {
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color.Transparent,
                        White.copy(alpha = 0.9f),
                        Color.Transparent
                    )
                ),
                startAngle = 0f,
                sweepAngle = 270f,
                useCenter = false,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        // İç halka — ters yöne döner
        Canvas(
            modifier = Modifier
                .size(40.dp)
                .rotate(rotateInner)
        ) {
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color.Transparent,
                        FrostBlue.copy(alpha = 0.9f),
                        Color.Transparent
                    )
                ),
                startAngle = 0f,
                sweepAngle = 220f,
                useCenter = false,
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
            )
        }
    }
}

// ════════════════════════════════════════════════════════════
//  ALT MARKA YAZISI — iki yanında ince ışık çizgileri.
// ════════════════════════════════════════════════════════════
@Composable
private fun BrandFooter(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .width(28.dp)
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(Color.Transparent, OffWhite.copy(alpha = 0.4f))
                    )
                )
        )
        Text(
            text = "N I N A D O",
            style = MaterialTheme.typography.labelSmall,
            color = OffWhite.copy(alpha = 0.45f),
            letterSpacing = 7.sp
        )
        Box(
            modifier = Modifier
                .width(28.dp)
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(OffWhite.copy(alpha = 0.4f), Color.Transparent)
                    )
                )
        )
    }
}