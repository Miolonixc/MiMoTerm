package com.mimoterm.core.ai

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MiMoRepository @Inject constructor(
    private val client: OkHttpClient,
    private val gson: Gson
) {
    private var apiKey: String = ""
    private var baseUrl: String = "https://api.mimo.xiaomi.com"

    fun configure(apiKey: String, baseUrl: String? = null) {
        this.apiKey = apiKey
        baseUrl?.let { this.baseUrl = it }
    }

    fun chatStream(messages: List<ChatMessage>): Flow<String> = flow {
        val request = ChatRequest(messages = messages)
        val httpRequest = Request.Builder()
            .url("$baseUrl/v1/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(gson.toJson(request).toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        val response = client.newCall(httpRequest).execute()
        if (!response.isSuccessful) {
            throw Exception("API error: ${response.code}")
        }

        val reader = BufferedReader(InputStreamReader(response.body!!.byteStream()))
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            val l = line ?: continue
            if (l.startsWith("data: ")) {
                val data = l.removePrefix("data: ").trim()
                if (data == "[DONE]") break
                try {
                    val chunk = gson.fromJson(data, ChatResponse::class.java)
                    val content = chunk.choices?.firstOrNull()?.delta?.content
                    if (content != null) {
                        emit(content)
                    }
                } catch (e: Exception) {
                    // Skip malformed chunks
                }
            }
        }
        reader.close()
        response.close()
    }.flowOn(Dispatchers.IO)
}
