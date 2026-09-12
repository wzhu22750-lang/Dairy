# Handoff Report — Explorer 3: Domain, Business Logic & Build Environment Survey

## 1. Observation
Direct observations from the project codebase and build environment:
1. **Room Entities & DAOs**:
   - `AppDatabase.kt:15-24`: `@Database(entities = [DiaryEntity::class, AttachmentEntity::class, TagEntity::class, DiaryTagCrossRef::class], version = 1, exportSchema = false)`.
   - `AppDatabase.kt:40`: Configured with `.fallbackToDestructiveMigration()`.
   - `DiaryDao.kt:1-79`: 15 methods querying `diary_entries`, handling active/trash entries, date intervals, keyword search, "On This Day" (`strftime('%m-%d', ...)`), sync status updates, and soft/hard deletes.
   - `AttachmentDao.kt:8-24` and `TagDao.kt:8-30`: Manage foreign keys referencing `diary_entries.id` with `onDelete = ForeignKey.CASCADE`.
   - `DiaryWithDetails.kt:8-60`: Embeds `DiaryEntity` with `@Relation` for attachments and junction `@Relation` for tags, providing `toDomain(): Diary`.
2. **Security & PIN/Biometrics**:
   - `AppLockManager.kt:13-37`: Singleton with `isLocked: StateFlow<Boolean>` and `@Volatile var isPickerActive: Boolean = false`.
   - `MainActivity.kt:87-89`: `if (isLockEnabled && !AppLockManager.isPickerActive) { AppLockManager.lock() }` in `onStop()`.
   - `PinCipher.kt:18-67`: Hardware-backed AndroidKeyStore `AES/GCM/NoPadding` encryption, alias `"ink_paper_diary_pin_key"`, formatting ciphertexts as `"c1:<base64(IV)>:<base64(ciphertext)>"`.
   - `BiometricHelper.kt:9-54`: Uses `BiometricManager` with `BIOMETRIC_STRONG or DEVICE_CREDENTIAL` and `BiometricPrompt`.
   - `LockViewModel.kt:20-24, 80-88`: Enforces 4~6 digit PIN with 5 failed attempts triggering a 30-second lockout ticker.
3. **Cloud Sync & Backup/Import**:
   - `SupabaseClient.kt:14-178`: OkHttp client targeting Supabase `/rest/v1/diaries` (with `Prefer: resolution=merge-duplicates`), `/rest/v1/diaries?updated_at=gt...`, and `/storage/v1/object/diary-media/...`.
   - `SyncManager.kt:31-155`: Three-phase sync: (1) upload unsynced media, (2) push local unsynced entries, (3) pull remote entries with conflict priority given to local pending deletions (`syncStatus == 2`).
   - `SyncWorker.kt:40-74`: `PeriodicWorkRequestBuilder<SyncWorker>(1, TimeUnit.HOURS)` with exponential backoff of 15 minutes.
   - `BackupManager.kt:25-183`: Full JSON backup/import cycle and Markdown ZIP archive export (storing `.md` with YAML frontmatter + `images/` directory).
   - `TxtDiaryImporter.kt:27-490`: Dual encoding fallback (`UTF-8` with BOM strip -> `GB18030`), date header pattern regex, compact 8-digit date parsing, filename date fallback, and keyword detection for weather, mood, and hashtags.
4. **ViewModels & StateFlow Contracts**:
   - Nine ViewModels: `TimelineViewModel`, `CalendarViewModel`, `OnThisDayViewModel`, `EditorViewModel`, `SettingsViewModel`, `LockViewModel`, `SearchViewModel`, `StatsViewModel`, `TrashViewModel`.
   - All ViewModels expose immutable `StateFlow` bindings created with `SharingStarted.WhileSubscribed(5000)`.
5. **Build Environment & Execution**:
   - `gradle/libs.versions.toml:1-72` and `app/build.gradle.kts:8-45`: AGP `9.0.1`, Gradle `9.1.0`, Kotlin `2.3.20`, KSP `2.3.11`, Compose BOM `2026.03.01`, JVM toolchain 17, `compileSdk = 36`, `targetSdk = 36`, `minSdk = 26`.
   - Executing `./gradlew test --stacktrace`: Exit code 0, `BUILD SUCCESSFUL in 827ms`.
   - Executing `./gradlew assembleDebug`: Exit code 0, `BUILD SUCCESSFUL in 829ms`.

## 2. Logic Chain
1. *From Observation 1*: The database schema relies on 4 Room entities and foreign key cascade rules. To prevent data corruption during UI changes, no Room entities, DAOs, or `DiaryRepository` contracts should be modified.
2. *From Observation 2*: Application lock enforcement is tightly coupled with lifecycle callbacks and external intent pickers. Specifically, when invoking any system photo picker or file picker in the redesigned UI, omitting `AppLockManager.isPickerActive = true` will cause the app to lock itself upon user return. Therefore, all photo and file pickers in the new UI must preserve this flag.
3. *From Observation 3*: Synchronization and backup routines are fully independent of Compose UI and reside cleanly in `core/sync`, `core/network`, and `core/backup`. They interact solely with `AppDatabase` and `SettingsRepository`. UI components only trigger them via ViewModel methods (`performManualSync`, `exportMarkdownZip`, `exportJsonBackup`, `importJsonBackup`, `importTxtFiles`).
4. *From Observation 4*: All 9 ViewModels are decoupled from UI layouts, exposing standard `StateFlow` contracts. Refactoring screens to iOS HIG layouts (e.g. `TimelineScreen`, `SettingsScreen`, `EditorScreen`, `LockScreen`) can be done strictly within the UI layer without modifying ViewModel business logic.
5. *From Observation 5*: Gradle 9.1.0 with AGP 9.0.1 and Kotlin 2.3.20 is configured cleanly and builds successfully in under 1 second. Existing unit tests (`BackupManagerTest`, `DiaryModelTest`, `TxtDiaryImporterTest`) compile and pass.

## 3. Caveats
- No unit tests currently exist for `SyncManager` or `PinCipher` (only `BackupManagerTest`, `DiaryModelTest`, and `TxtDiaryImporterTest` exist).
- `androidTest/` contains an empty folder structure; there are currently no instrumented UI tests in the project.
- No Supabase remote backend credentials are configured in local git; cloud tests rely on mocking or user input at runtime.

## 4. Conclusion
1. All non-UI domains (Room database, security/biometrics, cloud sync, backup pipelines, and ViewModels) are well-architected, stable, and completely decoupled from UI presentation.
2. The UI overhaul to Apple HIG (R1, R2, R3) can proceed with strict isolation: zero changes are needed or permitted in `core/database`, `core/security`, `core/sync`, `core/backup`, `core/network`, or `data/repository`.
3. The build environment compiles cleanly via `./gradlew assembleDebug` and passes all tests via `./gradlew test`.

## 5. Verification Method
To independently verify the non-UI domain and build health:
1. **Compilation verification**:
   ```bash
   ./gradlew assembleDebug
   ```
   *Expected output*: `BUILD SUCCESSFUL` with 0 errors.
2. **Unit test verification**:
   ```bash
   ./gradlew test
   ```
   *Expected output*: `BUILD SUCCESSFUL` with all tests passing.
3. **Inspect detailed report**:
   View `/Users/kuangqie/Documents/VibeCoding/日记本/.agents/explorer_survey_3/survey_domain_and_build.md`.
4. **Invalidation condition**: Any change to `core/database/entity`, `core/database/dao`, `core/security`, or `core/sync` that causes compilation or unit tests to fail indicates a boundary violation.
