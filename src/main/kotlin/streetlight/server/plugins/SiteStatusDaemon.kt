package streetlight.server.plugins

import io.github.oshai.kotlinlogging.KotlinLogging
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.delay
import streetlight.model.data.MetricResolution
import streetlight.model.data.MetricType
import streetlight.model.data.SiteMetric
import streetlight.model.data.StatusPoint
import streetlight.model.data.SiteStatusId
import streetlight.server.model.DaoFacade
import java.util.concurrent.TimeUnit
import kotlin.time.Clock
import kotlin.time.Instant

private val log = KotlinLogging.logger(SiteStatusDaemon::class.simpleName!!)

class SiteStatusDaemon(
    private val dao: DaoFacade,
    private val registry: MeterRegistry
) {
    private val previousCounters = mutableMapOf<SiteMetric, Long>()
    private val previousSnapshots = mutableMapOf<SiteMetric, TimerSnapshot>()

    suspend fun start() {
        val finest = MetricResolution.entries.first()
        var nextTick = Clock.System.now().nextAlignedBoundary(finest)

        while (true) {
            delay(nextTick - Clock.System.now())

            for (resolution in MetricResolution.entries) {
                if (!nextTick.isAlignedTo(resolution)) continue

                val periodStart = nextTick - resolution.duration

                try {
                    if (resolution.predecessor == null) {
                        sampleFromMicrometer(periodStart, nextTick, resolution)
                    } else {
                        aggregateFromPredecessor(periodStart, nextTick, resolution)
                    }
                } catch (e: Exception) {
                    log.error(e) { "Failed to record ${resolution.name} at $nextTick" }
                }
            }

            nextTick += finest.duration
        }
    }

    private suspend fun sampleFromMicrometer(
        periodStart: Instant,
        periodEnd: Instant,
        resolution: MetricResolution
    ) {
        val integers = mutableMapOf<SiteMetric, Int>()
        val doubles = mutableMapOf<SiteMetric, Double>()

        // log.info { "Meter count: ${registry.meters.size}" }
        // log.info { registry.meters.joinToString("\n") { "${it.id.name} ${it.id.tags}" } }

        for (metric in SiteMetric.entries) {
            when (metric.metricType) {
                MetricType.Count -> {
                    val timers = registry.find(metric.meterName).timers()
                    val current = timers.sumOf { it.count() }
                    val previous = previousCounters.put(metric, current)
                        ?: return
                    val delta = if (current >= previous) current - previous else current
                    integers[metric] = delta.toInt()
                }
                MetricType.Average -> {
                    val timers = registry.find(metric.meterName).timers()
                    val current = TimerSnapshot(
                        count = timers.sumOf { it.count() },
                        totalTimeMs = timers.sumOf { it.totalTime(TimeUnit.MILLISECONDS) }
                    )
                    val previous = previousSnapshots.put(metric, current)
                        ?: return
                    val deltaCount = current.count - previous.count
                    val deltaTime = current.totalTimeMs - previous.totalTimeMs
                    doubles[metric] = if (deltaCount > 0) deltaTime / deltaCount else 0.0
                }
                MetricType.Max -> {
                    val timers = registry.find(metric.meterName).timers()
                    doubles[metric] = timers.maxOfOrNull { it.max(TimeUnit.MILLISECONDS) } ?: 0.0
                }
            }
        }

        dao.siteStatus.create(
            StatusPoint(
                siteStatusId = SiteStatusId.Empty,
                integers = integers,
                doubles = doubles,
                resolution = resolution,
                coverage = 1.0f,
                startedAt = periodStart,
                endedAt = periodEnd,
                createdAt = Clock.System.now()
            )
        )
    }

    private suspend fun aggregateFromPredecessor(
        periodStart: Instant,
        periodEnd: Instant,
        resolution: MetricResolution
    ) {
        val predecessor = resolution.predecessor!!
        val records = dao.siteStatus.readByResolutionAndPeriod(
            predecessor, periodStart, periodEnd
        )

        if (records.isEmpty()) return

        val expectedCount = (resolution.duration / predecessor.duration).toInt()
        val coverage = records.size.toFloat() / expectedCount.toFloat()

        val integers = mutableMapOf<SiteMetric, Int>()
        val doubles = mutableMapOf<SiteMetric, Double>()

        for (metric in SiteMetric.entries) {
            when (metric.metricType) {
                MetricType.Count -> {
                    integers[metric] = records.mapNotNull { it.integers[metric] }.sum()
                }
                MetricType.Average -> {
                    val weighted = records.mapNotNull { record ->
                        val value = record.doubles[metric] ?: return@mapNotNull null
                        val weight = record.integers[SiteMetric.RequestCount]
                            ?: return@mapNotNull null
                        value to weight
                    }
                    val totalWeight = weighted.sumOf { it.second }
                    if (totalWeight > 0) {
                        doubles[metric] = weighted.sumOf { it.first * it.second } / totalWeight
                    }
                }
                MetricType.Max -> {
                    records.mapNotNull { it.doubles[metric] }.maxOrNull()?.let {
                        doubles[metric] = it
                    }
                }
            }
        }

        dao.siteStatus.create(
            StatusPoint(
                siteStatusId = SiteStatusId.Empty,
                integers = integers,
                doubles = doubles,
                resolution = resolution,
                coverage = coverage,
                startedAt = periodStart,
                endedAt = periodEnd,
                createdAt = Clock.System.now()
            )
        )
    }

    private fun Instant.nextAlignedBoundary(resolution: MetricResolution): Instant {
        val seconds = resolution.duration.inWholeSeconds
        val aligned = epochSeconds - (epochSeconds % seconds) + seconds
        return Instant.fromEpochSeconds(aligned)
    }

    private fun Instant.isAlignedTo(resolution: MetricResolution): Boolean {
        return epochSeconds % resolution.duration.inWholeSeconds == 0L
    }
}

private data class TimerSnapshot(val count: Long, val totalTimeMs: Double)
