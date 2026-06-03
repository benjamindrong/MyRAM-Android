package com.apexcoretechs.myram.debug

import com.apexcoretechs.myram.data.Note
import com.apexcoretechs.myram.data.PinnedText
import com.apexcoretechs.myram.data.dao.NoteDao
import java.time.LocalDate
import java.time.ZoneId

object DebugDemoDataGenerator {
    const val isAvailable: Boolean = true
    val demoNoteIds: Set<Int> by lazy { demoNotes.map { it.id }.toSet() }

    suspend fun generateDemoNotes(noteDao: NoteDao) {
        noteDao.replaceNotesWithPinnedText(
            notes = demoNotes.map { seed ->
                Note(
                    id = seed.id,
                    title = seed.title,
                    content = seed.body,
                    isPinned = false,
                    createdAt = seed.timestamp,
                    lastModified = seed.timestamp,
                    deletedAt = null,
                    folderId = null
                )
            },
            pinnedText = demoNotes.flatMap { seed ->
                seed.pinnedText.mapIndexed { index, text ->
                    PinnedText(
                        noteId = seed.id,
                        text = text,
                        sourceContent = text,
                        sourceStart = 0,
                        sortOrder = index,
                        createdAt = seed.timestamp,
                        lastModified = seed.timestamp
                    )
                }
            }
        )
    }

    suspend fun clearDemoNotes(noteDao: NoteDao) {
        noteDao.deletePinnedTextForNotes(demoNoteIds.toList())
        noteDao.deleteNotesById(demoNoteIds.toList())
    }

    internal val demoNotes = listOf(
        DemoNoteSeed(
            id = -610001,
            title = "TODAY - Jun 3, 2026",
            date = LocalDate.of(2026, 6, 3),
            pinnedText = listOf("Ask landlord about garage opener"),
            bodyLines = listOf(
                "Need to remember to move the laundry before bed.",
                "The garage door sounded weird again.",
                "Need cat food.",
                "It only seems to happen when closing, not opening.",
                "That conversation about memory was interesting.",
                "Maybe I should spray the rollers before calling someone.",
                "Remember to ask about insurance.",
                "The problem isn't forgetting things, it's losing track of them.",
                "I don't remember hearing the garage door make that noise last winter.",
                "Wonder if most people actually reread their notes."
            )
        ),
        DemoNoteSeed(
            id = -610002,
            title = "TODAY - Jun 2, 2026",
            date = LocalDate.of(2026, 6, 2),
            pinnedText = listOf("Renew passport this month"),
            bodyLines = listOf(
                "The coffee shop by the grocery store closed.",
                "Need paper towels.",
                "I still haven't watched that documentary.",
                "Wonder if the passport process is still mostly online now.",
                "The line at the gas station was ridiculous today.",
                "Need to stop leaving receipts in the center console.",
                "Maybe the kitchen light bulb is finally dying.",
                "I wonder how long that bulb has been in there.",
                "Forgot to move the package inside before it rained."
            )
        ),
        DemoNoteSeed(
            id = -610003,
            title = "TODAY - Jun 1, 2026",
            date = LocalDate.of(2026, 6, 1),
            pinnedText = listOf(
                "Call insurance tomorrow",
                "Bring passport to appointment"
            ),
            bodyLines = listOf(
                "The passenger side tire looked low this morning.",
                "Wonder if that was the place with the weird hold music.",
                "Probably should check the tire pressure before driving too much.",
                "That movie was way longer than it needed to be.",
                "Need to find where I put the registration paperwork.",
                "Might have moved the passport when cleaning a few weeks ago.",
                "I think the tire looked fine yesterday.",
                "Still haven't finished that article I started reading last week."
            )
        ),
        DemoNoteSeed(
            id = -610004,
            title = "MyRAM",
            date = LocalDate.of(2026, 5, 31),
            pinnedText = listOf(
                "The problem is that important information gets buried",
                "Storage is not the problem",
                "Users lose track of information, not notes"
            ),
            bodyLines = listOf(
                "Maybe the real issue is visibility.",
                "I found that article from six months ago almost immediately.",
                "Need to revisit the onboarding flow.",
                "People can usually find old notes if they search for them.",
                "Wonder if users immediately understand pinning.",
                "The thing that disappears is attention.",
                "Maybe \"Pinned Highlights\" explains itself better than \"Pinned Text\".",
                "Need screenshots before launch."
            )
        ),
        DemoNoteSeed(
            id = -610005,
            title = "Things To Buy Eventually",
            date = LocalDate.of(2026, 5, 30),
            pinnedText = listOf(
                "Office chair",
                "Better desk lighting"
            ),
            bodyLines = listOf(
                "The chair starts hurting after a few hours.",
                "Need to measure the desk before buying anything.",
                "Wonder if Costco still carries that lamp.",
                "Could probably get away with a smaller chair.",
                "Need to check how much room is left in the office.",
                "That standing desk converter looked interesting.",
                "The lighting over the desk is worse than I realized."
            )
        ),
        DemoNoteSeed(
            id = -610006,
            title = "Stuff To Figure Out",
            date = LocalDate.of(2026, 5, 29),
            pinnedText = emptyList(),
            bodyLines = listOf(
                "Why does the bathroom fan make that noise sometimes?",
                "Need to look up furnace filter sizes again.",
                "I wonder if that noise only happens when it's humid.",
                "Could probably replace the weather stripping this summer.",
                "The fan didn't do it yesterday.",
                "Need to figure out what size batteries that flashlight uses.",
                "Maybe I should actually write down when it happens."
            )
        ),
        DemoNoteSeed(
            id = -610007,
            title = "Ideas Worth Revisiting",
            date = LocalDate.of(2026, 5, 28),
            pinnedText = listOf(
                "Stay connected to what matters",
                "Pinned Highlights are signal",
                "Home screen widget",
                "Cross-note highlights",
                "AI organization"
            ),
            bodyLines = listOf(
                "The phrase still feels right.",
                "People don't usually lose notes.",
                "Need to look up widget limitations on iOS.",
                "They lose track of information.",
                "Cross-note highlights could be useful someday.",
                "The signal idea keeps coming back.",
                "Need to remember to send that email.",
                "AI organization should help surface useful information without assigning priorities.",
                "The wording probably still needs work.",
                "Home screen widgets would be useful for recurring information."
            )
        ),
        DemoNoteSeed(
            id = -610008,
            title = "Home Projects",
            date = LocalDate.of(2026, 5, 27),
            pinnedText = listOf("Replace furnace filter"),
            bodyLines = listOf(
                "The kitchen light flickered again.",
                "Need paper towels.",
                "Wonder if LED bulbs actually last as long as they claim.",
                "The neighbor finally finished that deck.",
                "Forgot where I put the tape measure.",
                "Need to stop leaving tools in random places.",
                "The garage is somehow messy again.",
                "Maybe that loose cabinet handle is finally getting worse.",
                "Need more batteries."
            )
        )
    )
}

internal data class DemoNoteSeed(
    val id: Int,
    val title: String,
    val date: LocalDate,
    val pinnedText: List<String>,
    val bodyLines: List<String>
) {
    val body: String = bodyLines.joinToString(separator = "\n\n")
    val timestamp: Long = date
        .atTime(9, 0)
        .atZone(ZoneId.of("America/Chicago"))
        .toInstant()
        .toEpochMilli()
}
