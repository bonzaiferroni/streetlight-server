package streetlight.server.utils

import kampfire.model.GeoPoint
import net.iakovlev.timeshape.TimeZoneEngine

object TimeZones {
    private val engine: TimeZoneEngine by lazy { TimeZoneEngine.initialize() }

    /** The id of the time zone at [geoPoint], or `null` at sea. */
    fun zoneIdAt(geoPoint: GeoPoint): String? = engine.query(geoPoint.lat, geoPoint.lng).orElse(null)?.id
}