package com.northsignalstudio.myram.intelligence

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Patterns
import com.northsignalstudio.myram.data.Note
import com.northsignalstudio.myram.data.NotePhotoAttachment
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.math.max

private const val ACTIVITY_PREFS_NAME = "note_intelligence_activity"
private const val OPEN_PREFIX = "open."
private const val EDIT_PREFIX = "edit."
private const val OPEN_WINDOW_MILLIS = 30L * 24 * 60 * 60 * 1000
private const val EDIT_WINDOW_MILLIS = 7L * 24 * 60 * 60 * 1000

class NoteIntelligenceService(
    appContext: Context,
    private val specProvider: NoteIntelligenceRuleSpecProvider = NoteIntelligenceRuleSpecProvider(appContext),
    private val extractor: MlKitNoteIntelligenceExtractor = MlKitNoteIntelligenceExtractor()
) {
    private val activityTracker = NoteIntelligenceActivityTracker(appContext)
    private var evaluator: NoteIntelligenceRuleEvaluator? = null

    suspend fun suggestionLabelsFor(
        note: Note,
        allActiveNotes: List<Note>,
        attachments: List<NotePhotoAttachment>
    ): List<String> {
        if (note.title.isBlank() && note.content.isBlank()) {
            return emptyList()
        }

        val fullText = combinedText(note.title, note.content)
        val extraction = extractor.extract(
            text = fullText,
            attachmentImageData = attachments.map { it.imageData }
        )
        val now = System.currentTimeMillis()
        val (openCount30d, editCount7d) = activityTracker.metrics(noteId = note.id, now = now)

        val similarNoteIds = findSimilarNoteIds(note, allActiveNotes)
        val canonicalInput = NoteIntelligenceCanonicalInput(
            noteId = note.id.toString(),
            text = fullText,
            language = extraction.language,
            createdAt = toIsoString(note.createdAt),
            modifiedAt = toIsoString(note.lastModified),
            features = NoteIntelligenceCanonicalInput.Features(
                lemmas = extraction.lemmas,
                tokens = extraction.tokens,
                openCount30d = openCount30d,
                editCount7d = editCount7d,
                firstPersonRatio = extraction.firstPersonRatio,
                ocrTokens = extraction.ocrTokens
            ),
            entities = NoteIntelligenceCanonicalInput.Entities(
                datetimes = extraction.entities.datetimes,
                emails = extraction.entities.emails,
                phones = extraction.entities.phones,
                urls = extraction.entities.urls,
                addresses = extraction.entities.addresses
            ),
            similarNoteIds = similarNoteIds
        )

        val engine = evaluator ?: NoteIntelligenceRuleEvaluator(specProvider.load()).also { evaluator = it }
        return engine.evaluateLabels(canonicalInput)
    }

    suspend fun recognizedTextFor(attachment: NotePhotoAttachment): String {
        return extractor.recognizeText(listOf(attachment.imageData))
    }

    fun evaluateCanonicalInput(input: NoteIntelligenceCanonicalInput): List<String> {
        val engine = evaluator ?: NoteIntelligenceRuleEvaluator(specProvider.load()).also { evaluator = it }
        return engine.evaluateLabels(input)
    }

    fun recordOpen(noteId: Int, now: Long = System.currentTimeMillis()) {
        activityTracker.recordOpen(noteId, now)
    }

    fun recordEdit(noteId: Int, now: Long = System.currentTimeMillis()) {
        activityTracker.recordEdit(noteId, now)
    }

    private fun combinedText(title: String, content: String): String {
        val cleanTitle = title.trim()
        val cleanContent = content.trim()
        return when {
            cleanTitle.isEmpty() -> cleanContent
            cleanContent.isEmpty() -> cleanTitle
            else -> "$cleanTitle. $cleanContent"
        }
    }

    private fun toIsoString(epochMillis: Long): String {
        return DateTimeFormatter.ISO_INSTANT
            .withZone(ZoneOffset.UTC)
            .format(Instant.ofEpochMilli(epochMillis))
    }

    private fun findSimilarNoteIds(note: Note, allActiveNotes: List<Note>): List<String> {
        val currentTokens = keywordSet(combinedText(note.title, note.content))
        if (currentTokens.isEmpty()) return emptyList()

        return allActiveNotes
            .asSequence()
            .filter { it.id != note.id }
            .mapNotNull { candidate ->
                val candidateTokens = keywordSet(combinedText(candidate.title, candidate.content))
                if (candidateTokens.isEmpty()) return@mapNotNull null

                val overlap = currentTokens.intersect(candidateTokens)
                val union = currentTokens.union(candidateTokens)
                val similarity = if (union.isEmpty()) 0.0 else overlap.size.toDouble() / union.size.toDouble()

                if (similarity >= 0.55 && overlap.size >= 2) {
                    candidate.id.toString()
                } else {
                    null
                }
            }
            .toList()
            .sorted()
    }

    private fun keywordSet(text: String): Set<String> {
        val stopWords = setOf(
            "a", "an", "and", "the", "to", "of", "for", "in", "on", "at", "with", "by", "is",
            "are", "was", "were", "be", "it", "this", "that", "from", "as", "or", "about", "after",
            "before", "into", "up", "down", "out", "off", "over", "under", "then", "than"
        )
        return Regex("[A-Za-z0-9]+")
            .findAll(text.lowercase())
            .map { it.value }
            .filter { it.length >= 3 && it !in stopWords }
            .toSet()
    }
}

