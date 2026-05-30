package com.apexcoretechs.myram

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.longClick
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@RunWith(AndroidJUnit4::class)
class NoteEditorScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private fun openNewNote() {
        val overflowButtons = composeRule.onAllNodesWithContentDescription("More actions")
        if (overflowButtons.fetchSemanticsNodes().isNotEmpty()) {
            overflowButtons[0].performClick()
            composeRule.onNodeWithText("New note").performClick()
        } else {
            val newNoteButtons = composeRule.onAllNodesWithContentDescription("New note")
            assertTrue(newNoteButtons.fetchSemanticsNodes().isNotEmpty())
            newNoteButtons[0].performClick()
        }
    }

    @Test
    fun keyboardControlBar_isVisible_withoutAttachments() {
        openNewNote()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("keyboard-control-bar")
                .fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithTag("keyboard-control-bar").assertIsDisplayed()
        assertTrue(
            composeRule.onAllNodesWithContentDescription("Hide keyboard")
                .fetchSemanticsNodes().isNotEmpty()
        )
        composeRule.onNodeWithTag("edit-note-title").assertIsDisplayed()
        composeRule.onNodeWithTag("redo-button").assertIsDisplayed()
    }

    @Test
    fun longPressNote_showsFloatingPreview() {
        openNewNote()
        composeRule.onNodeWithContentDescription("Back").performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Untitled")
                .fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onAllNodesWithText("Untitled")[0].performTouchInput {
            longClick()
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("note-preview-dialog")
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("note-preview-dialog").assertIsDisplayed()
    }
}
