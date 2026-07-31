package streetlight.server.routes

import kabinet.console.globalConsole
import streetlight.agent.KoogParserClient
import streetlight.model.data.ColdParse
import streetlight.model.data.Location
import streetlight.model.external.OSMQuery
import streetlight.model.external.toGeoPoint
import streetlight.server.external.OSMMapClient
import streetlight.server.model.DaoFacade

private val console = globalConsole.getHandle(ColdReader::class)

class ColdReader(
    private val parser: KoogParserClient,
    private val dao: DaoFacade,
) {
    private val osmClient by lazy { OSMMapClient() }

//    suspend fun serve(request: ParseRequest): MultiEventParseResult? {
//        val url = request.url.takeIf { url -> url.isNotEmpty() } ?: return null
//        val parse: ColdParse = if (request.isImage) {
//            agent.readImage(url, ReaderText.coldInstructions)
//        } else {
//            agent.readHtml(url, ReaderText.coldInstructions)
//        } ?: return null
//        val parsedEvents = parse.events
//
//        if (parsedEvents.isNullOrEmpty()) return null
//
//        val location = readLocationFromParse(parse, app)
//        val events = parsedEvents.mapNotNull { parsedEvent ->
//            val title = parsedEvent.name
//            val date = parsedEvent.date
//            val locationId = location?.locationId
//            val startsAt = parsedEvent.startsAt
//            val event = if (title != null && date != null)
//                dao.readEventAt(title, date)
//            else if (locationId != null && startsAt != null)
//                dao.readEventAt(locationId, startsAt)
//            else null
//            event?.toEdit() ?: parsedEvent.toEventEdit(url, null, null)
//        }
//
//        return MultiEventParseResult(parse.hasContent, location = location, events = events)
//    }

    private suspend fun readLocationFromParse(parse: ColdParse): Location? {
        val parse = parse.location ?: return null
        val name = parse.name
        var address = parse.address
        if (name == null && address == null) {
            console.log("name and address of location parse null")
            return null
        }

        var location = dao.location.readLocationAt(name, address)
        if (location != null) {
            console.log("found db location from parse")
            return location
        }

        val place = osmClient.search(OSMQuery(
            amenity = parse.name,
            street = parse.address,
            city = parse.city
        ))?.firstOrNull() ?: return null
        location = dao.location.readLocationAt(place.toGeoPoint())
        if (location != null) {
            console.log("found db location from OSM geoPoint")
            return location
        }

        address = place.address.let { "${it.number} ${it.road}" }
        location = dao.location.readLocationAt(place.name, address)
        if (location != null) {
            console.log("found db location from OSM name/address")
            return location
        }

        return null
    }
}
