# Domain, Business Logic & Build Environment Survey Report

**Author**: Explorer 3 (Domain, Business Logic & Build Environment Survey)  
**Date**: 2026-09-06  
**Target Package**: `com.example.inkpaperdiary`  
**Project Root**: `/Users/kuangqie/Documents/VibeCoding/日记本`  
**Objective**: Comprehensive survey of Room entities/DAOs, Security/Biometrics, Sync/Backup pipelines, ViewModels & StateFlow contracts, and Gradle build/test infrastructure to ensure 100% preservation (R4 Zero Regression) during iOS HIG UI refactoring.

---

## 1. Room Database Architecture & DAOs

### 1.1 Database Configuration
- **Class**: `com.example.inkpaperdiary.core.database.AppDatabase` (`app/src/main/java/com/example/inkpaperdiary/core/database/AppDatabase.kt:15-46`)
- **Database Name**: `"ink_paper_diary.db"`
- **Version**: `1`
- **Schema Export**: `exportSchema = false`
- **Migration Strategy**: `.fallbackToDestructiveMigration()`
- **Singleton**: `AppDatabase.getInstance(context: Context)` with double-checked locking (`@Volatile INSTANCE`).

### 1.2 Entity Specifications

| Entity | Table Name | Primary Key | Columns & Constraints | Foreign Keys & Indices |
| :--- | :--- | :--- | :--- | :--- |
| **`DiaryEntity`** | `diary_entries` | `id: String` | `id`, `userId` (default: `"local_guest"`), `title`, `contentMarkdown`, `mood` (default `"CALM"`), `weather` (default `"SUNNY"`), `locationName`, `latitude`, `longitude`, `entryDate`, `createdAt`, `updatedAt`, `isPinned`, `isDeleted`, `deletedAt`, `syncStatus` (0=SYNCED, 1=DIRTY, 2=DELETED) | Indices on: `entryDate`, `isDeleted`, `isPinned`, `syncStatus` |
| **`TagEntity`** | `tags` | `id: String` | `id`, `name`, `colorHex` (default `"#9E3323"`), `createdAt` | Unique index on: `name` |
| **`AttachmentEntity`** | `diary_attachments` | `id: String` | `id`, `diaryId`, `localPath`, `remoteUrl`, `fileName`, `fileSize`, `mimeType` (default `"image/webp"`), `sortOrder`, `syncStatus` (0=SYNCED, 1=DIRTY, 2=DELETED) | Foreign Key: references `diary_entries(id)` with `onDelete = CASCADE`. Index on: `diaryId` |
| **`DiaryTagCrossRef`** | `diary_tag_cross_ref` | Composite: `["diaryId", "tagId"]` | `diaryId`, `tagId` | Foreign Keys: references `diary_entries(id)` (`CASCADE`) and `tags(id)` (`CASCADE`). Indices on `diaryId`, `tagId` |

### 1.3 Relational Model: `DiaryWithDetails`
- **Location**: `app/src/main/java/com/example/inkpaperdiary/core/database/entity/DiaryWithDetails.kt`
- **Structure**:
  - `@Embedded val diary: DiaryEntity`
  - `@Relation(parentColumn = "id", entityColumn = "diaryId") val attachments: List<AttachmentEntity>`
  - `@Relation(parentColumn = "id", entityColumn = "id", associateBy = Junction(value = DiaryTagCrossRef::class, parentColumn = "diaryId", entityColumn = "tagId")) val tags: List<TagEntity>`
- **Domain Mapper**: `fun toDomain(): Diary` maps entity hierarchies into immutable domain model `Diary`, restoring enum types `Mood.fromCode`, `Weather.fromCode`, `SyncStatus.fromCode`, and sorting attachments by `sortOrder`.

### 1.4 DAO Contracts