class NoteIntelligenceRuleSpecProvider(private val context: Context) {
    fun load(): NoteIntelligenceRuleSpec {
        val jsonText = runCatching {
            context.assets.open("note-intelligence/note_intelligence_rules.v1.json")
                .bufferedReader()
                .use { it.readText() }
        }.getOrElse {
            FALLBACK_RULE_SPEC_JSON
        }

        return NoteIntelligenceRuleSpecParser.parse(jsonText)
    }

    companion object {
        private const val FALLBACK_RULE_SPEC_JSON = """
        {
          "spec_version": 1,
          "spec_name": "note_intelligence_rules",
          "labels": [
            "possible_task",
            "possible_event",
            "reminder_candidate",
            "idea",
            "journal_entry",
            "high_revisit_value",
            "merge_candidate"
          ],
          "rules": [
            {
              "id": "task_verbs_with_due_entity",
              "label": "possible_task",
              "priority": 90,
              "conditions": {"all": ["contains_action_verb", "has_datetime_entity"]},
              "rationale": "Action language paired with a date/time entity likely represents a task."
            },
            {
              "id": "calendar_phrase_or_datetime",
              "label": "possible_event",
              "priority": 85,
              "conditions": {"any": ["contains_event_phrase", "has_datetime_entity"]},
              "rationale": "Event-oriented phrases or date/time entities indicate a possible event."
            },
            {
              "id": "followup_with_date_or_contact",
              "label": "reminder_candidate",
              "priority": 80,
              "conditions": {"all": ["contains_followup_phrase", "has_datetime_or_contact_entity"]},
              "rationale": "Follow-up language with scheduling/contact signals suggests a reminder candidate."
            },
            {
              "id": "idea_language_without_task_signals",
              "label": "idea",
              "priority": 65,
              "conditions": {"all": ["contains_idea_phrase", "not_contains_action_verb"]},
              "rationale": "Idea language without action intent maps to idea classification."
            },
            {
              "id": "reflective_first_person_text",
              "label": "journal_entry",
              "priority": 60,
              "conditions": {"all": ["contains_reflective_phrase", "first_person_ratio_high"]},
              "rationale": "Reflective first-person language indicates journaling content."
            },
            {
              "id": "note_high_access_or_recent_edits",
              "label": "high_revisit_value",
              "priority": 70,
              "conditions": {"any": ["open_count_above_threshold", "edited_recently_multiple_times"]},
              "rationale": "Frequently revisited notes may deserve easier access."
            },
            {
              "id": "high_similarity_to_existing_note",
              "label": "merge_candidate",
              "priority": 75,
              "conditions": {"all": ["text_similarity_above_threshold", "shares_topic_keywords"]},
              "rationale": "Strong textual/topic overlap suggests merge consideration."
            }
          ]
        }
        """
    }
}

class NoteIntelligenceActivityTracker(context: Context) {
    private val prefs = context.getSharedPreferences(ACTIVITY_PREFS_NAME, Context.MODE_PRIVATE)

    fun recordOpen(noteId: Int, now: Long) {
        val key = "$OPEN_PREFIX$noteId"
        val values = decodeTimestamps(prefs.getString(key, null)) + now
        prefs.edit().putString(key, encodeTimestamps(prune(values, now - OPEN_WINDOW_MILLIS))).apply()
    }

    fun recordEdit(noteId: Int, now: Long) {
        val key = "$EDIT_PREFIX$noteId"
        val current = decodeTimestamps(prefs.getString(key, null))
        val mostRecent = current.maxOrNull()
        if (mostRecent != null && now - mostRecent < 120_000) {
            prefs.edit().putString(key, encodeTimestamps(prune(current, now - EDIT_WINDOW_MILLIS))).apply()
            return
        }

        val values = current + now
        prefs.edit().putString(key, encodeTimestamps(prune(values, now - EDIT_WINDOW_MILLIS))).apply()
    }

    fun metrics(noteId: Int, now: Long): Pair<Int, Int> {
        val openValues = decodeTimestamps(prefs.getString("$OPEN_PREFIX$noteId", null))
        val editValues = decodeTimestamps(prefs.getString("$EDIT_PREFIX$noteId", null))

        val openCount = prune(openValues, now - OPEN_WINDOW_MILLIS).size
        val editCount = prune(editValues, now - EDIT_WINDOW_MILLIS).size
        return openCount to editCount
    }

    private fun decodeTimestamps(raw: String?): List<Long> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw.split(',').mapNotNull { it.toLongOrNull() }
    }

    private fun encodeTimestamps(values: List<Long>): String {
        return values.joinToString(",")
    }

    private fun prune(values: List<Long>, cutoff: Long): List<Long> {
        return values.filter { it >= cutoff }.sorted()
    }
}

