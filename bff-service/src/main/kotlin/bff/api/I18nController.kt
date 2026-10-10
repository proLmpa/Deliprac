package bff.api

import org.springframework.context.MessageSource
import org.springframework.context.NoSuchMessageException
import org.springframework.core.io.ClassPathResource
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.util.Locale

@RestController
class I18nController(private val messageSource: MessageSource) {

    private val keys: List<String> by lazy {
        ClassPathResource("messages.properties")
            .inputStream
            .bufferedReader()
            .readLines()
            .filter { it.isNotBlank() && !it.startsWith('#') }
            .mapNotNull { it.substringBefore('=').trim().takeIf(String::isNotEmpty) }
    }

    @PostMapping("/api/i18n/messages")
    fun getMessage(@RequestBody body: Map<String, String>): Map<String, String> {
        val locale = if (body["lang"] == "ko") Locale.KOREAN else Locale.ENGLISH
        return keys.associateWith { key ->
            try { messageSource.getMessage(key, null, locale) }
            catch (_: NoSuchMessageException) { key }
        }

    }
}