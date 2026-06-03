package com.northsignalstudio.myram

import com.northsignalstudio.myram.ui.screens.noteContentPreviewText
import org.junit.Assert.assertEquals
import org.junit.Test

class NotesListPreviewTest {

    @Test
    fun noteContentPreviewText_omitsCompletedChecklistLines() {
        val preview = noteContentPreviewText("☑ Done\n☐ Pending\nRegular detail")

        assertEquals("☐ Pending\nRegular detail", preview)
    }

    @Test
    fun noteContentPreviewText_omitsLegacyCompletedChecklistLines() {
        val preview = noteContentPreviewText("- [x] Done\n[X] Also done\n- [ ] Pending")

        assertEquals("- [ ] Pending", preview)
    }
}