class MlKitNoteIntelligenceExtractor {
    data class Extraction(
        val language: String,
        val tokens: List<String>,
        val lemmas: List<String>,
        val firstPersonRatio: Double,
        val entities: NoteIntelligenceCanonicalInput.Entities,
        val ocrTokens: List<String>
    )

    suspend fun extract(text: String, attachmentImageData: List<ByteArray>): Extraction {
        val tokens = tokenize(text)
        val lemmas = tokens.map { normalizeLemma(it) }
        val ocrText = recognizeText(attachmentImageData)
        val ocrTokens = tokenize(ocrText)

        val language = detectLanguage(text)
        val entities = detectEntities(text, ocrText)

        return Extraction(
            language = language,
            tokens = tokens,
            lemmas = lemmas,
            firstPersonRatio = firstPersonRatio(tokens),
            entities = entities,
            ocrTokens = ocrTokens
        )
    }

    private suspend fun detectLanguage(text: String): String {
        if (text.isBlank()) return "und"

        return runCatching {
            val identifier = LanguageIdentification.getClient()
            try {
                identifier.identifyLanguage(text).await().ifBlank { "und" }
            } finally {
                identifier.close()
            }
        }.getOrElse { "und" }
    }

    suspend fun recognizeText(imageData: List<ByteArray>): String {
        if (imageData.isEmpty()) return ""

        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val recognizedParts = mutableListOf<String>()

        try {
            imageData.forEach { bytes ->
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return@forEach
                val image = InputImage.fromBitmap(bitmap, 0)
                val text = runCatching { recognizer.process(image).await().text }.getOrNull().orEmpty()
                if (text.isNotBlank()) {
                    recognizedParts += text
                }
            }
        } finally {
            recognizer.close()
        }

        return recognizedParts.joinToString("\n")
    }

    private fun tokenize(text: String): List<String> {
        if (text.isBlank()) return emptyList()
        return Regex("[A-Za-z0-9]+'?[A-Za-z0-9]*")
            .findAll(text)
            .map { it.value }
            .toList()
    }

    private fun normalizeLemma(token: String): String {
        var candidate = token.lowercase().trim()
        if (candidate.endsWith("ing") && candidate.length > 5) {
            candidate = candidate.dropLast(3)
        } else if (candidate.endsWith("ed") && candidate.length > 4) {
            candidate = candidate.dropLast(2)
        } else if (candidate.endsWith("s") && candidate.length > 3) {
            candidate = candidate.dropLast(1)
        }
        return candidate
    }

    private fun firstPersonRatio(tokens: List<String>): Double {
        if (tokens.isEmpty()) return 0.0

        val firstPerson = setOf("i", "me", "my", "mine", "myself", "we", "us", "our", "ours", "ourselves")
        val count = tokens.count { firstPerson.contains(it.lowercase()) }
        return max(0.0, count.toDouble() / tokens.size.toDouble())
    }

    private fun detectEntities(
        noteText: String,
        ocrText: String
    ): NoteIntelligenceCanonicalInput.Entities {
        val combinedText = listOf(noteText, ocrText).filter { it.isNotBlank() }.joinToString("\n")

        val datetimes = linkedSetOf<String>()
        val datetimeRegex = Regex(
            pattern = """\b(?:tomorrow|today|tonight|next\s+week|next\s+month|monday|tuesday|wednesday|thursday|friday|saturday|sunday|\d{1,2}:\d{2}\s?(?:am|pm)|\d{1,2}\s?(?:am|pm))\b""",
            option = RegexOption.IGNORE_CASE
        )
        datetimeRegex.findAll(combinedText).forEach { datetimes += it.value.trim() }

        val emails = linkedSetOf<String>()
        Patterns.EMAIL_ADDRESS.matcher(combinedText).apply {
            while (find()) {
                emails += group().orEmpty().trim()
            }
        }

        val phones = linkedSetOf<String>()
        Patterns.PHONE.matcher(combinedText).apply {
            while (find()) {
                val candidate = group().orEmpty().trim()
                val digits = candidate.count { it.isDigit() }
                if (digits >= 7) {
                    phones += candidate
                }
            }
        }

        val urls = linkedSetOf<String>()
        Patterns.WEB_URL.matcher(combinedText).apply {
            while (find()) {
                val candidate = group().orEmpty().trim()
                if (candidate.contains('.')) {
                    urls += candidate
                }
            }
        }

        val addresses = linkedSetOf<String>()
        val addressRegex = Regex(
            pattern = """\b\d{1,5}\s+[A-Za-z0-9\s]{2,40}\s(?:st|street|ave|avenue|rd|road|blvd|boulevard|lane|ln|dr|drive)\b""",
            option = RegexOption.IGNORE_CASE
        )
        addressRegex.findAll(combinedText).forEach { addresses += it.value.trim() }

        return NoteIntelligenceCanonicalInput.Entities(
            datetimes = datetimes.toList(),
            emails = emails.toList(),
            phones = phones.toList(),
            urls = urls.toList(),
            addresses = addresses.toList()
        )
    }
}
