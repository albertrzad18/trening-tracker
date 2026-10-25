package io.github.albertrzad18.treningtracker.ui.common

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

import io.github.albertrzad18.treningtracker.R
import io.github.albertrzad18.treningtracker.domain.Discipline
import io.github.albertrzad18.treningtracker.domain.PeriodFilter
import io.github.albertrzad18.treningtracker.domain.SummaryGranularity
import io.github.albertrzad18.treningtracker.ui.format.ValueUnit
import io.github.albertrzad18.treningtracker.ui.format.formatPercentAbs
import io.github.albertrzad18.treningtracker.ui.theme.LocalTtColors
import io.github.albertrzad18.treningtracker.ui.theme.PlexSans
import io.github.albertrzad18.treningtracker.ui.theme.TtMotion
import io.github.albertrzad18.treningtracker.ui.theme.TtShape
import io.github.albertrzad18.treningtracker.ui.theme.TtSpace
import io.github.albertrzad18.treningtracker.ui.theme.TtType
import io.github.albertrzad18.treningtracker.ui.theme.ttDuration

/**
 * Wspólne klocki interfejsu: panel, nagłówek sekcji, siatka metryk, chipy, przyciski, szkielety.
 * Wszystkie biorą kolory z [LocalTtColors], więc jasny i ciemny motyw wychodzą same.
 */

/** Kliknięcie z nakładką ink 8% (bez cienia i bez skalowania). */
@Composable
fun Modifier.ttClickable(enabled: Boolean = true, onClick: () -> Unit): Modifier {
    val interaction = remember { MutableInteractionSource() }
    val ink = LocalTtColors.current.ink
    return this.clickable(
        interactionSource = interaction,
        indication = ripple(color = ink),
        enabled = enabled,
        onClick = onClick,
    )
}

/** Panel: biała (albo ciemna) płyta na tle ekranu. Zamiast cienia używamy koloru `surface`. */
@Composable
fun Panel(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(TtSpace.panelPadding),
    shape: Shape = TtShape.panel,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = LocalTtColors.current
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface)
            .padding(padding),
        content = content,
    )
}

/** Linia 1 dp w kolorze `line`: rozdziela wiersze w panelu. */
@Composable
fun TtDivider(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(LocalTtColors.current.line))
}

/** Tytuł sekcji w panelu, z opcjonalną zawartością po prawej. */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = TtType.title, color = LocalTtColors.current.ink, modifier = Modifier.weight(1f))
        trailing?.invoke()
    }
}

/** Etykieta WERSALIKAMI (`label`): podpis nad liczbą, nazwa kolumny, zakres tygodnia. */
@Composable
fun LabelText(text: String, modifier: Modifier = Modifier, color: Color = LocalTtColors.current.ink2) {
    Text(text.uppercase(), style = TtType.label, color = color, modifier = modifier)
}

/**
 * Duża liczba z małą jednostką obok: jednostka ma ~40% stopnia liczby, kolor ink2 i 4 dp odstępu.
 * Obie części leżą na wspólnej linii pisma (`alignByBaseline`).
 */
@Composable
fun BigValue(
    value: String,
    unit: String? = null,
    style: TextStyle = TtType.hero,
    color: Color = LocalTtColors.current.ink,
    modifier: Modifier = Modifier,
) {
    Row(modifier) {
        Text(value, style = style, color = color, modifier = Modifier.alignByBaseline())
        if (unit != null) {
            Text(
                unit,
                style = TextStyle(
                    fontFamily = PlexSans,
                    fontSize = style.fontSize * 0.4f,
                    fontWeight = FontWeight.Medium,
                    fontFeatureSettings = "tnum",
                ),
                color = LocalTtColors.current.ink2,
                modifier = Modifier.alignByBaseline().padding(start = TtSpace.x1),
            )
        }
    }
}

@Composable
fun BigValue(
    parts: ValueUnit,
    style: TextStyle = TtType.hero,
    color: Color = LocalTtColors.current.ink,
    modifier: Modifier = Modifier,
) = BigValue(parts.value, parts.unit, style, color, modifier)

/** Jedna metryka w siatce: etykieta, wartość i (jeśli jest) jednostka. */
data class MetricItem(val label: String, val value: String, val unit: String? = null)

