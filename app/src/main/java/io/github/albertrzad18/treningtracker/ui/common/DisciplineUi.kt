package io.github.albertrzad18.treningtracker.ui.common

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.albertrzad18.treningtracker.R
import io.github.albertrzad18.treningtracker.domain.Discipline
import io.github.albertrzad18.treningtracker.ui.theme.LocalTtColors

/** Stały kolor dyscypliny z motywu: basen niebieski, rower zielony, trenażer pomarańczowy. */
@Composable
fun Discipline.color(): Color = LocalTtColors.current.of(this)

/** Tło kontenera dyscypliny (zaznaczony chip, obszar wykresu). */
@Composable
fun Discipline.container(): Color = LocalTtColors.current.container(this)

@StringRes
fun Discipline.labelRes(): Int = when (this) {
    Discipline.POOL -> R.string.discipline_pool
    Discipline.ROAD -> R.string.discipline_road
    Discipline.TRAINER -> R.string.discipline_trainer
}

/** Kolorowa kropka dyscypliny: 8 dp w listach i wierszach, większa w nagłówkach. */
@Composable
fun DisciplineDot(discipline: Discipline, size: Dp = 8.dp, modifier: Modifier = Modifier) {
    Box(modifier.size(size).background(discipline.color(), CircleShape))
}
