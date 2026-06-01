package com.example.data

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private const val PRIMARY_MODEL = "gemini-3.5-flash"
    private const val FALLBACK_MODEL = "gemini-2.5-flash"

    @Volatile
    var customModelOverride: String? = null

    fun getActiveModelName(): String {
        return customModelOverride ?: PRIMARY_MODEL
    }

    suspend fun generateGuideResponse(prompt: String, contextText: String, customSystemInstruction: String? = null): String {
        DiagnosticsTracker.logInfo("GeminiService", "Initiating AI request. Prompt size: ${prompt.length} bytes")
        
        val activeModel = getActiveModelName()
        var response = makeApiCall(activeModel, prompt, contextText, customSystemInstruction)
        
        // Handle fallback if the primary model failed with any API/Server error
        if (response.startsWith("ERROR_GEMINI_API_FAIL") && activeModel == PRIMARY_MODEL) {
            val originalError = response.removePrefix("ERROR_GEMINI_API_FAIL:")
            DiagnosticsTracker.logWarning("GeminiService", "Primary model ($PRIMARY_MODEL) failed: $originalError. Triggering automatic hot-fallback to $FALLBACK_MODEL.")
            
            response = makeApiCall(FALLBACK_MODEL, prompt, contextText, customSystemInstruction)
            
            if (response.startsWith("ERROR_GEMINI_API_FAIL")) {
                val fallbackError = response.removePrefix("ERROR_GEMINI_API_FAIL:")
                DiagnosticsTracker.logError("GeminiService", "Fallback model ($FALLBACK_MODEL) also failed: $fallbackError")
                return "Не удалось связаться с ИИ-Проводником.\n\nОшибка первичной модели ($PRIMARY_MODEL): $originalError\n\nОшибка резервной модели ($FALLBACK_MODEL): $fallbackError\n\nПожалуйста, скопируйте Диагностический Отчет и предоставьте его в службу поддержки."
            } else {
                DiagnosticsTracker.logInfo("GeminiService", "Cascade fallback to $FALLBACK_MODEL completed successfully!")
                return "[Резервный ИИ-канал $FALLBACK_MODEL] $response"
            }
        } else if (response.startsWith("ERROR_GEMINI_API_FAIL")) {
            val errStr = response.removePrefix("ERROR_GEMINI_API_FAIL:")
            return "Не удалось связаться с верховным разумом. Код ошибки Gemini API: $errStr. Проверьте ваш API-ключ в настройках."
        }
        
        return response
    }

    private suspend fun makeApiCall(modelName: String, prompt: String, contextText: String, customSystemInstruction: String? = null): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            DiagnosticsTracker.logWarning("GeminiService", "Gemini API key is unconfigured or is a default placeholder string.")
            return@withContext "Приветствую! На связи ИИ-Гильд Проводник. Обратите внимание, что API-ключ 'GEMINI_API_KEY' не настроен или содержит шаблонную заглушку. Пожалуйста, укажите рабочий ключ в панели Секретов приложения, чтобы общаться со мной.\n\nТем не менее, вы можете продолжить использовать классические CLI команды. Введите 'help' для полного списка команд."
        }

        val systemInstruction = customSystemInstruction ?: """
            Вы — ИИ-Гильдейский Проводник (AI Agent Guide) децентрализованной социальной операционной системы 'Открытый Орден' (Open Order Social OS).
            Вы выступаете в роли универсального ИИ-помощника, интегрированного прямо в CLI чат терминала.
            
            Ваши возможности и правила поведения:
            1. Вы квалифицированно объясняете ЛЮБЫЕ вопросы и темы (наука, программирование, история, философия, бытовые вопросы, устройство вселенной, написание кода и т.д.). Даете подробные, подробнейшие, понятные и содержательные ответы.
            2. Вы детально объясняете принципы функционирования «Открытого Ордена» (бирюзовое управление, совещательный совет, вето, Суд Чести, децентрализованный репутационный кворум, P2P-синхронизация по почтовому протоколу SMTP/IMAP).
            3. ВАЖНОЕ ПРАВИЛО: Вы НЕ выполняете и не симулируете исполнение CLI-команд терминала. Вы не можете самостоятельно добавлять участников, создавать предложения, выносить вердикты суда или менять базу данных. 
            Если пользователь просит вас зарегистрировать кого-то, проголосовать или совершить иное системное действие, вежливо и благородно объясните, какую именно CLI-команду ему нужно ввести в этот терминал САМОСТОЯТЕЛЬНО.
            Например, покажите синтаксис команды:
            • status — проверить состояние локального узла
            • help — вывести список доступных инструкций
            • register <id> | <имя> | <роль> — зарегистрировать нового соратника
            • proposal <тема> | <описание> — выдвинуть предложение в совет
            • dispute <defendant> | <разбор> | <статья> — запустить дело в Суде Чести
            • msg <recipient> | <текст> — отправить зашифрованное письмо
            • sync — запустить ручную синхронизацию данных
            
            4. ОСОБОЕ ПРАВИЛО ДЛЯ ОТПРАВКИ СООБЩЕНИЙ СОРАТНИКАМ: Если пользователь ПРОСИТ вас отправить сообщение конкретному участнику (например, "отправь сообщение leonid@orden.p2p Привет!", "напиши Ивану, пусть зайдет в чат", "ИИ, отправь сообщение Леону: мы готовы"), вы должны подтвердить это в своем ответе в вежливой рыцарской манере, а затем НАПИСАТЬ в самом конце ответа специальный блок триггера авто-отправки РОВНО в следующем формате:
            ##SEND_MESSAGE_TRIGGER_START##
            получатель_id_или_email|текст_сообщения
            ##SEND_MESSAGE_TRIGGER_END##
            Где получатель_id_или_email — это ID (например, alex@orden.p2p) или email соратника из предоставленного списка участников (сравнивайте по имени или ID), а текст_сообщения — содержание письма. 
            Обязательно добавляйте этот блок триггера в самый конец вашего ответа, чтобы узел системы мог распознать его и автоматически совершить отправку!
            
            Стиль общения: благородный, рыцарский, уважительный (обращение «соратник», «пир»), но в то же время современный, точный и технологичный. Отвечайте всегда на русском языке.
            
            Текущий контекст приложения и состояние базы данных:
            $contextText
        """.trimIndent()

        val jsonRequest = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", prompt) })
                    })
                })
            })
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply { put("text", systemInstruction) })
                })
            })
        }

        val requestBody = jsonRequest.toString().toRequestBody("application/json".toMediaType())
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val respString = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    DiagnosticsTracker.logError("GeminiService", "REST endpoint returned error code ${response.code} for model $modelName")
                    return@withContext "ERROR_GEMINI_API_FAIL:${response.code} $respString"
                }
                
                val jsonResponse = JSONObject(respString)
                val candidates = jsonResponse.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val candidate = candidates.getJSONObject(0)
                    val content = candidate.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        return@withContext parts.getJSONObject(0).optString("text", "Empty text response")
                    }
                }
                return@withContext "Рейнджеры сети вернули пустой ответ от ИИ."
            }
        } catch (e: Exception) {
            DiagnosticsTracker.logError("GeminiService", "HTTP exception during client.execute() for model $modelName", e)
            return@withContext "ERROR_GEMINI_API_FAIL:Exception - ${e.localizedMessage}"
        }
    }
}