/** Siatka metryk: dwie kolumny, wiersze rozdzielone linią 1 dp. Metryki bez danych w ogóle nie trafiają na listę. */
@Composable
fun MetricGrid(items: List<MetricItem>, modifier: Modifier = Modifier) {
    if (items.isEmpty()) return
    Column(modifier.fillMaxWidth()) {
        items.chunked(2).forEachIndexed { index, row ->
            if (index > 0) TtDivider(Modifier.padding(vertical = TtSpace.x3))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(TtSpace.x3)) {
                row.forEach { item ->
                    Column(Modifier.weight(1f)) {
                        LabelText(item.label)
                        Spacer(Modifier.height(2.dp))
                        BigValue(item.value, item.unit, style = TtType.value)
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

/**
 * Pigułka ze zmianą: strzałka w górę albo w dół, pogrubiony procent i opis.
 * Kolor zawsze ink (zielony znaczy „rower”, więc nie oznaczamy nim wzrostu).
 */
@Composable
fun DeltaBadge(
    percent: Double,
    text: String? = null,
    label: String? = null,
    modifier: Modifier = Modifier,
) {
    val colors = LocalTtColors.current
    Row(
        modifier
            .clip(TtShape.full)
            .background(colors.track)
            .padding(horizontal = TtSpace.x3, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TtSpace.x1),
    ) {
        if (label != null) Text(label, style = TtType.bodyS, color = colors.ink2)
        Icon(
            if (percent < 0) TtIcons.Down else TtIcons.Up,
            contentDescription = null,
            tint = colors.ink,
            modifier = Modifier.size(18.dp),
        )
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(formatPercentAbs(percent)) }
                if (text != null) append(" $text")
            },
            style = TtType.bodyS,
            color = colors.ink,
        )
    }
}

/** Opis zmiany dla pigułki: „mniej / więcej niż poprzedni tydzień” (albo miesiąc). */
@Composable
fun deltaPeriodText(percent: Double, period: SummaryGranularity): String = stringResource(
    when {
        period == SummaryGranularity.DAY -> if (percent < 0) R.string.delta_less_day else R.string.delta_more_day
        period == SummaryGranularity.MONTH && percent < 0 -> R.string.delta_less_month
        period == SummaryGranularity.MONTH -> R.string.delta_more_month
        percent < 0 -> R.string.delta_less_week
        else -> R.string.delta_more_week
    },
)

/** Opis zmiany dla pigułki w szczegółach treningu: „poniżej / powyżej średniej”. */
@Composable
fun deltaAverageText(percent: Double): String =
    stringResource(if (percent < 0) R.string.delta_below_average else R.string.delta_above_average)

/** Przełącznik segmentowy: tor `track`, wybrany segment w akcencie. */
@Composable
fun <T> SegmentedToggle(
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalTtColors.current
    Row(
        modifier
            .fillMaxWidth()
            .clip(TtShape.full)
            .background(colors.track)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            Box(
                Modifier
                    .weight(1f)
                    .height(36.dp)
                    .clip(TtShape.full)
                    .background(if (isSelected) colors.accent else Color.Transparent)
                    .ttClickable { onSelect(option) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label(option),
                    style = TtType.labelPlain,
                    color = if (isSelected) colors.onAccent else colors.ink,
                )
            }
        }
    }
}

/** Chip: obrys `line`, wysokość 36. Wariant zaznaczony dostaje własne tło i obrys. */
@Composable
fun TtChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    background: Color = Color.Transparent,
    border: Color = LocalTtColors.current.line,
    textColor: Color = LocalTtColors.current.ink,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    paddingStart: Dp = 14.dp,
    paddingEnd: Dp = 14.dp,
) {
    Row(
        modifier
            .height(36.dp)
            .clip(TtShape.full)
            .background(background)
            .border(1.dp, border, TtShape.full)
            .ttClickable(onClick = onClick)
            .padding(start = paddingStart, end = paddingEnd),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TtSpace.x2),
    ) {
        leading?.invoke()
        Text(label, style = TtType.labelPlain, color = textColor, maxLines = 1)
        trailing?.invoke()
    }
}

/** Chipy dyscyplin (wielokrotny wybór): zaznaczony ma tło i obrys w kolorze dyscypliny. */
@Composable
fun DisciplineFilterChips(
    selected: Set<Discipline>,
    onToggle: (Discipline) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalTtColors.current
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(TtSpace.x2)) {
        Discipline.entries.forEach { discipline ->
            val isSelected = discipline in selected
            TtChip(
                label = stringResource(discipline.labelRes()),
                onClick = { onToggle(discipline) },
                selected = isSelected,
                background = if (isSelected) colors.container(discipline) else Color.Transparent,
                border = if (isSelected) colors.of(discipline) else colors.line,
                leading = { DisciplineDot(discipline) },
                paddingStart = TtSpace.x3,
                paddingEnd = TtSpace.x3,
            )
        }
    }
}

