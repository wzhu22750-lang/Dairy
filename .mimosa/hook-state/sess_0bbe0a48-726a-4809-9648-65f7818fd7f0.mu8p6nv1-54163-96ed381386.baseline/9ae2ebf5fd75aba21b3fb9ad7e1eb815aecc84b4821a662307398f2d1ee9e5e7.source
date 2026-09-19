package com.example.inkpaperdiary.core.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

class SupabaseClient(
    private val baseUrl: String,
    private val anonKey: String,
    private var userAccessToken: String? = null
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    val isConfigured: Boolean
        get() = baseUrl.isNotBlank() && anonKey.isNotBlank()

    fun setAccessToken(token: String?) {
        this.userAccessToken = token
    }

    private fun buildHeaders(): Headers {
        val builder = Headers.Builder()
            .add("apikey", anonKey)
        if (!userAccessToken.isNullOrBlank()) {
            builder.add("Authorization", "Bearer $userAccessToken")
        } else {
            builder.add("Authorization", "Bearer $anonKey")
        }
        return builder.build()
    }

    suspend fun testConnection(): Result<Boolean> = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext Result.failure(Exception("Supabase URL 或 Key 未配置"))
        try {
            val request = Request.Builder()
                .url("${cleanUrl()}/rest/v1/")
                .headers(buildHeaders())
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful || response.code == 404 || response.code == 400) {
                    Result.success(true)
                } else {
                    Result.failure(IOException("连接测试失败: HTTP ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signInWithPassword(email: String, password: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val payload = buildJsonObject {
                put("email", email)
                put("password", password)
            }
            val request = Request.Builder()
                .url("${cleanUrl()}/auth/v1/token?grant_type=password")
                .headers(buildHeaders())
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val root = json.parseToJsonElement(body).jsonObject
                    val token = root["access_token"]?.jsonPrimitive?.content ?: ""
                    this@SupabaseClient.userAccessToken = token
                    Result.success(token)
                } else {
                    Result.failure(IOException("登录失败: $body"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun upsertDiaries(diariesJson: JsonArray): Result<Unit> = withContext(Dispatchers.IO) {
        if (!isConfigured || diariesJson.isEmpty()) return@withContext Result.success(Unit)
        try {
            val request = Request.Builder()
                .url("${cleanUrl()}/rest/v1/diaries")
                .headers(
                    buildHeaders().newBuilder()
                        .add("Prefer", "resolution=merge-duplicates")
                        .build()
                )
                .post(diariesJson.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success(Unit)
                } else {
                    Result.failure(IOException("同步日记到云端失败: ${response.body?.string()}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchUpdatedDiaries(lastSyncTimeIso: String): Result<JsonArray> = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext Result.success(buildJsonArray {})
        try {
            val url = "${cleanUrl()}/rest/v1/diaries?updated_at=gt.$lastSyncTimeIso&select=*"
            val request = Request.Builder()
                .url(url)
                .headers(buildHeaders())
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: "[]"
                if (response.isSuccessful) {
                    val array = json.parseToJsonElement(body).jsonArray
                    Result.success(array)
                } else {
                    Result.failure(IOException("拉取云端更新失败: $body"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadMedia(file: File, remotePath: String): Result<String> = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext Result.failure(Exception("Supabase 未配置"))
        try {
            val requestBody = file.asRequestBody("image/webp".toMediaType())
            val request = Request.Builder()
                .url("${cleanUrl()}/storage/v1/object/diary-media/$remotePath")
                .headers(
                    buildHeaders().newBuilder()
                        .add("x-upsert", "true")
                        .build()
                )
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val publicUrl = "${cleanUrl()}/storage/v1/object/public/diary-media/$remotePath"
                    Result.success(publicUrl)
                } else {
                    Result.failure(IOException("图片上传失败: ${response.body?.string()}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun cleanUrl(): String {
        return baseUrl.trimEnd('/')
    }
}
