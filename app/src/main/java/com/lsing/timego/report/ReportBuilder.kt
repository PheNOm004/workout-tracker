package com.lsing.timego.report

import com.lsing.timego.data.Exercise
import com.lsing.timego.data.LoggingType
import com.lsing.timego.data.SetLog
import com.lsing.timego.profile.TrainingProfile
import java.time.LocalDate
import java.time.YearMonth

fun buildWeeklyReport(input: ReportInput, week: LocalDate, profile: TrainingProfile = TrainingProfile()): WorkoutReport =
    buildReport(input, ReportPeriod.weekContaining(week), profile)

fun buildMonthlyReport(input: ReportInput, month: YearMonth, profile: TrainingProfile = TrainingProfile()): WorkoutReport =
    buildReport(input, ReportPeriod.month(month), profile)

fun buildReport(input: ReportInput, period: ReportPeriod, profile: TrainingProfile = TrainingProfile()): WorkoutReport {
    val exercises = input.exercises.associateBy(Exercise::id)
    val sessions = input.sessions.filter { it.date in period }
    val sessionIds = sessions.mapTo(hashSetOf()) { it.id }
    val sets = input.sets.filter { it.sessionId in sessionIds }
    val previous = period.previous()
    val previousSessionIds = input.sessions.filter { it.date in previous }.mapTo(hashSetOf()) { it.id }
    val previousSets = input.sets.filter { it.sessionId in previousSessionIds }
    val strengthVolume = strengthVolume(sets, exercises)
    val previousStrengthVolume = strengthVolume(previousSets, exercises).takeIf { previousSets.isNotEmpty() }
    val completedDuration = sessions.sumOf { session ->
        val end = session.endEpochMillis ?: session.startEpochMillis
        ((end - session.startEpochMillis).coerceAtLeast(0) / 60_000)
    }
    val muscleGroups = sets.flatMap { exercises[it.exerciseId]?.muscleGroups.orEmpty() }.toSet()
    val currentMax = sets.filter { !it.isWarmup }.groupBy(SetLog::exerciseId).mapValues { (_, logs) -> logs.maxOf { it.weightKg } }
    val priorMax = input.sets.filter { log ->
        val sessionDate = input.sessions.firstOrNull { it.id == log.sessionId }?.date
        sessionDate != null && sessionDate.isBefore(period.start) && !log.isWarmup
    }.groupBy(SetLog::exerciseId).mapValues { (_, logs) -> logs.maxOf { it.weightKg } }
    val records = currentMax.count { (exerciseId, value) -> value > 0 && value > (priorMax[exerciseId] ?: Double.NEGATIVE_INFINITY) }
    val latestWeight = input.bodyMetrics.filter { it.date in period && it.weightKg != null }.maxByOrNull { it.date }?.weightKg
    val targetDays = profile.trainingDaysPerWeek
    val suggestion = when {
        sessions.isEmpty() -> "No workouts were logged. Start with one manageable session when ready."
        targetDays != null && sessions.map { it.date }.distinct().size < targetDays -> "You logged fewer active days than planned. Consider a shorter session rather than adding intensity."
        else -> "Keep the next period consistent and progress only while technique remains controlled."
    }
    return WorkoutReport(
        period = period,
        sessionCount = sessions.size,
        activeDays = sessions.map { it.date }.distinct().size,
        durationMinutes = completedDuration,
        workingSets = sets.count { !it.isWarmup && exercises[it.exerciseId]?.loggingType == LoggingType.WEIGHT_REPS.name },
        strengthVolumeKg = MetricComparison(strengthVolume, previousStrengthVolume),
        holdSeconds = sets.sumOf { it.holdSeconds ?: 0 },
        cardioMinutes = sets.filter { exercises[it.exerciseId]?.loggingType == LoggingType.DURATION_DISTANCE.name }.sumOf { it.durationMinutes ?: 0.0 },
        cardioDistanceKm = sets.filter { exercises[it.exerciseId]?.loggingType == LoggingType.DURATION_DISTANCE.name }.sumOf { it.distanceKm ?: 0.0 },
        muscleGroups = muscleGroups,
        personalRecordCount = records,
        latestBodyWeightKg = latestWeight,
        suggestion = suggestion,
    )
}

private fun strengthVolume(sets: List<SetLog>, exercises: Map<Long, Exercise>): Double = sets.sumOf { log ->
    if (!log.isWarmup && exercises[log.exerciseId]?.loggingType == LoggingType.WEIGHT_REPS.name) log.weightKg * log.reps else 0.0
}
