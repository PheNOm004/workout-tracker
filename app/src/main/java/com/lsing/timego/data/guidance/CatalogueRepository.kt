package com.lsing.timego.data.guidance

import androidx.room.withTransaction
import com.lsing.timego.data.TimeGoDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CatalogueRepository(private val database: TimeGoDatabase) {
    private val dao = database.exerciseGuidanceDao()

    val guidanceByKey: Flow<Map<String, ExerciseGuidance>> =
        dao.observeAll().map { rows -> rows.associateBy(ExerciseGuidance::catalogueKey) }

    fun observeGuidance(key: String): Flow<ExerciseGuidance?> = dao.observeByKey(key)

    suspend fun ensureBundledImported() {
        database.withTransaction {
            if ((dao.newestVersion() ?: 0) < BUNDLED_CATALOGUE_VERSION) {
                require(BUNDLED_EXERCISE_GUIDANCE.size == 820) { "Bundled catalogue must contain 820 entries" }
                require(BUNDLED_EXERCISE_GUIDANCE.map(ExerciseGuidance::catalogueKey).toSet().size == 820) { "Bundled catalogue keys must be unique" }
                dao.upsertAll(BUNDLED_EXERCISE_GUIDANCE)
            }
        }
    }

    suspend fun resolveEasier(key: String): ExerciseGuidance? {
        val rows = BUNDLED_EXERCISE_GUIDANCE.associateBy(ExerciseGuidance::catalogueKey)
        return rows[rows[key]?.easierVariationKey]
    }
}