#### `DiaryDao` (`app/src/main/java/com/example/inkpaperdiary/core/database/dao/DiaryDao.kt`)
- `fun getAllDiaries(): Flow<List<DiaryWithDetails>>` — active (non-deleted) entries ordered by `isPinned DESC, entryDate DESC`.
- `suspend fun getDiaryById(id: String): DiaryWithDetails?` — query non-deleted single entry.
- `suspend fun getDiaryEntityAnyStatus(id: String): DiaryEntity?` — query entry regardless of `isDeleted` (critical for sync conflict resolution and togglePin).
- `fun getDiariesBetweenDates(startTime: Long, endTime: Long): Flow<List<DiaryWithDetails>>` — date range query for calendar.
- `fun searchDiaries(query: String): Flow<List<DiaryWithDetails>>` — SQL `LIKE` query matching `title`, `contentMarkdown`, or `locationName`.
- `suspend fun getOnThisDay(monthDay: String): List<DiaryWithDetails>` — matches `strftime('%m-%d', datetime(entryDate / 1000, 'unixepoch', 'localtime'))`.
- `fun getTrashDiaries(): Flow<List<DiaryWithDetails>>` — soft-deleted entries ordered by `deletedAt DESC`.
- `suspend fun getUnsyncedDiaries(): List<DiaryWithDetails>` — entries with `syncStatus != 0`.
- `suspend fun insertOrUpdate(diary: DiaryEntity)` — `@Insert(onConflict = REPLACE)`.
- `suspend fun insertAll(diaries: List<DiaryEntity>)`.
- `suspend fun softDelete(id: String, deletedAt: Long)` — sets `isDeleted = 1`, `deletedAt`, `syncStatus = 2`.
- `suspend fun restoreFromTrash(id: String)` — sets `isDeleted = 0, deletedAt = NULL, syncStatus = 1`.
- `suspend fun hardDelete(id: String)` — removes from table (`CASCADE` triggers for attachments/cross refs).
- `suspend fun purgeOldTrash(cutoffTime: Long)` — deletes entries soft-deleted before `cutoffTime`.
- `suspend fun updateSyncStatus(id: String, newStatus: Int)`.
- `suspend fun updatePinned(id: String, pinned: Boolean)` — updates `isPinned` and sets `syncStatus = 1`.
- `fun getDiaryCount(): Flow<Int>`.

#### `TagDao` (`app/src/main/java/com/example/inkpaperdiary/core/database/dao/TagDao.kt`)
- `fun getAllTags(): Flow<List<TagEntity>>`
- `suspend fun getTagByName(name: String): TagEntity?`
- `suspend fun insertTag(tag: TagEntity): Long` (`OnConflictStrategy.IGNORE`)
- `suspend fun insertCrossRef(crossRef: DiaryTagCrossRef)`
- `suspend fun insertAllCrossRefs(crossRefs: List<DiaryTagCrossRef>)`
- `suspend fun deleteCrossRefsForDiary(diaryId: String)`
- `suspend fun deleteTag(id: String)`

#### `AttachmentDao` (`app/src/main/java/com/example/inkpaperdiary/core/database/dao/AttachmentDao.kt`)
- `fun getAttachmentsForDiary(diaryId: String): Flow<List<AttachmentEntity>>`
- `suspend fun insertOrUpdate(attachment: AttachmentEntity)`
- `suspend fun insertAll(attachments: List<AttachmentEntity>)`
- `suspend fun deleteForDiary(diaryId: String)`
- `suspend fun deleteById(id: String)`

### 1.5 Repository Layer: `DiaryRepository` Integrity Boundaries
In `DiaryRepository.kt:62-130`:
1. `saveDiary` handles attachment synchronization: removes obsolete attachment files on disk, updates/inserts existing attachments, preserves `syncStatus = 0` if `remoteUrl` already exists.
2. `saveDiary` reuses existing tag IDs for identical tag names to avoid cascade deletion caused by SQLite `REPLACE` with unique index constraints.
3. `togglePin` uses `getDiaryEntityAnyStatus` and `updatePinned` without rewriting tags or attachments.
4. `hardDeleteDiary` and `purgeOldTrash` physically delete local attachment files from storage (`mediaDir`) before deleting database rows.
5. `calculateStreak` calculates consecutive journaling days safely across months and years with standard timezone calculation.