/** Chip okresu z rozwijanym menu: `Wszystko ▾`. */
@Composable
fun PeriodMenuChip(
    selected: PeriodFilter,
    onSelect: (PeriodFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalTtColors.current
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        TtChip(
            label = stringResource(selected.labelRes()),
            onClick = { open = true },
            paddingEnd = 10.dp,
            trailing = {
                Icon(TtIcons.DropDown, contentDescription = null, tint = colors.ink, modifier = Modifier.size(20.dp))
            },
        )
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            shape = RoundedCornerShape(16.dp),
            containerColor = colors.surface,
        ) {
            PeriodFilter.entries.forEach { period ->
                DropdownMenuItem(
                    text = {
                        Text(
                            stringResource(period.labelRes()),
                            style = TtType.body.copy(
                                fontWeight = if (period == selected) FontWeight.SemiBold else FontWeight.Normal,
                            ),
                            color = colors.ink,
                        )
                    },
                    trailingIcon = if (period == selected) {
                        {
                            Icon(
                                TtIcons.Check,
                                contentDescription = null,
                                tint = colors.ink,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    } else {
                        null
                    },
                    onClick = {
                        onSelect(period)
                        open = false
                    },
                )
            }
        }
    }
}

/** Przycisk główny: wysokość 52, akcent, pełne zaokrąglenie. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
) {
    val colors = LocalTtColors.current
    Row(
        modifier
            .height(52.dp)
            .clip(TtShape.full)
            .background(colors.accent)
            .ttClickable(onClick = onClick)
            .padding(horizontal = TtSpace.x6),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TtSpace.x2, Alignment.CenterHorizontally),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = colors.onAccent, modifier = Modifier.size(20.dp))
        }
        Text(text, style = TtType.title, color = colors.onAccent, maxLines = 1)
    }
}

/** Przycisk pomocniczy: obrys `line`, wysokość 48 (albo 36 dla wariantu `small`). */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    small: Boolean = false,
    contentColor: Color = LocalTtColors.current.ink,
) {
    val colors = LocalTtColors.current
    Row(
        modifier
            .height(if (small) 36.dp else TtSpace.minTouch)
            .clip(TtShape.full)
            .border(1.dp, colors.line, TtShape.full)
            .ttClickable(onClick = onClick)
            .padding(horizontal = if (small) 14.dp else TtSpace.x5),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TtSpace.x2, Alignment.CenterHorizontally),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(18.dp))
        }
        Text(
            text,
            style = if (small) TtType.labelPlain else TtType.body.copy(fontWeight = FontWeight.Medium),
            color = contentColor,
            maxLines = 1,
        )
    }
}

/** Klikalny wiersz „Zobacz podsumowania →” na dole panelu. */
@Composable
fun LinkRow(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    divider: Boolean = true,
) {
    val colors = LocalTtColors.current
    Column(modifier.fillMaxWidth()) {
        if (divider) TtDivider()
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = TtSpace.minTouch)
                .ttClickable(onClick = onClick),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(text, style = TtType.bodyS.copy(fontWeight = FontWeight.SemiBold), color = colors.ink)
            Icon(TtIcons.Forward, contentDescription = null, tint = colors.ink, modifier = Modifier.size(18.dp))
        }
    }
}

/** Wiersz listy: ikona, tytuł z podtytułem, chevron. Minimum 64 dp, nad nim linia 1 dp. */
@Composable
fun ListRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    value: String? = null,
    divider: Boolean = true,
    trailingIcon: androidx.compose.ui.graphics.vector.ImageVector? = TtIcons.ChevronRight,
) {
    val colors = LocalTtColors.current
    Column(modifier.fillMaxWidth()) {
        if (divider) TtDivider()
        Row(
            Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 64.dp)
                .ttClickable(onClick = onClick)
                .padding(vertical = TtSpace.x3),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TtSpace.x4),
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = colors.ink2, modifier = Modifier.size(24.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = TtType.body.copy(fontWeight = FontWeight.Medium), color = colors.ink)
                if (subtitle != null) Text(subtitle, style = TtType.bodyS, color = colors.ink2)
            }
            if (value != null) Text(value, style = TtType.body, color = colors.ink2)
            if (trailingIcon != null) {
                Icon(trailingIcon, contentDescription = null, tint = colors.ink2, modifier = Modifier.size(24.dp))
            }
        }
    }
}

/** Pasek tytułu ekranu podrzędnego: strzałka powrotu i nagłówek, bez tła i bez cienia. */
@Composable
fun TtTopBar(title: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalTtColors.current
    Row(
        modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(start = TtSpace.x2, end = TtSpace.screenMargin),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TtSpace.x1),
    ) {
        Box(
            Modifier
                .size(TtSpace.minTouch)
                .clip(TtShape.full)
                .ttClickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(TtIcons.Back, contentDescription = stringResource(R.string.back), tint = colors.ink)
        }
        if (title.isNotEmpty()) Text(title, style = TtType.headline, color = colors.ink)
    }
}

