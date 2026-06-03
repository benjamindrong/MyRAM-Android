package com.northsignalstudio.myram

import com.northsignalstudio.myram.intelligence.NoteIntelligenceCanonicalInput
import com.northsignalstudio.myram.intelligence.NoteIntelligenceRuleEvaluator
import com.northsignalstudio.myram.intelligence.NoteIntelligenceRuleSpecParser
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.Instant

class NoteIntelligenceSpecTest {

    @Test
    fun ruleSpecV1_hasExpectedVersion_uniqueRuleIds_andKnownLabels() {
        val spec = readJson("docs/note-intelligence/note_intelligence_rules.v1.json")

        assertEquals(1, spec.getInt("spec_version"))
        assertEquals("note_intelligence_rules", spec.getString("spec_name"))

        val labels = spec.getJSONArray("labels").toStringSet()
        assertFalse(labels.isEmpty())

        val rules = spec.getJSONArray("rules")
        assertTrue(rules.length() > 0)

        val seenRuleIds = mutableSetOf<String>()
        for (i in 0 until rules.length()) {
            val rule = rules.getJSONObject(i)
            val id = rule.getString("id")
            assertTrue("Rule IDs must be unique.", seenRuleIds.add(id))

            val label = rule.getString("label")
            assertTrue("Rule label must be in labels list.", labels.contains(label))

            val priority = rule.getInt("priority")
            assertTrue("Priority must be between 0 and 100.", priority in 0..100)
        }
    }

    @Test
    fun fixtures_useOnlyKnownLabels_andHaveExpectedCount() {
        val spec = readJson("docs/note-intelligence/note_intelligence_rules.v1.json")
        val knownLabels = spec.getJSONArray("labels").toStringSet()

        val fixtureFiles = fixtureFiles()
        assertEquals(8, fixtureFiles.size)

        fixtureFiles.forEach { file ->
            val fixture = readJson(file.relativeTo(repoRoot()).invariantSeparatorsPath)
            val expectedLabels = fixture.getJSONArray("expected_labels").toStringList()

            assertTrue("Fixture must include expected labels: ${file.name}", expectedLabels.isNotEmpty())
            assertEquals(
                "Fixture labels must be unique: ${file.name}",
                expectedLabels.toSet().size,
                expectedLabels.size
            )
            assertTrue(
                "Fixture has unknown label: ${file.name}",
                expectedLabels.all { knownLabels.contains(it) }
            )
        }
    }

    @Test
    fun fixtures_haveBaselineCanonicalInputShape() {
        fixtureFiles().forEach { file ->
            val fixture = readJson(file.relativeTo(repoRoot()).invariantSeparatorsPath)
            val input = fixture.getJSONObject("input")
            val features = input.getJSONObject("features")

            assertTrue(fixture.getString("fixture_id").isNotBlank())
            assertTrue(input.getString("note_id").isNotBlank())
            assertTrue(input.getString("text").isNotBlank())
            assertTrue(input.getString("language").length >= 2)
            assertTrue(features.getInt("open_count_30d") >= 0)
            assertTrue(features.getInt("edit_count_7d") >= 0)

            val firstPersonRatio = features.getDouble("first_person_ratio")
            assertTrue(firstPersonRatio >= 0.0)
            assertTrue(firstPersonRatio <= 1.0)

            Instant.parse(input.getString("created_at"))
            Instant.parse(input.getString("modified_at"))

            val entities = input.getJSONObject("entities")
            entities.getJSONArray("datetimes")
            entities.getJSONArray("emails")
            entities.getJSONArray("phones")
            entities.getJSONArray("urls")
            entities.getJSONArray("addresses")
            input.getJSONArray("similar_note_ids")
        }
    }

    @Test
    fun ruleSpecV1_usesSupportedConditionKeysAndNames() {
        val specJson = readJson("docs/note-intelligence/note_intelligence_rules.v1.json")
        val spec = NoteIntelligenceRuleSpecParser.parse(specJson.toString())
        val supportedConditions = setOf(
            "contains_action_verb",
            "has_datetime_entity",
            "contains_event_phrase",
            "contains_followup_phrase",
            "has_datetime_or_contact_entity",
            "contains_idea_phrase",
            "not_contains_action_verb",
            "contains_reflective_phrase",
            "first_person_ratio_high",
            "open_count_above_threshold",
            "edited_recently_multiple_times",
            "text_similarity_above_threshold",
            "shares_topic_keywords"
        )

        spec.rules.forEach { rule ->
            rule.conditions.forEach { (key, conditionNames) ->
                assertTrue(
                    "Unsupported condition key in ${rule.id}: $key",
                    key == "all" || key == "any"
                )
                conditionNames.forEach { name ->
                    assertTrue(
                        "Unsupported condition name in ${rule.id}: $name",
                        supportedConditions.contains(name)
                    )
                }
            }
        }
    }

