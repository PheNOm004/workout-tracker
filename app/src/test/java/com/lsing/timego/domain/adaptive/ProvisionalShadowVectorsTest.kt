package com.lsing.timego.domain.adaptive

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.charset.StandardCharsets
import kotlin.math.abs

/**
 * Frozen cross-language examples for the deliberately hidden continuous-shadow contract.
 *
 * A wrong transform, a cross-basis leak, a non-neutral first observation, or an omitted
 * elapsed-time transition must make at least one vector fail.  The fixture uses only invented
 * task keys and values; it is never a user-history export.
 */
class ProvisionalShadowVectorsTest {

    @Test
    fun fixtureMetadataAndDeclaredTransformsMatchTheFrozenScores() {
        val fixture = ProvisionalShadowVectorFixture.document()

        assertEquals(1, fixture.schemaVersion)
        assertEquals("timego.provisional-continuous-shadow.v1", fixture.modelContractVersion)
        assertEquals("synthetic.parity.v1", fixture.metadataVersion)
        assertEquals("sha256:synthetic-fixture-no-user-history", fixture.metadataHash)
        assertEquals(1e-9, fixture.numericTolerance, 0.0)
        fixture.vectors.forEach { vector ->
            val transformed = checkNotNull(
                ProvisionalContinuousCapability.transformWorkScore(
                    basis = vector.basis,
                    primaryValue = vector.transformInputA,
                    secondaryValue = vector.transformInputB,
                ),
            )
            assertEquals(vector.expectedWorkScore, transformed, TOLERANCE)
            assertEquals(vector.expectedWorkScore, vector.observedWorkScore, TOLERANCE)
        }
    }

    @Test
    fun loadedRepFirstObservationRegistersNeutralPersonalBaseline() {
        assertVector("loaded_rep_baseline")
    }

    @Test
    fun laterSameBasisObservationUpdatesTheLoadedRepState() {
        assertVector("loaded_rep_update")
    }

    @Test
    fun repsOnlyObservationUsesAnIndependentState() {
        assertVector("reps_only_baseline")
    }

    @Test
    fun holdObservationUsesItsOwnState() {
        assertVector("hold_baseline")
    }

    @Test
    fun longGapWidensVarianceBeforeApplyingTheLaterObservation() {
        assertVector("loaded_rep_long_gap")
    }

    private fun assertVector(id: String) {
        val vector = ProvisionalShadowVectorFixture.load().single { it.id == id }
        val config = ShadowConfig(
            coordinateCount = vector.coordinateCount,
            priorVariance = vector.priorVariance,
            processVariancePerDay = vector.processVariancePerDay,
            observationVariance = vector.observationVariance,
        )
        val initialState = ShadowState(
            basis = vector.basis,
            mean = vector.initialMean,
            variance = vector.initialVariance,
            observedAtEpochMillis = vector.initialObservedAtEpochMillis,
            personalBaselines = vector.initialPersonalBaselines,
        )
        val observation = ShadowObservation(
            catalogueKey = vector.catalogueKey,
            sessionId = vector.sessionId,
            setId = vector.setId,
            endedAtEpochMillis = vector.endedAtEpochMillis,
            demandVector = vector.demandVector,
            basis = vector.basis,
            observedWorkScore = vector.observedWorkScore,
        )

        val update = ProvisionalContinuousReplay.update(initialState, observation, config)

        assertEquals(vector.expectedUpdated, update.updated)
        assertEquals(vector.expectedReason, update.abstentionReason)
        assertEquals(vector.expectedObservedAtEpochMillis, update.state.observedAtEpochMillis)
        assertEquals(vector.expectedPersonalBaselines, update.state.personalBaselines)
        assertVectorClose(vector.expectedMean, update.state.mean)
        assertVectorClose(vector.expectedVariance, update.state.variance)
        assertEquals(vector.expectedWorkScore, observation.observedWorkScore, TOLERANCE)
        if (vector.expectedUpdated) {
            assertNull(update.abstentionReason)
        } else {
            assertFalse(update.updated)
            assertEquals("registered_personal_baseline", update.abstentionReason)
        }
    }

    private fun assertVectorClose(expected: List<Double>, actual: List<Double>) {
        assertEquals(expected.size, actual.size)
        expected.zip(actual).forEach { (wanted, observed) ->
            assertTrue("expected $wanted but was $observed", abs(wanted - observed) <= TOLERANCE)
        }
    }

    private companion object {
        const val TOLERANCE = 1e-9
    }
}

private data class ProvisionalShadowVector(
    val id: String,
    val coordinateCount: Int,
    val priorVariance: Double,
    val processVariancePerDay: Double,
    val observationVariance: Double,
    val basis: ShadowBasis,
    val catalogueKey: String,
    val sessionId: Long,
    val setId: Long,
    val endedAtEpochMillis: Long,
    val demandVector: List<Double>,
    val observedWorkScore: Double,
    val transformInputA: Double,
    val transformInputB: Double?,
    val initialMean: List<Double>,
    val initialVariance: List<Double>,
    val initialObservedAtEpochMillis: Long?,
    val initialPersonalBaselines: Map<String, Double>,
    val expectedUpdated: Boolean,
    val expectedReason: String?,
    val expectedObservedAtEpochMillis: Long,
    val expectedPersonalBaselines: Map<String, Double>,
    val expectedMean: List<Double>,
    val expectedVariance: List<Double>,
    val expectedWorkScore: Double,
)

private data class ProvisionalShadowFixture(
    val schemaVersion: Int,
    val modelContractVersion: String,
    val metadataVersion: String,
    val metadataHash: String,
    val numericTolerance: Double,
    val vectors: List<ProvisionalShadowVector>,
)