/** Szkielet: prostokąt w kolorze `track`, pulsujący krycie 0,5 ↔ 1 w 1,2 s. */
@Composable
fun SkeletonBlock(
    height: Dp,
    modifier: Modifier = Modifier,
    width: Dp? = null,
    shape: Shape = RoundedCornerShape(6.dp),
) {
    val colors = LocalTtColors.current
    val duration = ttDuration(TtMotion.SKELETON)
    val alpha = if (duration == 0) {
        0.7f
    } else {
        val transition = rememberInfiniteTransition(label = "skeleton")
        transition.animateFloat(
            initialValue = 0.5f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(duration, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "alpha",
        ).value
    }
    Box(
        modifier
            .then(if (width != null) Modifier.width(width) else Modifier.fillMaxWidth())
            .height(height)
            .alpha(alpha)
            .background(colors.track, shape),
    )
}

/** Poziomy pasek z udziałem dyscyplin: odcinki w kolorach, przerwa 1 dp, pełne zaokrąglenie. */
@Composable
fun ShareBar(
    parts: List<Pair<Color, Float>>,
    modifier: Modifier = Modifier,
    height: Dp = 10.dp,
    widthFraction: Float = 1f,
    alpha: Float = 1f,
    gap: Dp = 1.dp,
) {
    val colors = LocalTtColors.current
    val total = parts.sumOf { it.second.toDouble() }.toFloat()
    Box(
        modifier
            .fillMaxWidth(widthFraction.coerceIn(0.02f, 1f))
            .height(height)
            .clip(TtShape.full)
            .background(colors.track),
    ) {
        if (total > 0f) {
            Row(
                Modifier.fillMaxWidth().fillMaxHeight().alpha(alpha),
                horizontalArrangement = Arrangement.spacedBy(gap),
            ) {
                parts.filter { it.second > 0f }.forEach { (color, value) ->
                    Box(Modifier.weight(value).fillMaxHeight().clip(TtShape.full).background(color))
                }
            }
        }
    }
}

/** Pusty stan ekranu: nagłówek, zdanie wyjaśniające i (opcjonalnie) akcja. */
@Composable
fun EmptyState(
    body: String,
    modifier: Modifier = Modifier,
    title: String? = null,
    topPadding: Dp = 72.dp,
    action: (@Composable () -> Unit)? = null,
) {
    val colors = LocalTtColors.current
    Column(
        modifier
            .fillMaxWidth()
            .padding(start = TtSpace.screenMargin, end = TtSpace.screenMargin, top = topPadding, bottom = TtSpace.x6),
        verticalArrangement = Arrangement.spacedBy(TtSpace.x4),
    ) {
        if (title != null) Text(title, style = TtType.headline, color = colors.ink)
        Text(body, style = TtType.body, color = colors.ink2)
        action?.invoke()
    }
}

/** Numerowane kroki łańcucha danych (pusty Pulpit): `1 Health Sync…`. */
@Composable
fun NumberedSteps(steps: List<String>, modifier: Modifier = Modifier) {
    val colors = LocalTtColors.current
    Column(modifier.fillMaxWidth()) {
        steps.forEachIndexed { index, step ->
            TtDivider()
            Row(
                Modifier.fillMaxWidth().padding(vertical = TtSpace.x3),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(TtSpace.x3),
            ) {
                Box(
                    Modifier.size(24.dp).clip(TtShape.full).border(1.dp, colors.line, TtShape.full),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("${index + 1}", style = TtType.caption, color = colors.ink2)
                }
                Text(step, style = TtType.body, color = colors.ink)
            }
        }
    }
}

/** Mały podpis osi i legend (12/14). */
@Composable
fun CaptionText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = LocalTtColors.current.ink2,
    textAlign: androidx.compose.ui.text.style.TextAlign? = null,
) {
    Text(text, style = TtType.caption, color = color, textAlign = textAlign, maxLines = 1, modifier = modifier)
}


/**
 * Pasek filtrów: chipy dyscyplin i menu okresu w jednym wierszu.
 * Gdy ekran jest zbyt wąski, wiersz przewija się poziomo (nic nie przechodzi do drugiej linii ani nie jest ucinane).
 */
@Composable
fun FilterBar(
    disciplines: Set<Discipline>,
    onToggleDiscipline: (Discipline) -> Unit,
    period: PeriodFilter,
    onPeriod: (PeriodFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalTtColors.current
    Row(
        modifier
            .fillMaxWidth()
            .horizontalScroll(androidx.compose.foundation.rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(TtSpace.x2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Discipline.entries.forEach { discipline ->
            val isSelected = discipline in disciplines
            TtChip(
                label = stringResource(discipline.labelRes()),
                onClick = { onToggleDiscipline(discipline) },
                selected = isSelected,
                background = if (isSelected) colors.container(discipline) else Color.Transparent,
                border = if (isSelected) colors.of(discipline) else colors.line,
                leading = { DisciplineDot(discipline) },
                paddingStart = 10.dp,
                paddingEnd = 10.dp,
            )
        }
        PeriodMenuChip(period, onPeriod)
    }
}
