package bff.config

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.github.resilience4j.micrometer.tagged.TaggedCircuitBreakerMetrics
import io.micrometer.core.instrument.MeterRegistry
import jakarta.annotation.PostConstruct
import org.springframework.context.annotation.Configuration

@Configuration
class MetricsConfig(
    private val circuitBreakerRegistry: CircuitBreakerRegistry,
    private val meterRegistry: MeterRegistry
) {
    @PostConstruct
    fun bindCircuitBreakerMetrics() {
        TaggedCircuitBreakerMetrics
            .ofCircuitBreakerRegistry(circuitBreakerRegistry)
            .bindTo(meterRegistry)
    }
}
