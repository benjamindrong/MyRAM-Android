package com.apexcoretechs.myram

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@RunWith(AndroidJUnit4::class)
class NoteEditorScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun keyboardControlBar_isVisible_withoutAttachments() {
        val newNoteButtons = composeRule.onAllNodesWithContentDescription("New note")
        if (newNoteButtons.fetchSemanticsNodes().isNotEmpty()) {
            newNoteButtons[0].performClick()
        } else {
            composeRule.onNodeWithText("+").performClick()
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("keyboard-control-bar")
                .fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithTag("keyboard-control-bar").assertIsDisplayed()
        assertTrue(
            composeRule.onAllNodesWithContentDescription("Hide keyboard")
                .fetchSemanticsNodes().isNotEmpty()
        )
    }
}
