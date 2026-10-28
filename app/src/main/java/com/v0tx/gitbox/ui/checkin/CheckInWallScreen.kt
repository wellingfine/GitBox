package com.v0tx.gitbox.ui.checkin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.v0tx.gitbox.ui.MainViewModel
import com.v0tx.gitbox.ui.UiState
import com.v0tx.gitbox.ui.components.AppTopBar
import com.v0tx.gitbox.ui.components.HintText
import com.v0tx.gitbox.ui.components.SectionTitle
import com.v0tx.gitbox.ui.components.TagChip
import com.v0tx.gitbox.util.monthLabel
import com.v0tx.gitbox.util.startOfWeek
import com.v0tx.gitbox.util.toLocalDate
import java.time.LocalDate

private const val WEEKS = 53

@Composable
fun CheckInWallScreen(
    state: UiState,
    vm: MainViewModel,
    onBack: () -> Unit,
    onOpenDay: (LocalDate) -> Unit
) {
    val today = LocalDate.now()
    val counts = remember(state.checkIn.records) {
        state.checkIn.records.groupBy { it.timestamp.toLocalDate() }
            .mapValues { it.value.size }
    }
    val days = remember(state.checkIn.records) {
        state.checkIn.records.map { it.timestamp.toLocalDate() }.toSet()
    }
    val lastSunday = today.startOfWeek()
    val firstDay = lastSunday.minusWeeks((WEEKS - 1).toLong())

    LazyColumn(modifier = Modifier.fillMaxWidth()) {
        item { AppTopBar(title = "打卡记录墙", onBack = onBack) }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "近一年共打卡 ${state.checkIn.records.size} 次",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(top = 6.dp)
                        ) {
                            TagChip(text = "当前连续 ${currentStreak(days)} 天")
                            TagChip(text = "最长连续 ${longestStreak(days)} 天")
                        }
                    }
                }
            }
        }

        item { SectionTitle("每天打卡次数") }
        item {
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                for (w in 0 until WEEKS) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                        modifier = Modifier.padding(end = 3.dp)
                    ) {
                        Text(
                            text = monthLabelFor(firstDay, w),
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.height(14.dp)
                        )
                        for (d in 0..6) {
                            val date = firstDay.plusDays((w * 7 + d).toLong())
                            val count = counts[date] ?: 0
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(cellColor(count, date > today))
                                    .clickable { onOpenDay(date) }
                            )
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "少",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                for (level in 0..4) {
                    Box(
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .size(12.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(cellColor(level, false))
                    )
                }
                Text(
                    text = "多",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
        item { HintText("点击任意格子可以查看当天的打卡时间。") }
    }
}

@Composable
private fun cellColor(level: Int, future: Boolean): Color {
    if (future) return Color.Transparent
    val empty = MaterialTheme.colorScheme.surfaceVariant
    return when (level) {
        0 -> empty
        1 -> Color(0xFF9BE9A8)
        2 -> Color(0xFF40C463)
        3 -> Color(0xFF30A14E)
        else -> Color(0xFF216E39)
    }
}

private fun monthLabelFor(firstDay: LocalDate, week: Int): String {
    val date = firstDay.plusDays((week * 7).toLong())
    if (week == 0) return ""
    val prev = firstDay.plusDays(((week - 1) * 7).toLong())
    return if (date.monthValue != prev.monthValue) date.monthLabel() else ""
}

private fun currentStreak(days: Set<LocalDate>): Int {
    if (days.isEmpty()) return 0
    var cursor = LocalDate.now()
    if (cursor !in days) cursor = cursor.minusDays(1)
    var count = 0
    while (cursor in days) {
        count++
        cursor = cursor.minusDays(1)
    }
    return count
}

private fun longestStreak(days: Set<LocalDate>): Int {
    if (days.isEmpty()) return 0
    val sorted = days.sorted()
    var best = 1
    var current = 1
    for (i in 1 until sorted.size) {
        current = if (sorted[i].minusDays(1) == sorted[i - 1]) current + 1 else 1
        if (current > best) best = current
    }
    return best
}
