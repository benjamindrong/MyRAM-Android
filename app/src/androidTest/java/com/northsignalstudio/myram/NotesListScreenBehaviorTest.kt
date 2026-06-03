package com.northsignalstudio.myram

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
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

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithContentDescription("Selection actions")
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onAllNodesWithContentDescription("Selection actions")[0].performClick()

        composeRule.onNodeWithText("Choose an action for selected notes.").assertIsDisplayed()
    }

    @Test
    fun folderTitle_canBeRenamedFromTopBar() {
        val folderName = "UITest Folder"
        val renamedFolderName = "UITest Folder Renamed"

        val overflowButtons = composeRule.onAllNodesWithContentDescription("More actions")
        assertTrue(overflowButtons.fetchSemanticsNodes().isNotEmpty())
        overflowButtons[0].performClick()
        composeRule.onNodeWithText("New folder").performClick()

        composeRule.onNodeWithText("Folder name").performClick()
        composeRule.onNodeWithText("Folder name").performTextInput(folderName)
        composeRule.onNodeWithText("Create").performClick()

        composeRule.onNodeWithText(folderName).performClick()
        composeRule.onNodeWithTag("edit-folder-title").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("Folder name").performClick()
        composeRule.onNodeWithText("Folder name").performTextClearance()
        composeRule.onNodeWithText("Folder name").performTextInput(renamedFolderName)
        composeRule.onNodeWithText("Save").performClick()

        composeRule.onNodeWithText(renamedFolderName).assertIsDisplayed()
    }

    private fun closeEditor() {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("close-note-editor")
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("close-note-editor").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithContentDescription("Select notes")
                .fetchSemanticsNodes().isNotEmpty() ||
                composeRule.onAllNodesWithContentDescription("More actions")
                    .fetchSemanticsNodes().isNotEmpty()
        }
    }
}
