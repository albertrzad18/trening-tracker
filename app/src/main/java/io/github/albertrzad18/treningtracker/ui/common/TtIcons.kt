package io.github.albertrzad18.treningtracker.ui.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.FormatListBulleted
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CalendarViewWeek
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Troubleshoot
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Jeden zestaw ikon na całą apkę (zaokrąglone, odpowiednik Material Symbols Rounded).
 * Trzymamy je w jednym miejscu, żeby nie mieszać zestawów i żeby łatwo było podmienić ikonę.
 */
object TtIcons {
    val Home: ImageVector get() = Icons.Rounded.Home
    val Workouts: ImageVector get() = Icons.AutoMirrored.Rounded.FormatListBulleted
    val Stats: ImageVector get() = Icons.Rounded.BarChart
    val More: ImageVector get() = Icons.Rounded.MoreHoriz

    val Summaries: ImageVector get() = Icons.Rounded.CalendarViewWeek
    val Trophy: ImageVector get() = Icons.Rounded.EmojiEvents
    val Goals: ImageVector get() = Icons.Rounded.Flag
    val Settings: ImageVector get() = Icons.Rounded.Tune
    val Diagnostics: ImageVector get() = Icons.Rounded.Troubleshoot

    val Sync: ImageVector get() = Icons.Rounded.Sync
    val Add: ImageVector get() = Icons.Rounded.Add
    val Done: ImageVector get() = Icons.Rounded.CheckCircle
    val Check: ImageVector get() = Icons.Rounded.Check
    val Up: ImageVector get() = Icons.Rounded.ArrowUpward
    val Down: ImageVector get() = Icons.Rounded.ArrowDownward
    val DropDown: ImageVector get() = Icons.Rounded.ArrowDropDown
    val ChevronRight: ImageVector get() = Icons.Rounded.ChevronRight
    val ExpandMore: ImageVector get() = Icons.Rounded.ExpandMore
    val ExpandLess: ImageVector get() = Icons.Rounded.ExpandLess
    val Copy: ImageVector get() = Icons.Rounded.ContentCopy
    val Back: ImageVector get() = Icons.AutoMirrored.Rounded.ArrowBack
    val Forward: ImageVector get() = Icons.AutoMirrored.Rounded.ArrowForward
}
