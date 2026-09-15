package com.lsing.timego.report

import com.lsing.timego.data.BodyMetric
import com.lsing.timego.data.Exercise
import com.lsing.timego.data.SetLog
import com.lsing.timego.data.WorkoutSession

data class ReportInput(
    val sessions: List<WorkoutSession>,
    val sets: List<SetLog>,
    val exercises: List<Exercise>,
    val bodyMetrics: List<BodyMetric> = emptyList(),
)

data class MetricComparison(val current: Double, val previous: Double?)

data class WorkoutReport(
    val period: ReportPeriod,
    val sessionCount: Int,
    val activeDays: Int,
    val durationMinutes: Long,
    val workingSets: Int,
    val strengthVolumeKg: MetricComparison,
    val holdSeconds: Int,
    val cardioMinutes: Double,
    val cardioDistanceKm: Double,
    val muscleGroups: Set<String>,
    val personalRecordCount: Int,
    val latestBodyWeightKg: Double?,
    val suggestion: String,
)
