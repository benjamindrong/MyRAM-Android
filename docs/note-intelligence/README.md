# Note Intelligence Spec v1

This directory defines the shared, versioned rule spec artifacts for note intelligence.

## Purpose

The spec is the source of truth for cross-platform suggestion behavior so iOS and Android can produce the same labels from equivalent canonical inputs.

## Files

- `note_intelligence_rules.v1.json`: Rule definitions and output labels.
- `contracts/note_intelligence_input.schema.v1.json`: Canonical input contract.
- `contracts/note_intelligence_output.schema.v1.json`: Canonical output contract.
- `fixtures/v1/*.json`: Deterministic input/output examples for parity tests.

## v1 Labels

- `possible_task`
- `possible_event`
- `reminder_candidate`
- `idea`
- `journal_entry`
- `high_revisit_value`
- `merge_candidate`

## Rules Philosophy

- Suggestions only; no automatic actions.
- Optional and user-controlled.
- On-device processing.
- Rule conditions should stay deterministic and auditable.

## Android Runtime Notes

- Suggestion generation runs on-device in the app process.
- Runtime extraction uses ML Kit language identification and ML Kit OCR for attachment-derived text signals.
- Rule evaluation is deterministic and local against `note_intelligence_rules.v1.json`.
- Suggestion generation does not require any network call in the runtime path; if ML Kit extraction is unavailable, extraction falls back to local heuristics.

## Rule/Spec Version Bump Process

1. Copy artifacts and increment version suffixes:
- `note_intelligence_rules.v{N+1}.json`
- `contracts/note_intelligence_input.schema.v{N+1}.json`
- `contracts/note_intelligence_output.schema.v{N+1}.json`

2. Update `spec_version`, labels/rules, and condition semantics in the new rule artifact.

3. Add fixture corpus for the new version under `fixtures/v{N+1}/` with expected labels.

4. Keep prior versions immutable for release auditability and backward compatibility.

5. Run cross-platform fixture parity validation before release.