---

## 2. Security, AppLockManager & Biometrics Architecture

### 2.1 Hardware-Backed KeyStore Encryption: `PinCipher`
- **Location**: `app/src/main/java/com/example/inkpaperdiary/core/security/PinCipher.kt`
- **Algorithm**: `AES/GCM/NoPadding`, 128-bit authentication tag.
- **Key Store**: `AndroidKeyStore`, alias `"ink_paper_diary_pin_key"`.
- **Payload Format**: `"c1:<base64(IV)>:<base64(ciphertext)>"`.
- **Migration**: `SettingsRepository.verifyAppPin(input)` decrypts via `PinCipher.decrypt()`. If stored PIN was plaintext (legacy format), it validates plaintext directly and immediately upgrades stored PIN to AES-GCM ciphertext.

### 2.2 Biometrics Integration: `BiometricHelper`
- **Location**: `app/src/main/java/com/example/inkpaperdiary/core/security/BiometricHelper.kt`
- **Authenticators**: `BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL`.
- **API**: `isBiometricAvailable(context: Context): Boolean`, `showBiometricPrompt(activity: FragmentActivity, title, subtitle, onSuccess, onError)`.

### 2.3 App Lock State Machine: `AppLockManager`
- **Location**: `app/src/main/java/com/example/inkpaperdiary/core/security/AppLockManager.kt`
- **State**: `isLocked: StateFlow<Boolean>` (starts `true`).
- **External Picker Flag**: `var isPickerActive: Boolean = false` (`@Volatile`).
- **Lifecycle Protection Mechanism**:
  1. In `MainActivity.kt:84-96`:
     - `onStop()`: if `isLockEnabled && !AppLockManager.isPickerActive`, triggers `AppLockManager.lock()`.
     - `onResume()`: resets `AppLockManager.isPickerActive = false`.
  2. In `MainActivity.kt:54-67`:
     - Dynamic listener on `settingsRepository.appLockEnabled`: if active, sets `WindowManager.LayoutParams.FLAG_SECURE` to block screenshots and task switcher previews.
  3. External Intent Pickers:
     - Before launching `PickVisualMedia` (photo picker) or `OpenDocument` (backup/import), code **must set `AppLockManager.isPickerActive = true`**.
     - Current usages: `EditorScreen.kt:245`, `TimelineScreen.kt:92`, `SettingsScreen.kt:84, 435, 456`.
     - **UI Refactoring Warning**: Any redesigned photo or file picker launcher must retain `AppLockManager.isPickerActive = true` before launching.

### 2.4 Lock Gate in Navigation & LockScreen Contract
- **Gate**: In `AppNavigation.kt:72-80`, if `isLockEnabled && isAppLocked`, `LockScreen` is rendered; no inner screen is accessible.
- **Back Button**: In `LockScreen.kt:45-47`, pressing the system back button while locked invokes `activity?.moveTaskToBack(true)` to prevent back-navigation bypass.
- **Lockout Rule**: 5 consecutive failed PIN entries triggers a 30-second lockout cooldown (`LockViewModel.LOCKOUT_MILLIS = 30_000L`).

---

## 3. Cloud Synchronization & Backup/Import Pipelines

### 3.1 Network Layer: `SupabaseClient`
- **Location**: `app/src/main/java/com/example/inkpaperdiary/core/network/SupabaseClient.kt`
- **Transport**: `OkHttpClient` (timeouts: connect 15s, read 20s, write 20s).
- **Serialization**: `kotlinx.serialization.json.Json` (`ignoreUnknownKeys = true`, `isLenient = true`).
- **Endpoints**:
  - `GET {baseUrl}/rest/v1/` — ping / connection test (`testConnection()`).
  - `POST {baseUrl}/auth/v1/token?grant_type=password` — email/password sign-in.
  - `POST {baseUrl}/rest/v1/diaries` with header `Prefer: resolution=merge-duplicates` — bulk upsert diaries (`upsertDiaries`).
  - `GET {baseUrl}/rest/v1/diaries?updated_at=gt.{iso}&select=*` — fetch remote incremental updates (`fetchUpdatedDiaries`).
  - `POST {baseUrl}/storage/v1/object/diary-media/{remotePath}` with header `x-upsert: true` — image upload (`uploadMedia`).

