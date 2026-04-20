package bff.config

import common.security.HmacUtils
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse

class HmacSigningInterceptor(private val secret: String) : ClientHttpRequestInterceptor {

    override fun intercept(
        request: HttpRequest,
        body: ByteArray,
        execution: ClientHttpRequestExecution
    ): ClientHttpResponse {
        val ts = System.currentTimeMillis()
        val sig = HmacUtils.sign(secret, request.method.name(), request.uri.path, ts, body)
        request.headers["X-Bff-Timestamp"] = ts.toString()
        request.headers["X-Bff-Signature"] = sig
        return execution.execute(request, body)
    }
}
