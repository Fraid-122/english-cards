package com.fraid.englishcards.data

import java.util.TimeZone

/** Days in a row with at least one answered card. */
object Streak {

    private const val DAY_MILLIS = 24L * 60 * 60 * 1000

    /** Number of the local calendar day. */
    fun today(now: Long = System.currentTimeMillis()): Long =
        (now + TimeZone.getDefault().getOffset(now)) / DAY_MILLIS

    /** The streak after studying on [today]. */
    fun afterStudy(lastDay: Long, streak: Int, today: Long): Int = when (lastDay) {
        today -> maxOf(streak, 1)
        today - 1 -> streak + 1
        else -> 1
    }

    /** The streak to show on [today]: it is lost after a full day without study. */
    fun current(lastDay: Long, streak: Int, today: Long): Int =
        if (lastDay >= today - 1) streak else 0
}
