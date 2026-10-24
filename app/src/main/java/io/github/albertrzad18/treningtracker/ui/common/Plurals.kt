package io.github.albertrzad18.treningtracker.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.albertrzad18.treningtracker.R
import io.github.albertrzad18.treningtracker.ui.format.pluralPl

/** `1 dzień`, `3 dni`, `7 dni`. */
@Composable
fun daysText(count: Int): String = pluralPl(
    count,
    stringResource(R.string.unit_day_one),
    stringResource(R.string.unit_day_few),
    stringResource(R.string.unit_day_many),
)

/** `1 okres`, `3 okresy`, `6 okresów`. */
@Composable
fun periodsText(count: Int): String = pluralPl(
    count,
    stringResource(R.string.unit_period_one),
    stringResource(R.string.unit_period_few),
    stringResource(R.string.unit_period_many),
)