### 3.2 Sync Engine: `SyncManager`
- **Location**: `app/src/main/java/com/example/inkpaperdiary/core/sync/SyncManager.kt`
- **Algorithm (Three-Phase Sync)**:
  1. **Phase 1: Local Media Upload**: Queries `diaryDao.getUnsyncedDiaries()`. Any attachment lacking `remoteUrl` but existing on disk is uploaded to Supabase Storage (`{userId}/{fileName}`) and updated locally with `syncStatus = 0`.
  2. **Phase 2: Local Mutation Push**: Formats unsynced entries into ISO-8601 UTC JSON payloads and upserts to Supabase `/rest/v1/diaries`. On success, updates local `syncStatus = 0`.
  3. **Phase 3: Remote Pull & Merge**: Pulls remote entries modified after `lastSyncTime`.
     - **Conflict Resolution Rule**: If local entity has `syncStatus == 2` (locally soft-deleted, delete pending push), local deletion takes priority. If remote is also deleted, status is set to 0. If local entity is null and remote is deleted, insertion is skipped to prevent resurrected ghost records. If remote `updatedAt > local.updatedAt`, local DB is updated with remote entity.
  4. Updates `lastSyncTimestamp` in `SettingsRepository`.

### 3.3 Background Sync: `SyncWorker`
- **Location**: `app/src/main/java/com/example/inkpaperdiary/core/sync/SyncWorker.kt`
- **Base Class**: `CoroutineWorker`
- **Work Name**: `"ink_paper_diary_sync_work"`
- **Interval**: 1 hour periodic work (`PeriodicWorkRequestBuilder<SyncWorker>(1, TimeUnit.HOURS)`).
- **Backoff Policy**: `BackoffPolicy.EXPONENTIAL`, 15 minutes.
- **Constraints**: Requires `NetworkType.CONNECTED` (or `NetworkType.UNMETERED` if wifi-only).
- **Triggers**: Scheduled on app launch in `MainActivity.kt:47` if `autoSyncEnabled`.

### 3.4 Data Export & Import: `BackupManager`
- **Location**: `app/src/main/java/com/example/inkpaperdiary/core/backup/BackupManager.kt`
- **JSON Backup**:
  - `exportToJson(context, diaries)`: exports full schema (id, contentMarkdown, mood, weather, location, tags, attachments metadata) to cache file `inkpaper_backup_{timestamp}.json`.
  - `importFromJson(jsonString)`: parses JSON, assigns fallback UUIDs if missing, reconstructs `Diary` and `Attachment` objects (marks localPath empty for re-download or re-association).
- **Markdown ZIP Archive**:
  - `exportToMarkdownZip(context, diaries)`: creates `inkpaper_markdown_{timestamp}.zip`.
  - Directory structure:
    - `diaries/{yyyy-MM-dd}_{safeTitle}_{shortId}.md`: YAML frontmatter (`id`, `title`, `date`, `mood`, `weather`, `location`, `tags`) + markdown content.
    - `images/{fileName}`: copies existing local image files from disk into ZIP.

### 3.5 Intelligent TXT Importer: `TxtDiaryImporter`
- **Location**: `app/src/main/java/com/example/inkpaperdiary/core/backup/TxtDiaryImporter.kt`
- **Features**:
  - Dual encoding fallback: UTF-8 (stripping BOM) -> `GB18030` / `GBK` -> standard UTF-8.
  - Multi-entry splitting: detects line headers matching `DATE_PATTERN` (`\d{4}[年/.-]\d{1,2}[月/.-]\d{1,2}日?`).
  - Fallback timestamp matrix: Header date -> filename date (`2023-08-15` or `20230815`) -> file `lastModified` -> `currentTimeMillis()`.
  - Natural language metadata extraction: extracts weather keywords (晴/雨/阴/雪/风/多云), mood keywords (开心/平静/充实/疲惫/难过/焦虑), time of day (`14:30`, `下午 2:30`), hashtags (`#旅行 #生活`), and strips title boilerplate.
  - Streaming API: `importTxtUris(context, uris, onSaveDiary)` batches URI processing with progress tracking.

