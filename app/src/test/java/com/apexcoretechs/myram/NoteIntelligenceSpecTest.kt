package com.apexcoretechs.myram

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

    private fun repoRoot(): File {
        return File(System.getProperty("user.dir"))
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
