package com.example.inkpaperdiary.core.backup

import android.content.Context
import com.example.inkpaperdiary.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object BackupManager {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val dateFullFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    suspend fun exportToJson(context: Context, diaries: List<Diary>): Result<File> = withContext(Dispatchers.IO) {
        try {
            val exportDir = File(context.cacheDir, "exports").apply { if (!exists()) mkdirs() }
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val exportFile = File(exportDir, "inkpaper_backup_$timeStamp.json")

            val jsonArray = buildJsonArray {
                for (d in diaries) {
                    add(buildJsonObject {
                        put("id", d.id)
                        put("userId", d.userId)
                        put("title", d.title)
                        put("contentMarkdown", d.contentMarkdown)
                        put("mood", d.mood.code)
                        put("weather", d.weather.code)
                        put("locationName", d.locationName)
                        d.latitude?.let { put("latitude", it) }
                        d.longitude?.let { put("longitude", it) }
                        put("entryDate", d.entryDate)
                        put("createdAt", d.createdAt)
                        put("updatedAt", d.updatedAt)
                        put("isPinned", d.isPinned)
                        put("attachments", buildJsonArray {
                            d.attachments.forEach { att ->
                                add(buildJsonObject {
                                    put("id", att.id)
                                    put("fileName", att.fileName)
                                    put("fileSize", att.fileSize)
                                    put("mimeType", att.mimeType)
                                    put("sortOrder", att.sortOrder)
                                    att.remoteUrl?.let { put("remoteUrl", it) }
                                })
                            }
                        })
                        put("tags", buildJsonArray {
                            d.tags.forEach { tag ->
                                add(buildJsonObject {
                                    put("id", tag.id)
                                    put("name", tag.name)
                                    put("colorHex", tag.colorHex)
                                })
                            }
                        })
                    })
                }
            }

            exportFile.writeText(json.encodeToString(JsonArray.serializer(), jsonArray))
            Result.success(exportFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun importFromJson(jsonString: String): Result<List<Diary>> = withContext(Dispatchers.IO) {
        try {
            val array = json.parseToJsonElement(jsonString).jsonArray
            val list = mutableListOf<Diary>()
            for (element in array) {
                val obj = element.jsonObject
                val tags = obj["tags"]?.jsonArray?.map {
                    val tObj = it.jsonObject
                    Tag(
                        id = tObj["id"]?.jsonPrimitive?.content ?: UUID.randomUUID().toString(),
                        name = tObj["name"]?.jsonPrimitive?.content ?: "",
                        colorHex = tObj["colorHex"]?.jsonPrimitive?.content ?: "#9E3323"
                    )
                } ?: emptyList()

                val diary = Diary(
                    id = obj["id"]?.jsonPrimitive?.content ?: UUID.randomUUID().toString(),
                    userId = obj["userId"]?.jsonPrimitive?.content ?: "local_guest",
                    title = obj["title"]?.jsonPrimitive?.content ?: "",
                    contentMarkdown = obj["contentMarkdown"]?.jsonPrimitive?.content ?: "",
                    mood = Mood.fromCode(obj["mood"]?.jsonPrimitive?.content),
                    weather = Weather.fromCode(obj["weather"]?.jsonPrimitive?.content),
                    locationName = obj["locationName"]?.jsonPrimitive?.contentOrNull,
                    latitude = obj["latitude"]?.jsonPrimitive?.doubleOrNull,
                    longitude = obj["longitude"]?.jsonPrimitive?.doubleOrNull,
                    entryDate = obj["entryDate"]?.jsonPrimitive?.longOrNull ?: System.currentTimeMillis(),
                    createdAt = obj["createdAt"]?.jsonPrimitive?.longOrNull ?: System.currentTimeMillis(),
                    updatedAt = obj["updatedAt"]?.jsonPrimitive?.longOrNull ?: System.currentTimeMillis(),
                    isPinned = obj["isPinned"]?.jsonPrimitive?.booleanOrNull ?: false,
                    tags = tags,
                    attachments = obj["attachments"]?.jsonArray?.map { attEl ->
                        val attObj = attEl.jsonObject
                        Attachment(
                            id = attObj["id"]?.jsonPrimitive?.content ?: UUID.randomUUID().toString(),
                            diaryId = obj["id"]?.jsonPrimitive?.content ?: "",
                            localPath = "", // 本地文件无法跨设备迁移，导入后通过远程链接/重新上传恢复
                            remoteUrl = attObj["remoteUrl"]?.jsonPrimitive?.contentOrNull,
                            fileName = attObj["fileName"]?.jsonPrimitive?.content ?: "",
                            fileSize = attObj["fileSize"]?.jsonPrimitive?.longOrNull ?: 0L,
                            mimeType = attObj["mimeType"]?.jsonPrimitive?.content ?: "image/webp",
                            sortOrder = attObj["sortOrder"]?.jsonPrimitive?.intOrNull ?: 0
                        )
                    } ?: emptyList()
                )
                list.add(diary)
            }
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun exportToMarkdownZip(context: Context, diaries: List<Diary>): Result<File> = withContext(Dispatchers.IO) {
        try {
            val exportDir = File(context.cacheDir, "exports").apply { if (!exists()) mkdirs() }
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val zipFile = File(exportDir, "inkpaper_markdown_$timeStamp.zip")

            ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
                for (d in diaries) {
                    val safeTitle = (if (d.title.isNotBlank()) d.title else "无标题")
                        .replace(Regex("[/\\\\:*?\"<>|]"), "_")
                    val datePrefix = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(d.entryDate))
                    val fileName = "diaries/${datePrefix}_${safeTitle}_${d.id.take(6)}.md"

                    val tagsStr = d.tags.joinToString(", ") { "\"${it.name}\"" }
                    val frontMatter = """
                        ---
                        id: "${d.id}"
                        title: "${d.title.replace("\"", "\\\"")}"
                        date: "${dateFullFormatter.format(Date(d.entryDate))}"
                        mood: "${d.mood.displayName}"
                        weather: "${d.weather.displayName}"
                        location: "${d.locationName ?: ""}"
                        tags: [$tagsStr]
                        ---
                        
                    """.trimIndent()

                    val fullContent = frontMatter + "\n" + d.contentMarkdown

                    zos.putNextEntry(ZipEntry(fileName))
                    zos.write(fullContent.toByteArray(Charsets.UTF_8))
                    zos.closeEntry()

                    // 将本地图片附件打包进 images/ 目录
                    for (att in d.attachments) {
                        val file = File(att.localPath)
                        if (file.exists()) {
                            zos.putNextEntry(ZipEntry("images/${att.fileName}"))
                            file.inputStream().use { input ->
                                input.copyTo(zos)
                            }
                            zos.closeEntry()
                        }
                    }
                }
            }

            Result.success(zipFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
