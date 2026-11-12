package br.com.carrinhocerto.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val Forest = Color(0xFF115C48)
val Mint = Color(0xFFBCF2D4)
private val Day = lightColorScheme(
    primary = Forest, onPrimary = Color.White, primaryContainer = Color(0xFFDDF3E7), onPrimaryContainer = Color(0xFF093D2E),
    secondary = Color(0xFF52685C), secondaryContainer = Color(0xFFE6EEE8), onSecondaryContainer = Color(0xFF243C2F),
    background = Color(0xFFF4F7F5), onBackground = Color(0xFF172C23),
    surface = Color.White, onSurface = Color(0xFF172C23), surfaceVariant = Color(0xFFEAF0EC), onSurfaceVariant = Color(0xFF5B6D62),
    outline = Color(0xFF7A8B80), outlineVariant = Color(0xFFDCE5DE), error = Color(0xFFBA3030)
)
private val Night = darkColorScheme(
    primary = Mint, onPrimary = Color(0xFF073B2C), primaryContainer = Color(0xFF1F4F3D), onPrimaryContainer = Mint,
    secondary = Color(0xFFB5CCBD), secondaryContainer = Color(0xFF2E4437), onSecondaryContainer = Color(0xFFD9EBDD),
    background = Color(0xFF101B16), onBackground = Color(0xFFE3EFE7),
    surface = Color(0xFF19271F), onSurface = Color(0xFFE3EFE7), surfaceVariant = Color(0xFF28392E), onSurfaceVariant = Color(0xFFB2C5B7),
    outline = Color(0xFF8B9F91), outlineVariant = Color(0xFF34483B), error = Color(0xFFFFB4AB)
)
@Composable fun CarrinhoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) Night else Day,
        typography = Typography(
            headlineLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 36.sp, lineHeight = 42.sp, letterSpacing = (-1).sp),
            titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp),
            titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
            bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
            bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
            bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 18.sp),
            labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp)
        ),
        shapes = Shapes(extraSmall = RoundedCornerShape(12.dp), small = RoundedCornerShape(12.dp), medium = RoundedCornerShape(18.dp), large = RoundedCornerShape(24.dp), extraLarge = RoundedCornerShape(28.dp)),
        content = content
    )
}

enum class Symbol(val drawing: String) {
    Cart("M3 3H5L7.5 15H19L21 7H6 M9 20H9.01 M18 20H18.01"),
    Scan("M3 8V4H7 M17 4H21V8 M21 16V20H17 M7 20H3V16 M7 8V16 M10 7V17 M14 7V17 M17 8V16"),
    Store("M3 10L5 4H19L21 10 M3 10C3 13 7 13 7 10C7 13 12 13 12 10C12 13 17 13 17 10C17 13 21 13 21 10 M5 13V21H19V13 M10 21V16H14V21"),
    Shield("M12 3L20 6V11C20 16 16 20 12 22C8 20 4 16 4 11V6Z M8 12L11 15L16 9"),
    Clock("M21 12A9 9 0 1 1 3 12A9 9 0 1 1 21 12 M12 7V12L15 14"),
    Box("M3 7L12 3L21 7V17L12 21L3 17Z M3 7L12 11L21 7 M12 11V21 M7 5L16 9"),
    Chevron("M9 5L16 12L9 19"),
    Plus("M12 5V19 M5 12H19"), Minus("M5 12H19"),
    Delete("M4 7H20 M9 7V4H15V7 M6 7L7 21H17L18 7 M10 11V17 M14 11V17"),
    Search("M16 16L21 21 M18 10A8 8 0 1 1 2 10A8 8 0 1 1 18 10"),
    Flash("M13 2L4 14H11L10 22L20 9H13Z"),
    Camera("M3 7H7L9 4H15L17 7H21V20H3Z M16 13A4 4 0 1 1 8 13A4 4 0 1 1 16 13"),
    Keyboard("M3 5H21V19H3Z M7 9H7.01 M11 9H11.01 M15 9H15.01 M18 9H18.01 M7 12H7.01 M11 12H11.01 M15 12H15.01 M18 12H18.01 M8 16H16"),
    Check("M5 12L10 17L20 6"),
    Wallet("M20 8V5H4V20H20V16 M4 5V3H17V5 M15 9H22V16H15Z M18 12H18.01"),
    Edit("M15 4L20 9 M4 20L9 19L21 7L17 3L5 15Z"),
    Close("M6 6L18 18 M6 18L18 6")
}
private val vectors = Symbol.entries.associateWith { symbol ->
    ImageVector.Builder(symbol.name, 24.dp, 24.dp, 24f, 24f).addPath(
        pathData = PathParser().parsePathString(symbol.drawing).toNodes(), fill = null, stroke = SolidColor(Color.Black),
        strokeLineWidth = 1.8f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round
    ).build()
}
@Composable fun AppIcon(symbol: Symbol, description: String? = null, modifier: Modifier = Modifier.size(24.dp), tint: Color = LocalContentColor.current) {
    Icon(vectors.getValue(symbol), description, modifier, tint)
}
@Composable fun EmptyState(symbol: Symbol, title: String, text: String, modifier: Modifier = Modifier, compact: Boolean = false) {
    if (compact) {
        Surface(modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(42.dp).background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) { AppIcon(symbol, tint = MaterialTheme.colorScheme.primary) }
                Column(Modifier.weight(1f)) { Text(title, style = MaterialTheme.typography.titleMedium); Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
        return
    }
    Column(modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.size(60.dp).background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(20.dp)), contentAlignment = Alignment.Center) { AppIcon(symbol, modifier = Modifier.size(28.dp), tint = MaterialTheme.colorScheme.primary) }
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}