/** Minimal test-only decoder for the frozen fixture; production never reads test resources. */
private object ProvisionalShadowVectorFixture {
    fun load(): List<ProvisionalShadowVector> = document().vectors

    fun document(): ProvisionalShadowFixture {
        val classLoader = requireNotNull(javaClass.classLoader) {
            "Missing class loader for provisional shadow vector fixture"
        }
        val text = checkNotNull(classLoader.getResource("adaptive/provisional-shadow-vectors.json")) {
            "Missing provisional shadow vector fixture"
        }.readText(StandardCharsets.UTF_8)
        return parseDocument(text)
    }

    private fun parseDocument(text: String): ProvisionalShadowFixture {
        val schemaVersion = integer(text, "schemaVersion")
        val modelContractVersion = requiredString(text, "modelContractVersion")
        val metadataVersion = requiredString(text, "metadataVersion")
        val metadataHash = requiredString(text, "metadataHash")
        val numericTolerance = number(text, "numericTolerance")
        val coordinateCount = integer(text, "coordinateCount")
        val priorVariance = number(text, "priorVariance")
        val processVariancePerDay = number(text, "processVariancePerDay")
        val observationVariance = number(text, "observationVariance")

        return Regex("\\{(?=[^{}]*\\\"id\\\")[^{}]*}")
            .findAll(text)
            .map { match ->
                val item = match.value
                val initialBaselineKey = nullableString(item, "initialPersonalBaselineKey")
                val initialBaselineScore = nullableNumber(item, "initialPersonalBaselineScore")
                val expectedBaselineKey = requiredString(item, "expectedPersonalBaselineKey")
                val expectedBaselineScore = number(item, "expectedPersonalBaselineScore")
                ProvisionalShadowVector(
                    id = requiredString(item, "id"),
                    coordinateCount = coordinateCount,
                    priorVariance = priorVariance,
                    processVariancePerDay = processVariancePerDay,
                    observationVariance = observationVariance,
                    basis = when (requiredString(item, "basis")) {
                        "load_reps" -> ShadowBasis.LOAD_REPS
                        "reps_only" -> ShadowBasis.REPS_ONLY
                        "hold_seconds" -> ShadowBasis.HOLD_SECONDS
                        else -> error("Unknown frozen vector basis")
                    },
                    catalogueKey = requiredString(item, "catalogueKey"),
                    sessionId = integer(item, "sessionId").toLong(),
                    setId = integer(item, "setId").toLong(),
                    endedAtEpochMillis = integer(item, "endedAtEpochMillis").toLong(),
                    demandVector = numberList(item, "demandVector"),
                    observedWorkScore = number(item, "observedWorkScore"),
                    transformInputA = number(item, "transformInputA"),
                    transformInputB = nullableNumber(item, "transformInputB"),
                    initialMean = numberList(item, "initialMean"),
                    initialVariance = numberList(item, "initialVariance"),
                    initialObservedAtEpochMillis = nullableInteger(item, "initialObservedAtEpochMillis")?.toLong(),
                    initialPersonalBaselines = initialBaselineKey
                        ?.let { key -> mapOf(key to checkNotNull(initialBaselineScore)) }
                        .orEmpty(),
                    expectedUpdated = boolean(item, "expectedUpdated"),
                    expectedReason = nullableString(item, "expectedReason"),
                    expectedObservedAtEpochMillis = integer(item, "expectedObservedAtEpochMillis").toLong(),
                    expectedPersonalBaselines = mapOf(expectedBaselineKey to expectedBaselineScore),
                    expectedMean = numberList(item, "expectedMean"),
                    expectedVariance = numberList(item, "expectedVariance"),
                    expectedWorkScore = number(item, "expectedWorkScore"),
                )
            }
            .toList()
            .also { require(it.size == 5) { "Expected five frozen provisional shadow vectors" } }
            .let { vectors ->
                ProvisionalShadowFixture(
                    schemaVersion = schemaVersion,
                    modelContractVersion = modelContractVersion,
                    metadataVersion = metadataVersion,
                    metadataHash = metadataHash,
                    numericTolerance = numericTolerance,
                    vectors = vectors,
                )
            }
    }

    private fun requiredString(text: String, field: String): String =
        capture(text, field, "\\\"([^\\\"]*)\\\"")

    private fun nullableString(text: String, field: String): String? =
        if (hasNull(text, field)) null else requiredString(text, field)

    private fun number(text: String, field: String): Double =
        capture(text, field, "(-?[0-9]+(?:\\.[0-9]+)?(?:[eE][+-]?[0-9]+)?)").toDouble()

    private fun integer(text: String, field: String): Int = number(text, field).toInt()

    private fun nullableNumber(text: String, field: String): Double? =
        if (hasNull(text, field)) null else number(text, field)

    private fun nullableInteger(text: String, field: String): Int? =
        nullableNumber(text, field)?.toInt()

    private fun boolean(text: String, field: String): Boolean =
        capture(text, field, "(true|false)").toBooleanStrict()

    private fun numberList(text: String, field: String): List<Double> =
        capture(text, field, "\\[([^]]*)]")
            .split(',')
            .map(String::trim)
            .filter(String::isNotEmpty)
            .map(String::toDouble)

    private fun hasNull(text: String, field: String): Boolean =
        Regex("\\\"${Regex.escape(field)}\\\"\\s*:\\s*null").containsMatchIn(text)

    private fun capture(text: String, field: String, valuePattern: String): String =
        checkNotNull(
            Regex("\\\"${Regex.escape(field)}\\\"\\s*:\\s*$valuePattern").find(text),
        ) { "Missing $field in frozen provisional shadow vector" }.groupValues[1]
}
