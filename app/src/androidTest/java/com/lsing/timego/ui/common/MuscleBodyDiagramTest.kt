package com.lsing.timego.ui.common

import android.graphics.Region
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.lsing.timego.data.MuscleGroup
import com.lsing.timego.ui.theme.TimeGoTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

/** Run only on a disposable emulator; does not load or modify workout data. */
class MuscleBodyDiagramTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun everyGroupHasVisibleHitRegionsAndNeutralAnatomyNeverHits() {
        val found = mutableSetOf<MuscleGroup>()
        for ((specs, box) in listOf(FRONT_BODY_PATHS to FRONT_BODY_VIEWBOX, BACK_BODY_PATHS to BACK_BODY_VIEWBOX)) {
            val shapes = buildShapes(specs, box)
            val regions = buildGroupRegions(shapes, box)
            regions.forEach { (group, region) ->
                assertFalse("$group has no hit area", region.isEmpty)
                found += group
            }
            for ((group, region) in regions) {
                regions.filterKeys { it != group }.forEach { (other, otherRegion) ->
                    assertTrue("$group overlaps $other", Region(region).apply { op(otherRegion, Region.Op.INTERSECT) }.isEmpty)
                }
            }
            // Probe head, palms, ankles, and central pelvis in source coordinates.
            val samples = if (box === FRONT_BODY_VIEWBOX) listOf(471 to 80, 307 to 505, 406 to 878, 470 to 473, 412 to 306, 529 to 306)
                else listOf(1060 to 80, 900 to 505, 989 to 884)
            samples.forEach { (x, y) ->
                assertTrue("Neutral point $x,$y is tracked", regions.values.none { it.contains(x - box[0].toInt(), y - box[1].toInt()) })
            }
            // Real Android path parsing must preserve every shape and its crop extent.
            shapes.zip(specs).forEach { (shape, spec) ->
                assertFalse(shape.path.isEmpty)
                val clip = Region(0, 0, 400, 960)
                val pixels = Region().apply { setPath(shape.path.asAndroidPath(), clip) }.bounds
                if (!pixels.isEmpty) {
                    assertTrue(pixels.left >= spec.bounds[0] - box[0] - 2)
                    assertTrue(pixels.top >= spec.bounds[1] - box[1] - 2)
                    assertTrue(pixels.right <= spec.bounds[2] - box[0] + 2)
                    assertTrue(pixels.bottom <= spec.bounds[3] - box[1] + 2)
                }
            }
        }
        assertEquals(MuscleGroup.entries.toSet() - MuscleGroup.FULL_BODY, found)
    }

    @Test
    fun neutralOverlaysRemoveTheUnderlyingHitTarget() {
        val specs = listOf(
            MusclePathSpec("M0 0 H100 V100 H0 Z", MuscleGroup.CHEST, false, .6f, floatArrayOf(0f,0f,100f,100f)),
            MusclePathSpec("M40 0 H60 V100 H40 Z", null, true, .6f, floatArrayOf(40f,0f,60f,100f)),
        )
        val box = floatArrayOf(0f,0f,100f,100f)
        val region = buildGroupRegions(buildShapes(specs, box), box).getValue(MuscleGroup.CHEST)
        assertTrue(region.contains(20,50))
        assertFalse(region.contains(50,50))
        assertTrue(region.contains(80,50))
    }

    @Test
    fun holdingChestShowsAndReleasingHidesReadout() {
        compose.setContent {
            TimeGoTheme {
                MuscleBodyDiagram(emptyMap(), Modifier.fillMaxWidth().testTag("map"), periodLabel = "test period")
            }
        }
        val node=compose.onNodeWithTag("map")
        val width=node.fetchSemanticsNode().size.width.toFloat()/2f
        val point=Offset((420f-FRONT_BODY_VIEWBOX[0])*width/380f,(238f-FRONT_BODY_VIEWBOX[1])*width/380f)
        node.performTouchInput { down(point) }
        compose.mainClock.advanceTimeBy(1000)
        compose.onNodeWithText("Chest").assertExists()
        compose.onNodeWithText("No sets · test period").assertExists()
        node.performTouchInput { up() }
        compose.mainClock.advanceTimeBy(1000)
        compose.onNodeWithText("No sets · test period").assertDoesNotExist()
    }

    @Test fun renderDarkStates() = renderStates(true)
    @Test fun renderLightStates() = renderStates(false)

    private fun renderStates(dark: Boolean) {
        var state by androidx.compose.runtime.mutableStateOf(0)
        compose.setContent {
            TimeGoTheme(darkTheme = dark) {
                Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background).testTag("states")) {
                    MuscleBodyDiagram(
                        when(state) { 0 -> emptyMap(); 1 -> mapOf("CHEST" to .85f, "FRONT_DELTS" to .5f, "ADDUCTORS" to .65f); else -> mapOf("FULL_BODY" to .8f) },
                        periodLabel = "test period",
                    )
                    CroppedMuscleDiagram(setOf("CHEST", "TRICEPS"), Color.Cyan, Modifier.height(100.dp))
                    CroppedMuscleDiagram(setOf("ADDUCTORS"), Color.Cyan, Modifier.height(80.dp))
                }
            }
        }
        for (i in 0..2) {
            compose.runOnIdle { state=i }
            compose.mainClock.advanceTimeBy(2000)
            compose.waitForIdle()
            val bitmap=compose.onNodeWithTag("states").captureToImage().asAndroidBitmap()
            val folder=File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null),"bodymap-review").apply { mkdirs() }
            File(folder,"${if(dark) "dark" else "light"}-$i.png").outputStream().use {
                bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)
            }
        }
    }
}
