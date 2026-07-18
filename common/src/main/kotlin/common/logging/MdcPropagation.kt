package common.logging

import java.util.concurrent.CompletableFuture
import org.slf4j.MDC

fun runAsyncWithMdc(block: () -> Unit): CompletableFuture<Void> {
    val capturedMdc = MDC.getCopyOfContextMap() ?: emptyMap()
    return CompletableFuture.runAsync {
        MDC.setContextMap(capturedMdc)
        try {
            block()
        } finally {
            MDC.clear()
        }
    }
}