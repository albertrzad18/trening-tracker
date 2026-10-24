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
import io.github.albertrzad18.treningtracker.ui.theme.PoolBlue
import io.github.albertrzad18.treningtracker.ui.theme.RoadGreen
import io.github.albertrzad18.treningtracker.ui.theme.TrainerOrange

/** Stały kolor dyscypliny: basen niebieski, rower zielony, trenażer pomarańczowy. */
fun Discipline.color(): Color = when (this) {
    Discipline.POOL -> PoolBlue
    Discipline.ROAD -> RoadGreen
    Discipline.TRAINER -> TrainerOrange
}

@StringRes
fun Discipline.labelRes(): Int = when (this) {
    Discipline.POOL -> R.string.discipline_pool
    Discipline.ROAD -> R.string.discipline_road
    Discipline.TRAINER -> R.string.discipline_trainer
}

/** Kolorowa kropka dyscypliny, używana w tabelach, kafelkach i kartach. */
@Composable
fun DisciplineDot(discipline: Discipline, size: Dp = 12.dp, modifier: Modifier = Modifier) {
    Box(modifier.size(size).background(discipline.color(), CircleShape))
}
