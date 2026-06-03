package com.northsignalstudio.myram.intelligence

class NoteIntelligenceRuleEvaluator(
    private val spec: NoteIntelligenceRuleSpec
) {
    fun evaluate(input: NoteIntelligenceCanonicalInput): List<NoteIntelligenceSuggestion> {
        var suggestions = spec.rules
            .filter { matches(it, input) }
            .map { rule ->
                NoteIntelligenceSuggestion(
                    label = rule.label,
                    ruleId = rule.id,
                    score = (rule.priority.coerceIn(0, 100)) / 100.0,
                    explanations = listOf(rule.rationale)
                )
            }

        suggestions = applyEventOverlapGuardIfNeeded(suggestions, input)

        return suggestions
            .groupBy { it.label }
            .mapNotNull { (_, byLabel) -> byLabel.maxByOrNull { it.score } }
            .sortedWith(compareByDescending<NoteIntelligenceSuggestion> { it.score }.thenBy { it.label })
    }

    fun evaluateLabels(input: NoteIntelligenceCanonicalInput): List<String> {
        return evaluate(input).map { it.label }
    }

    private fun matches(
        rule: NoteIntelligenceRuleSpec.Rule,
        input: NoteIntelligenceCanonicalInput
    ): Boolean {
        val allConditions = rule.conditions["all"].orEmpty()
        val anyConditions = rule.conditions["any"].orEmpty()

        val allMatch = allConditions.all { condition(it, input) }
        val anyMatch = anyConditions.isEmpty() || anyConditions.any { condition(it, input) }

        return allMatch && anyMatch
    }

    private fun applyEventOverlapGuardIfNeeded(
        suggestions: List<NoteIntelligenceSuggestion>,
        input: NoteIntelligenceCanonicalInput
    ): List<NoteIntelligenceSuggestion> {
        val labels = suggestions.map { it.label }.toSet()
        if (!labels.contains("possible_event")) return suggestions

        val hasTaskOrReminder = labels.contains("possible_task") || labels.contains("reminder_candidate")
        val hasEventPhrase = condition("contains_event_phrase", input)

        return if (hasTaskOrReminder && !hasEventPhrase) {
            suggestions.filterNot { it.label == "possible_event" }
        } else {
            suggestions
        }
    }

    private fun condition(name: String, input: NoteIntelligenceCanonicalInput): Boolean {
        return when (name) {
            "contains_action_verb" -> {
                val verbs = setOf(
                    "call", "email", "send", "schedule", "book", "confirm", "submit", "pay", "finish",
                    "complete", "review", "prepare", "plan"
                )
                input.features.lemmas.map { it.lowercase() }.toSet().intersect(verbs).isNotEmpty()
            }

            "not_contains_action_verb" -> !condition("contains_action_verb", input)

            "has_datetime_entity" -> input.entities.datetimes.isNotEmpty()

            "contains_event_phrase" -> {
                val text = input.text.lowercase()
                listOf(
                    "meeting", "sync", "appointment", "calendar", "event", "standup", "call with", "demo"
                ).any { text.contains(it) }
            }

            "contains_followup_phrase" -> {
                val text = input.text.lowercase()
                listOf(
                    "follow up", "follow-up", "check in", "remind", "email", "circle back", "ping"
                ).any { text.contains(it) }
            }

            "has_datetime_or_contact_entity" -> {
                input.entities.datetimes.isNotEmpty() ||
                    input.entities.emails.isNotEmpty() ||
                    input.entities.phones.isNotEmpty()
            }

            "contains_idea_phrase" -> {
                val text = input.text.lowercase()
                listOf("idea", "brainstorm", "concept", "what if", "maybe build", "proposal")
                    .any { text.contains(it) }
            }

            "contains_reflective_phrase" -> {
                val text = input.text.lowercase()
                listOf(
                    "i felt", "i feel", "i realized", "i learned", "i noticed", "today i", "tonight"
                ).any { text.contains(it) }
            }

            "first_person_ratio_high" -> input.features.firstPersonRatio >= 0.25

            "open_count_above_threshold" -> input.features.openCount30d >= 10

            "edited_recently_multiple_times" -> input.features.editCount7d >= 3

            "text_similarity_above_threshold" -> input.similarNoteIds.isNotEmpty()

            "shares_topic_keywords" ->
                input.features.lemmas.isNotEmpty() && input.similarNoteIds.isNotEmpty()

            else -> false
        }
    }
}
