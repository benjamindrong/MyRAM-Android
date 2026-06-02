# MYR-52 Branding Audit

## Current Values

- App/repository name: `MyRAM`
- Android app label: `MyRAM`
- Gradle root project name: `MyRAM`
- Android namespace: `com.apexcoretechs.myram`
- Android application ID: `com.apexcoretechs.myram`
- File provider authority: `${applicationId}.fileprovider`
- Local database name: `myram.db`
- Shared preferences name: `myram_prefs`
- Rich text storage prefixes:
  - `[myram-rich-text-v2]`
  - `[myram-rich-text-v1]`
- Export naming:
  - `MyRAM-Notes-<timestamp>.json`
  - `MyRAM-Export-<timestamp>.zip`
- Note intelligence schema domains:
  - `https://apexcoretechs.com/schemas/note_intelligence_input.schema.v1.json`
  - `https://apexcoretechs.com/schemas/note_intelligence_output.schema.v1.json`

## Values to Decide Before Play Store Submission

- Final publisher/developer account name for Play Store listings.
- Final production application ID.
- Whether `apexcoretechs.com` is the final public domain for schema IDs, support, privacy, and app metadata.
- Public support URL.
- Public privacy policy URL.
- Public terms URL, if needed.
- Final store-facing app name and short description.
- Final release app label if it should differ from `MyRAM`.

## Hardcoded Project Locations

- Namespace and application ID are set in `app/build.gradle.kts`.
- App label is set in `app/src/main/AndroidManifest.xml` and `app/src/main/res/values/strings.xml`.
- File provider authority is set in `app/src/main/AndroidManifest.xml`.
- Database name and shared preferences name are set in:
  - `app/src/main/java/com/apexcoretechs/myram/data/Repository.kt`
  - `app/src/main/java/com/apexcoretechs/myram/MainActivity.kt`
  - `app/src/main/java/com/apexcoretechs/myram/ui/NotesViewModel.kt`
- Export filenames are hardcoded in:
  - `app/src/main/java/com/apexcoretechs/myram/export/NoteExporter.kt`
  - `app/src/main/java/com/apexcoretechs/myram/MainActivity.kt`
- Rich text prefixes are hardcoded in `app/src/main/java/com/apexcoretechs/myram/ui/richtext/RichTextContent.kt`.
- Note intelligence schema IDs are hardcoded in:
  - `docs/note-intelligence/contracts/note_intelligence_input.schema.v1.json`
  - `docs/note-intelligence/contracts/note_intelligence_output.schema.v1.json`

## Notes

- No support URL, privacy policy URL, terms URL, or publisher name was found in the Android project.
- No placeholder support or store URLs were found.
- No final branding values were invented for this audit.
