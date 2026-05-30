package com.apexcoretechs.myram.intelligence

import org.json.JSONArray
import org.json.JSONObject

data class NoteIntelligenceCanonicalInput(
    val noteId: String,
    val text: String,
    val language: String,
    val createdAt: String,
    val modifiedAt: String,
    val features: Features,
    val entities: Entities,
    val similarNoteIds: List<String> = emptyList()
) {
    data class Features(
        val lemmas: List<String>,
        val tokens: List<String>,
        val openCount30d: Int,
        val editCount7d: Int,
        val firstPersonRatio: Double,
        val ocrTokens: List<String> = emptyList()
    )

    data class Entities(
        val datetimes: List<String>,
        val emails: List<String>,
        val phones: List<String>,
        val urls: List<String>,
        val addresses: List<String>
    )
}

data class NoteIntelligenceSuggestion(
    val label: String,
    val ruleId: String,
    val score: Double,
    val explanations: List<String>
)

data class NoteIntelligenceRuleSpec(
    val specVersion: Int,
    val specName: String,
    val labels: List<String>,
    val rules: List<Rule>
) {
    data class Rule(
        val id: String,
        val label: String,
        val priority: Int,
        val conditions: Map<String, List<String>>,
        val rationale: String
    )
}

object NoteIntelligenceRuleSpecParser {
    fun parse(jsonText: String): NoteIntelligenceRuleSpec {
        val root = JSONObject(jsonText)
        val labels = root.getJSONArray("labels").toStringList()
        val rulesJson = root.getJSONArray("rules")

        val rules = buildList {
            for (index in 0 until rulesJson.length()) {
                val ruleObject = rulesJson.getJSONObject(index)
                val conditionsObject = ruleObject.getJSONObject("conditions")
                val conditionNames = mutableMapOf<String, List<String>>()

                conditionsObject.keys().forEach { key ->
                    conditionNames[key] = conditionsObject.getJSONArray(key).toStringList()
                }

                add(
                    NoteIntelligenceRuleSpec.Rule(
                        id = ruleObject.getString("id"),
                        label = ruleObject.getString("label"),
                        priority = ruleObject.getInt("priority"),
                        conditions = conditionNames,
                        rationale = ruleObject.getString("rationale")
                    )
                )
            }
        }

        return NoteIntelligenceRuleSpec(
            specVersion = root.getInt("spec_version"),
            specName = root.getString("spec_name"),
            labels = labels,
            rules = rules
        )
    }

    private fun JSONArray.toStringList(): List<String> {
        return buildList {
            for (index in 0 until length()) {
                add(getString(index))
            }
        }
    }
}
