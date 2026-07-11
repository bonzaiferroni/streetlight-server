package streetlight.server.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.metrics.micrometer.MicrometerMetrics
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import kotlinx.coroutines.launch
import streetlight.server.model.ServerScope
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration

fun Application.configureMetrics(server: ServerScope) {
    val registry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    val daemon = SiteStatusDaemon(server.dao, registry)
    install(MicrometerMetrics) {
        this.registry = registry
        distributionStatisticConfig = DistributionStatisticConfig.Builder()
            .expiry(1.minutes.toJavaDuration())
            .bufferLength(1)
            .build()
    }

    launch {
        daemon.start()
    }
}

