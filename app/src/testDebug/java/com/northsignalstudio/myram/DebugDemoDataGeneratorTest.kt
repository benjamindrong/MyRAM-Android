package com.northsignalstudio.myram

import com.northsignalstudio.myram.debug.DebugDemoDataGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DebugDemoDataGeneratorTest {

    @Test
    fun demoNotes_includeExpectedSeedCountAndStableIds() {
        assertEquals(8, DebugDemoDataGenerator.demoNotes.size)
        assertEquals(8, DebugDemoDataGenerator.demoNoteIds.size)
        assertEquals(
            setOf(-610001, -610002, -610003, -610004, -610005, -610006, -610007, -610008),
            DebugDemoDataGenerator.demoNoteIds
        )
    }

    @Test
    fun demoNotes_keepPinnedTextOutOfBody() {
        DebugDemoDataGenerator.demoNotes.forEach { seed ->
            seed.pinnedText.forEach { pinnedText ->
                assertFalse(
                    "Pinned text should not appear in body for ${seed.title}",
                    seed.body.contains(pinnedText)
                )
            }
        }
    }

    @Test
    fun demoNotes_includeNotesWithAndWithoutPinnedText() {
        assertTrue(DebugDemoDataGenerator.demoNotes.any { it.pinnedText.isEmpty() })
        assertTrue(DebugDemoDataGenerator.demoNotes.any { it.pinnedText.size > 1 })
    }
}
