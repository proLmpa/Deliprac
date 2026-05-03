package common.security

import jakarta.servlet.FilterChain
import jakarta.servlet.ReadListener
import jakarta.servlet.ServletInputStream
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletRequestWrapper
import jakarta.servlet.http.HttpServletResponse
import org.springframework.web.filter.OncePerRequestFilter
import kotlin.math.abs

class HmacRequestFilter(
    private val secret: String,
    private val windowMs: Long = 30_000L
) : OncePerRequestFilter() {

    override fun doFilterInternal(req: HttpServletRequest, res: HttpServletResponse, chain: FilterChain) {
        val timestampHeader = req.getHeader(HmacUtils.TIMESTAMP_HEADER)
        val signatureHeader = req.getHeader(HmacUtils.SIGNATURE_HEADER)

        if (timestampHeader == null || signatureHeader == null) {
            res.reject()
            return
        }

        val timestampMs = timestampHeader.toLongOrNull()
        if (timestampMs == null) {
            res.reject()
            return
        }

        if (abs(System.currentTimeMillis() - timestampMs) > windowMs) {
            res.reject()
            return
        }

        val cached = CachedBodyHttpServletRequest(req)
        val body = cached.cachedBody

        val valid = HmacUtils.verify(
            secret = secret,
            method = req.method,
            path = req.requestURI,
            timestampMs = timestampMs,
            body = body,
            signature = signatureHeader
        )

        if (!valid) {
            res.reject()
            return
        }

        chain.doFilter(cached, res)
    }

    private fun HttpServletResponse.reject() {
        status = HttpServletResponse.SC_UNAUTHORIZED
        contentType = "text/plain"
        writer.write("Unauthorized")
    }

    private class CachedBodyHttpServletRequest(request: HttpServletRequest) : HttpServletRequestWrapper(request) {
        val cachedBody: ByteArray = request.inputStream.readBytes()

        override fun getInputStream(): ServletInputStream = object : ServletInputStream() {
            private val stream = cachedBody.inputStream()
            override fun read(): Int = stream.read()
            override fun isFinished(): Boolean = stream.available() == 0
            override fun isReady(): Boolean = true
            override fun setReadListener(listener: ReadListener) = Unit
        }
    }
}
