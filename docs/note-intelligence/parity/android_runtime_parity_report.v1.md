# Android Runtime Parity Report (v1)

Generated from fixture-driven runtime evaluator validation (`NoteIntelligenceSpecTest.runtimeEvaluator_matchesFixtureExpectedLabels_withParityReport`).

## Fixture Corpus

- `01_possible_task.json`
- `02_possible_event.json`
- `03_reminder_candidate.json`
- `04_idea.json`
- `05_journal_entry.json`
- `06_high_revisit_value.json`
- `07_merge_candidate.json`
- `08_multi_label.json`

## Mismatch Reporting

- The test emits a parity report in assertion output.
- Any mismatch includes fixture id plus expected vs actual label sets.
- No mismatch output indicates full parity for the fixture corpus.