    @Test
    fun runtimeEvaluator_matchesFixtureExpectedLabels_withParityReport() {
        val spec = NoteIntelligenceRuleSpecParser.parse(
            readJson("docs/note-intelligence/note_intelligence_rules.v1.json").toString()
        )
        val evaluator = NoteIntelligenceRuleEvaluator(spec)
        val mismatches = mutableListOf<String>()

        fixtureFiles().forEach { file ->
            val fixture = readJson(file.relativeTo(repoRoot()).invariantSeparatorsPath)
            val fixtureId = fixture.getString("fixture_id")
            val expected = fixture.getJSONArray("expected_labels").toStringList().toSet()
            val actual = evaluator.evaluateLabels(canonicalInputFromFixture(fixture)).toSet()
            if (expected != actual) {
                mismatches += "- $fixtureId: expected=$expected actual=$actual"
            }
        }

        assertTrue(
            buildString {
                appendLine("Android runtime parity report (v1):")
                if (mismatches.isEmpty()) {
                    appendLine("- No mismatches detected.")
                } else {
                    mismatches.forEach(::appendLine)
                }
            },
            mismatches.isEmpty()
        )
    }

    @Test
    fun bundledRulesAsset_matchesDocsRuleArtifact() {
        val docsFile = File(repoRoot(), "docs/note-intelligence/note_intelligence_rules.v1.json")
        val assetFile = File(repoRoot(), "app/src/main/assets/note-intelligence/note_intelligence_rules.v1.json")
        assertTrue("Missing docs rules artifact.", docsFile.exists())
        assertTrue("Missing bundled rules asset.", assetFile.exists())
        assertEquals(docsFile.readText(Charsets.UTF_8), assetFile.readText(Charsets.UTF_8))
    }

    private fun fixtureFiles(): List<File> {
        val dir = File(repoRoot(), "docs/note-intelligence/fixtures/v1")
        return dir.listFiles { f -> f.isFile && f.extension.equals("json", ignoreCase = true) }
            ?.sortedBy { it.name }
            ?: emptyList()
    }

    private fun readJson(relativePath: String): JSONObject {
        val file = File(repoRoot(), relativePath)
        return JSONObject(file.readText(Charsets.UTF_8))
    }

    private fun canonicalInputFromFixture(fixture: JSONObject): NoteIntelligenceCanonicalInput {
        val input = fixture.getJSONObject("input")
        val features = input.getJSONObject("features")
        val entities = input.getJSONObject("entities")

        return NoteIntelligenceCanonicalInput(
            noteId = input.getString("note_id"),
            text = input.getString("text"),
            language = input.getString("language"),
            createdAt = input.getString("created_at"),
            modifiedAt = input.getString("modified_at"),
            features = NoteIntelligenceCanonicalInput.Features(
                lemmas = features.getJSONArray("lemmas").toStringList(),
                tokens = features.getJSONArray("tokens").toStringList(),
                openCount30d = features.getInt("open_count_30d"),
                editCount7d = features.getInt("edit_count_7d"),
                firstPersonRatio = features.getDouble("first_person_ratio")
            ),
            entities = NoteIntelligenceCanonicalInput.Entities(
                datetimes = entities.getJSONArray("datetimes").toStringList(),
                emails = entities.getJSONArray("emails").toStringList(),
                phones = entities.getJSONArray("phones").toStringList(),
                urls = entities.getJSONArray("urls").toStringList(),
                addresses = entities.getJSONArray("addresses").toStringList()
            ),
            similarNoteIds = input.getJSONArray("similar_note_ids").toStringList()
        )
    }

    private fun repoRoot(): File {
        val start = File(System.getProperty("user.dir") ?: ".").absoluteFile
        var current: File? = start
        while (current != null) {
            val candidate = File(current, "docs/note-intelligence")
            if (candidate.exists()) {
                return current
            }
            current = current.parentFile
        }
        return start
    }

    private fun JSONArray.toStringSet(): Set<String> {
        val values = mutableSetOf<String>()
        for (i in 0 until length()) {
            values.add(getString(i))
        }
        return values
    }

    private fun JSONArray.toStringList(): List<String> {
        val values = mutableListOf<String>()
        for (i in 0 until length()) {
            values.add(getString(i))
        }
        return values
    }
}
