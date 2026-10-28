package com.v0tx.gitbox.ui.icons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.vector.ImageVector

data class AppIcon(val key: String, val label: String, val image: ImageVector)

/** 内置图标库，box 与打卡事项都可以从中选择 */
val BUILT_IN_ICONS: List<AppIcon> = listOf(
    AppIcon("star", "星星", Icons.Filled.Star),
    AppIcon("home", "家", Icons.Filled.Home),
    AppIcon("favorite", "喜欢", Icons.Filled.Favorite),
    AppIcon("check", "完成", Icons.Filled.CheckCircle),
    AppIcon("task", "任务", Icons.Filled.TaskAlt),
    AppIcon("flag", "目标", Icons.Filled.Flag),
    AppIcon("bolt", "闪电", Icons.Filled.Bolt),
    AppIcon("fire", "热情", Icons.Filled.Whatshot),
    AppIcon("lightbulb", "想法", Icons.Filled.Lightbulb),
    AppIcon("rocket", "冲刺", Icons.Filled.RocketLaunch),
    AppIcon("trophy", "成就", Icons.Filled.EmojiEvents),
    AppIcon("trend", "成长", Icons.Filled.TrendingUp),
    AppIcon("school", "学习", Icons.Filled.School),
    AppIcon("book", "阅读", Icons.Filled.Book),
    AppIcon("code", "编码", Icons.Filled.Code),
    AppIcon("build", "折腾", Icons.Filled.Build),
    AppIcon("science", "实验", Icons.Filled.Science),
    AppIcon("language", "语言", Icons.Filled.Language),
    AppIcon("note", "笔记", Icons.Filled.Note),
    AppIcon("brush", "绘画", Icons.Filled.Brush),
    AppIcon("camera", "摄影", Icons.Filled.Camera),
    AppIcon("music", "音乐", Icons.Filled.MusicNote),
    AppIcon("movie", "影视", Icons.Filled.Movie),
    AppIcon("game", "游戏", Icons.Filled.SportsEsports),
    AppIcon("fitness", "健身", Icons.Filled.FitnessCenter),
    AppIcon("run", "跑步", Icons.Filled.DirectionsRun),
    AppIcon("bike", "骑行", Icons.Filled.DirectionsBike),
    AppIcon("yoga", "冥想", Icons.Filled.SelfImprovement),
    AppIcon("bed", "睡眠", Icons.Filled.Bedtime),
    AppIcon("food", "吃饭", Icons.Filled.Restaurant),
    AppIcon("cafe", "咖啡", Icons.Filled.LocalCafe),
    AppIcon("cake", "甜点", Icons.Filled.Cake),
    AppIcon("water", "喝水", Icons.Filled.WaterDrop),
    AppIcon("flower", "植物", Icons.Filled.LocalFlorist),
    AppIcon("pet", "宠物", Icons.Filled.Pets),
    AppIcon("cart", "购物", Icons.Filled.ShoppingCart),
    AppIcon("money", "记账", Icons.Filled.AttachMoney),
    AppIcon("card", "银行卡", Icons.Filled.CreditCard),
    AppIcon("flight", "旅行", Icons.Filled.Flight),
    AppIcon("place", "地点", Icons.Filled.Place),
    AppIcon("public", "世界", Icons.Filled.Public),
    AppIcon("cloud", "云端", Icons.Filled.Cloud),
    AppIcon("sync", "同步", Icons.Filled.Sync),
    AppIcon("timer", "计时", Icons.Filled.Timer),
    AppIcon("alarm", "闹钟", Icons.Filled.Alarm),
    AppIcon("calendar", "日历", Icons.Filled.CalendarMonth),
    AppIcon("work", "工作", Icons.Filled.Work),
    AppIcon("person", "个人", Icons.Filled.Person)
)

val DEFAULT_ICON_KEY = "star"
val DEFAULT_ITEM_ICON_KEY = "check"

fun iconImage(key: String): ImageVector =
    BUILT_IN_ICONS.firstOrNull { it.key == key }?.image ?: Icons.Filled.Star

fun iconLabel(key: String): String =
    BUILT_IN_ICONS.firstOrNull { it.key == key }?.label ?: key

val BUILT_IN_COLORS: List<String> = listOf(
    "#4CAF50", "#8BC34A", "#009688", "#2196F3",
    "#3F51B5", "#9C27B0", "#E91E63", "#F44336",
    "#FF9800", "#FFC107", "#795548", "#607D8B"
)

val DEFAULT_COLOR = "#4CAF50"

fun parseColor(hex: String): androidx.compose.ui.graphics.Color = runCatching {
    androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(hex))
}.getOrDefault(androidx.compose.ui.graphics.Color(0xFF4CAF50))
