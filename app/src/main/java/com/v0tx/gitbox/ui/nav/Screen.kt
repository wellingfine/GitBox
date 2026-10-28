package com.v0tx.gitbox.ui.nav

import java.time.LocalDate

sealed class Screen {
    /** 第一个 Tab：当前 Box 的功能页（Box 由代码注册） */
    data object BoxHome : Screen()

    /** 第二个 Tab：内置 Box 列表，只能切换不能增删 */
    data object BoxList : Screen()

    /** 第三个 Tab：配置（存储 / 外观 / 关于） */
    data object Settings : Screen()

    /** 修改某个 Box 的显示名 / 图标 / 颜色 */
    data class BoxAppearance(val boxId: String) : Screen()

    /** 打卡的子页面 */
    data class CheckInDay(val date: LocalDate) : Screen()
    data object CheckInWall : Screen()
}

val TAB_ROOTS = listOf(Screen.BoxHome, Screen.BoxList, Screen.Settings)