---

## 4. ViewModels & StateFlow Contracts Catalog

The table below outlines all ViewModels in `com.example.inkpaperdiary`, their constructor dependencies, StateFlow contracts, and public API methods.

| ViewModel | Constructor Arguments | Primary StateFlow Contract | Actions / Events | Notes / Screen Usage |
| :--- | :--- | :--- | :--- | :--- |
| **`TimelineViewModel`** | `repository: DiaryRepository` | `uiState: StateFlow<TimelineUiState>`<br>• `diaries: List<Diary>`<br>• `filteredDiaries: List<Diary>`<br>• `selectedMoodFilter: Mood?`<br>• `onlyPinned: Boolean`<br>• `isLoading: Boolean` | • `setMoodFilter(Mood?)`<br>• `togglePinnedFilter()`<br>• `togglePin(Diary)`<br>• `deleteDiary(String)`<br>• `importTxtFiles(Context, List<Uri>, callback)` | Backs Primary Timeline (`Journal` Tab). Needs Segmented Filter integration in UI. |
| **`CalendarViewModel`** | `repository: DiaryRepository` | `uiState: StateFlow<CalendarUiState>`<br>• `selectedDate: Calendar`<br>• `currentMonth: Calendar`<br>• `monthDiaries: List<Diary>`<br>• `selectedDayDiaries: List<Diary>`<br>• `diaryDaysMap: Map<Int, List<Diary>>` | • `selectDate(dayOfMonth: Int)`<br>• `nextMonth()`<br>• `previousMonth()` | Backs `Calendar` Tab. Maps month entries by day-of-month. |
| **`OnThisDayViewModel`** | `repository: DiaryRepository` | `uiState: StateFlow<OnThisDayUiState>`<br>• `memories: List<HistoricalMemory>` (`yearsAgo: Int`, `diary: Diary`)<br>• `isLoading: Boolean` | • `loadMemories()` | Backs `Memories` Tab (Historical memories on same month & day). |
| **`EditorViewModel`** | `diaryRepository: DiaryRepository`<br>`mediaRepository: MediaRepository`<br>`diaryId: String?`<br>`initialEntryDate: Long?` | `uiState: StateFlow<EditorUiState>`<br>• `id: String`<br>• `userId: String`<br>• `title: String`<br>• `contentMarkdown: String`<br>• `mood: Mood`<br>• `weather: Weather`<br>• `locationName: String?`<br>• `entryDate: Long`<br>• `createdAt: Long`<br>• `tags: List<Tag>`<br>• `attachments: List<Attachment>`<br>• `isPinned: Boolean`<br>• `isSaving: Boolean`<br>• `isLoaded: Boolean` | • `updateTitle(String)`<br>• `updateContent(String)`<br>• `updateMood(Mood)`<br>• `updateWeather(Weather)`<br>• `updateLocation(String?)`<br>• `updateEntryDate(Long)`<br>• `togglePinned()`<br>• `addTag(String)`<br>• `removeTag(String)`<br>• `addImage(Uri)`<br>• `removeAttachment(String)`<br>• `saveDiary(onSaved: () -> Unit)` | Supports new or edit mode; preserves `createdAt`; compresses image via `MediaRepository`. |
| **`SettingsViewModel`** | `settingsRepository: SettingsRepository`<br>`diaryRepository: DiaryRepository`<br>`syncManager: SyncManager` | `uiState: StateFlow<SettingsUiState>`<br>• `supabaseUrl: String`<br>• `supabaseAnonKey: String`<br>• `appLockEnabled: Boolean`<br>• `appLockPin: String`<br>• `biometricEnabled: Boolean`<br>• `paperPattern: PaperPattern`<br>• `autoSyncEnabled: Boolean`<br>• `lastSyncTime: Long`<br>• `isSyncing: Boolean`<br>• `syncMessage: String?`<br>• `exportFile: File?`<br>`syncMessage: StateFlow<String?>` | • `saveSupabaseConfig(url, anonKey)`<br>• `testSupabaseConnection(url, anonKey, callback)`<br>• `performManualSync()`<br>• `setAutoSyncEnabled(Boolean)`<br>• `setAppLock(enabled, pin)`<br>• `setBiometric(Boolean)`<br>• `setPaperPattern(PaperPattern)`<br>• `exportMarkdownZip(Context, callback)`<br>• `exportJsonBackup(Context, callback)`<br>• `importJsonBackup(String, callback)`<br>• `importTxtFiles(Context, List<Uri>, callback)`<br>• `clearSyncMessage()` | Backs `Settings` Tab. All settings mutations must flow through these methods. |
| **`LockViewModel`** | `settingsRepository: SettingsRepository` | `uiState: StateFlow<LockUiState>`<br>• `isLockEnabled: Boolean`<br>• `biometricEnabled: Boolean`<br>• `isUnlocked: Boolean`<br>• `maxPinLength: Int`<br>`inputPin: StateFlow<String>`<br>`isUnlocked: StateFlow<Boolean>`<br>`lockoutRemaining: StateFlow<Long>` | • `appendPinDigit(String)`<br>• `deletePinDigit()`<br>• `unlockByBiometric()` | Enforces 4~6 digit PIN; 5 failed tries lock out for 30s. |
| **`SearchViewModel`** | `repository: DiaryRepository` | `uiState: StateFlow<SearchUiState>`<br>• `query: String`<br>• `results: List<Diary>`<br>• `selectedMood: Mood?`<br>• `selectedWeather: Weather?`<br>• `selectedTag: Tag?`<br>• `allTags: List<Tag>` | • `updateQuery(String)`<br>• `toggleMood(Mood)`<br>• `toggleWeather(Weather)`<br>• `toggleTag(Tag)` | Multi-dimensional filter (query + mood + weather + tag). |
| **`StatsViewModel`** | `repository: DiaryRepository` | `stats: StateFlow<Stats>`<br>• `totalDiaries: Int`<br>• `totalWords: Int`<br>• `streakDays: Int`<br>• `moodDistribution: Map<Mood, Int>`<br>• `weatherDistribution: Map<Weather, Int>`<br>• `topTags: List<Pair<String, Int>>` | None (read-only flow) | Backs Stats screen; word count and streak computed in repo. |
| **`TrashViewModel`** | `repository: DiaryRepository` | `trashDiaries: StateFlow<List<Diary>>` | • `restore(diaryId: String)`<br>• `permanentDelete(diaryId: String)`<br>• `emptyTrash()` | Soft delete recovery & hard delete with disk image removal. |

