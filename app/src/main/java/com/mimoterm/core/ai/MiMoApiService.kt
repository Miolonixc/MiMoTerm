package com.mimoterm.core.ai

import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Streaming

data class ChatMessage(
    val role: String,
    val content: String
)

data class ChatRequest(
    val model: String = "mimo",
    val messages: List<ChatMessage>,
    val stream: Boolean = true,
    val temperature: Float = 0.7f
)

data class ChatResponse(
    val id: String?,
    val choices: List<Choice>?
)

data class Choice(
    val index: Int,
    val delta: Delta?,
    val finish_reason: String?
)

data class Delta(
    val role: String?,
    val content: String?
)

interface MiMoApiService {
    @POST("v1/chat/completions")
    @Streaming
    suspend fun chatStream(
        @Header("Authorization") auth: String,
        @Body request: ChatRequest
    ): ResponseBody
}
