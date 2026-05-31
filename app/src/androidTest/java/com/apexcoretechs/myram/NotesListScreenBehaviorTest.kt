package com.apexcoretechs.myram

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
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
class NotesListScreenBehaviorTest {
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
    fun longPressSelectedNote_showsBulkActions_whenMultipleSelected() {
        openNewNote()
        composeRule.onAllNodesWithContentDescription("Back")[0].performClick()
        openNewNote()
        composeRule.onAllNodesWithContentDescription("Back")[0].performClick()

        composeRule.onNodeWithText("Untitled").assertIsDisplayed()
        composeRule.onAllNodesWithContentDescription("Select notes")[0].performClick()

        composeRule.onAllNodesWithText("Untitled")[0].performClick()
        composeRule.onAllNodesWithText("Untitled")[1].performClick()

        composeRule.onAllNodesWithText("Untitled")[0].performTouchInput {
            longClick()
        }

        composeRule.onNodeWithText("Choose an action for selected notes.").assertIsDisplayed()
    }
}
