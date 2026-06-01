package com.apexcoretechs.myram

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.longClick
import androidx.test.espresso.Espresso.pressBack
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
        val directByContentDescription = composeRule.onAllNodesWithContentDescription("new-note")
        if (directByContentDescription.fetchSemanticsNodes().isNotEmpty()) {
            directByContentDescription[0].performClick()
            return
        }

        val overflowButtons = composeRule.onAllNodesWithContentDescription("More actions")
        assertTrue(overflowButtons.fetchSemanticsNodes().isNotEmpty())
        overflowButtons[0].performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("New note", ignoreCase = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onAllNodesWithText("New note", ignoreCase = true)[0].performClick()
    }

    @Test
    fun longPressSelectedNote_showsBulkActions_whenMultipleSelected() {
        openNewNote()
        closeEditor()
        openNewNote()
        closeEditor()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Untitled")
                .fetchSemanticsNodes().size >= 2
        }
        composeRule.onAllNodesWithContentDescription("Select notes")[0].performClick()

        composeRule.onAllNodesWithText("Untitled")[0].performClick()
        composeRule.onAllNodesWithText("Untitled")[1].performClick()

        composeRule.onAllNodesWithText("Untitled")[0].performTouchInput {
            longClick()
        }

        composeRule.onNodeWithText("Choose an action for selected notes.").assertIsDisplayed()
    }

    private fun closeEditor() {
        pressBack()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithContentDescription("Select notes")
                .fetchSemanticsNodes().isNotEmpty() ||
                composeRule.onAllNodesWithContentDescription("More actions")
                    .fetchSemanticsNodes().isNotEmpty()
        }
    }
}
