package com.example.bfit

import android.graphics.Color
import android.text.style.ForegroundColorSpan

class DayCompletionDecorator(private val completedDates: Set<Long>) {

    fun shouldDecorate(day: Long): Boolean {
        return completedDates.contains(day)
    }

}
