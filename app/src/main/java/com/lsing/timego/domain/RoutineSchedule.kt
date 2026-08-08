package com.lsing.timego.domain

import com.lsing.timego.data.Routine
import java.time.DayOfWeek

fun routinesForToday(routines: List<Routine>, today: DayOfWeek): List<Routine> =
    routines.filter { today.name in it.daysOfWeek }
