package com.liquidglass.sample

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liquidglass.GlassButton
import com.liquidglass.GlassContainer
import com.liquidglass.GlassIcon
import com.liquidglass.GlassIconButton
import com.liquidglass.GlassScreen
import com.liquidglass.GlassShape
import com.liquidglass.GlassStyle
import com.liquidglass.GlassSurface
import com.liquidglass.GlassTabBar
import com.liquidglass.GlassTabItem
import com.liquidglass.GlassText
import kotlin.math.roundToInt

private val SystemBlue = Color(0xFF0A84FF)

private val Tabs = listOf(
    GlassTabItem("Home", SampleIcons.Home),
    GlassTabItem("Shorts", SampleIcons.Play),
    GlassTabItem("My List", SampleIcons.Library),
    GlassTabItem("Profile", SampleIcons.Person)
)

@Composable
fun SampleApp(initialScroll: Int = 0) {
    GlassScreen(
        modifier = Modifier.fillMaxSize(),
        background = {
            Wallpaper(Modifier.fillMaxSize().background(Color(0xFFF2F2F7)), initialScroll)
        }
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            GlassSurface(
                modifier = Modifier.fillMaxWidth().height(132.dp),
                shape = GlassShape.Card,
                contentAlignment = Alignment.CenterStart
            ) {
                Column(Modifier.padding(horizontal = 24.dp)) {
                    GlassText("Liquid Glass", style = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold))
                    Spacer(Modifier.height(6.dp))
                    GlassText("Refraction, dispersion, adaptive tint and tilt-driven specular highlights.", style = TextStyle(fontSize = 14.sp))
                }
            }
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GlassButton(onClick = {}, modifier = Modifier.weight(1f).height(52.dp)) {
                    GlassText("Regular", style = ButtonText)
                }
                GlassButton(onClick = {}, modifier = Modifier.weight(1f).height(52.dp), style = GlassStyle.Clear) {
                    GlassText("Clear", style = ButtonText)
                }
                GlassButton(onClick = {}, modifier = Modifier.weight(1f).height(52.dp), style = GlassStyle.Regular.tint(SystemBlue)) {
                    GlassText("Tinted", style = ButtonText, color = Color.White)
                }
            }
            Spacer(Modifier.weight(1f))
            MorphMenu()
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                var selected by remember { mutableIntStateOf(0) }
                GlassTabBar(
                    items = Tabs,
                    selectedIndex = selected,
                    onTabSelected = { selected = it },
                    modifier = Modifier.weight(1f)
                )
                GlassIconButton(onClick = {}, icon = SampleIcons.Search, contentDescription = "Search", size = 64.dp, style = GlassStyle.Bar)
            }
        }
    }
}

private val ButtonText = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold)

@Composable
private fun MorphMenu() {
    var expanded by remember { mutableStateOf(false) }
    val progress by animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 180f)
    )
    val actions = listOf(SampleIcons.Star to "Favourite", SampleIcons.Favorite to "Like", SampleIcons.Edit to "Edit")
    GlassContainer(
        modifier = Modifier.fillMaxWidth().height(72.dp),
        spacing = 16.dp,
        contentAlignment = Alignment.CenterEnd
    ) {
        actions.forEachIndexed { index, (icon, label) ->
            Box(
                Modifier
                    .offset { IntOffset(-((index + 1) * 80.dp.toPx() * progress).roundToInt() - 4.dp.roundToPx(), 0) }
                    .size(56.dp)
                    .graphicsLayer {
                        val s = 0.3f + 0.7f * progress.coerceIn(0f, 1f)
                        scaleX = s
                        scaleY = s
                    }
                    .glass(GlassShape.Circle, interactive = true)
                    .clickable(interactionSource = null, indication = null) {},
                contentAlignment = Alignment.Center
            ) {
                GlassIcon(
                    icon,
                    label,
                    modifier = Modifier.alpha(progress.coerceIn(0f, 1f)),
                    tint = if (icon == SampleIcons.Favorite) Color(0xFFFF375F) else Color.Unspecified
                )
            }
        }
        Box(
            Modifier
                .size(64.dp)
                .glass(GlassShape.Circle, interactive = true, tint = SystemBlue)
                .clickable(interactionSource = null, indication = null) { expanded = !expanded },
            contentAlignment = Alignment.Center
        ) {
            GlassIcon(SampleIcons.Add, if (expanded) "Close" else "More", modifier = Modifier.rotate(45f * progress), size = 28.dp, tint = Color.White)
        }
    }
}

@Composable
private fun Wallpaper(modifier: Modifier, initialScroll: Int) {
    val colors = listOf(
        Color(0xFFFF375F), Color(0xFFFF9F0A), Color(0xFFFFD60A), Color(0xFF30D158),
        Color(0xFF64D2FF), Color(0xFF0A84FF), Color(0xFF5E5CE6), Color(0xFFBF5AF2)
    )
    Column(modifier.verticalScroll(rememberScrollState(initialScroll))) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(420.dp)
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF5E5CE6), Color(0xFFFF375F), Color(0xFFFF9F0A))
                    )
                )
        ) {
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(
                    Brush.radialGradient(listOf(Color(0xFF64D2FF), Color.Transparent), Offset(size.width * 0.2f, size.height * 0.3f), size.width * 0.5f),
                    size.width * 0.5f,
                    Offset(size.width * 0.2f, size.height * 0.3f)
                )
                drawCircle(
                    Brush.radialGradient(listOf(Color(0xFFFFD60A), Color.Transparent), Offset(size.width * 0.85f, size.height * 0.75f), size.width * 0.45f),
                    size.width * 0.45f,
                    Offset(size.width * 0.85f, size.height * 0.75f)
                )
                val step = 18.dp.toPx()
                var x = 0f
                while (x < size.width) {
                    drawLine(Color.White.copy(alpha = 0.35f), Offset(x, size.height * 0.55f), Offset(x, size.height), 2.dp.toPx())
                    x += step
                }
            }
            BasicText(
                "Drama\nShorts",
                modifier = Modifier.align(Alignment.BottomStart).padding(24.dp),
                style = TextStyle(color = Color.White, fontSize = 56.sp, fontWeight = FontWeight.Black, lineHeight = 56.sp)
            )
        }
        repeat(16) { index ->
            val color = colors[index % colors.size]
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .height(96.dp)
                    .background(Color.White, RoundedCornerShape(20.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(72.dp)
                        .background(
                            Brush.linearGradient(listOf(color, colors[(index + 3) % colors.size])),
                            RoundedCornerShape(16.dp)
                        )
                )
                Spacer(Modifier.width(14.dp))
                Column {
                    BasicText(
                        "Episode ${index + 1}",
                        style = TextStyle(color = Color.Black, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    )
                    BasicText(
                        "A short drama about glass, light and refraction.",
                        style = TextStyle(color = Color(0xFF3C3C43), fontSize = 13.sp)
                    )
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(360.dp).background(Color(0xFF1C1C1E))) {
            BasicText(
                "Dark section",
                modifier = Modifier.align(Alignment.Center),
                style = TextStyle(color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
            )
        }
        Spacer(Modifier.height(200.dp))
    }
}