---

## 5. Build System, Dependencies & Verification Commands

### 5.1 Build Environment Specifications
- **Gradle Version**: `9.1.0` (Gradle Wrapper)
- **Android Gradle Plugin (AGP)**: `9.0.1`
- **Kotlin Version**: `2.3.20`
- **KSP Version**: `2.3.11`
- **Compose Compiler**: Kotlin 2.0+ official Compose compiler plugin (`org.jetbrains.kotlin.plugin.compose` version `2.3.20`)
- **Compose BOM**: `2026.03.01`
- **JDK / Toolchain**: Java 17 (`jvmToolchain(17)`, running on host JVM `21.0.5 LTS`)
- **Android SDK**: `compileSdk = 36`, `targetSdk = 36`, `minSdk = 26`
- **Configuration Cache**: Enabled (`org.gradle.configuration-cache=true`)
- **Build Cache**: Enabled (`org.gradle.caching=true`)
- **JVM Args**: `-Xmx2048m -Dfile.encoding=UTF-8`

### 5.2 Dependency Breakdown

```
Core Android & Lifecycle:
├── androidx.core:core-ktx:1.18.0
├── androidx.activity:activity-compose:1.13.0
├── androidx.lifecycle:lifecycle-runtime-ktx:2.10.0
├── androidx.lifecycle:lifecycle-runtime-compose:2.10.0
└── androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0

Compose UI (via Compose BOM 2026.03.01):
├── androidx.compose.ui:ui
├── androidx.compose.ui:ui-tooling-preview
├── androidx.compose.material3:material3
├── androidx.compose.material:material-icons-extended
└── androidx.compose.ui:ui-tooling (debug)

Navigation:
├── androidx.navigation3:navigation3-runtime:1.0.1
├── androidx.navigation3:navigation3-ui:1.0.1
└── androidx.lifecycle:lifecycle-viewmodel-navigation3:2.10.0

Persistence & Storage:
├── androidx.room:room-runtime:2.7.2
├── androidx.room:room-ktx:2.7.2
├── androidx.room:room-compiler:2.7.2 (ksp)
└── androidx.datastore:datastore-preferences:1.1.1

Security & Background:
├── androidx.biometric:biometric:1.2.0-alpha05
└── androidx.work:work-runtime-ktx:2.9.1

Networking, Media & Serialization:
├── io.coil-kt:coil-compose:2.7.0
├── org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3
└── com.squareup.okhttp3:okhttp:4.12.0

Unit & Local Testing:
├── junit:junit:4.13.2
└── org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2
```

### 5.3 Verification Commands & Results

1. **Unit Test Command**:
   ```bash
   ./gradlew test --stacktrace
   ```
   - **Result**: `BUILD SUCCESSFUL in 827ms`, 26 actionable tasks up-to-date.
   - **Tested Suites**:
     - `com.example.inkpaperdiary.BackupManagerTest`: JSON import/export round-trip test.
     - `com.example.inkpaperdiary.DiaryModelTest`: CJK/Latin word count calculation, markdown stripping, preview extraction, and enum code fallbacks.
     - `com.example.inkpaperdiary.TxtDiaryImporterTest`: multi-entry date header parsing, filename date extraction, compact 8-digit date extraction, GB18030 encoding fallback, and file lastModified fallback.

2. **Assemble Debug Compilation Command**:
   ```bash
   ./gradlew assembleDebug
   ```
   - **Result**: `BUILD SUCCESSFUL in 829ms`, 37 actionable tasks up-to-date.
   - **Output APK Target**: `app/build/outputs/apk/debug/app-debug.apk`.

---

## 6. Zero-Regression Boundaries for UI Refactoring (R4 Check-list)

To ensure the Apple HIG UI refactoring causes zero regressions in business logic:
1. **Never mutate Room Schema or DAOs**: `DiaryEntity`, `AttachmentEntity`, `TagEntity`, and `DiaryTagCrossRef` must not be altered.
2. **Preserve External Picker Security Flag**:
   - Any UI calling `rememberLauncherForActivityResult` for photo picker or file export/import MUST set `AppLockManager.isPickerActive = true` before launching.
3. **Preserve Navigation BackStack Contract**:
   - `AppDestination.Editor` expects `(diaryId: String?, entryDate: Long? = null)`.
   - `AppDestination.Calendar`, `OnThisDay`, `Search`, `Stats`, `Settings`, `Trash` must retain their destination routes and ViewModel bindings.
4. **Preserve ViewModel Public Method Signatures**:
   - `TimelineViewModel.setMoodFilter(Mood?)`, `togglePinnedFilter()`, `togglePin(Diary)`, `deleteDiary(id)`
   - `EditorViewModel.saveDiary(onSaved)`
   - `SettingsViewModel`: all Supabase config, biometric, app lock, and backup export/import methods.
5. **No Third-Party UI Libraries**: The build dependencies must not include external UI component libraries; all iOS HIG materials, glass borders, spring interactions, and squircle shapes must be implemented using existing Compose Foundation/UI primitives.
