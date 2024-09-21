package streetlight.server.db.routes

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import streetlight.model.Endpoint
import streetlight.model.core.Location
import streetlight.server.db.services.LocationService
import streetlight.server.extensions.getIdOrThrow
import streetlight.server.extensions.getUsername
import streetlight.server.extensions.okData
import streetlight.server.plugins.Log
import streetlight.server.plugins.authenticateJwt

fun Routing.atlasRouting(endpoint: Endpoint, service: LocationService) {
    authenticateJwt {
        get(endpoint.path) {
            Log.logDebug("Routing: GET ${endpoint.path}")
            val username = call.getUsername()
            val locations = service.getUserLocations(username)
            call.okData(locations)
        }

        get(endpoint.serverIdTemplate) {
            Log.logDebug("Routing: GET ${endpoint.serverIdTemplate}")
            val username = call.getUsername()
            val id = call.getIdOrThrow()
            val location = service.getUserLocation(username, id)
            call.okData(location)
        }

        put(endpoint.path) {
            Log.logDebug("Routing: PUT ${endpoint.path}")
            val username = call.getUsername()
            var location = call.receive<Location>()
            location = service.updateUserLocation(username, location)
            call.okData(location)
        }

        post(endpoint.path) {
            Log.logDebug("Routing: POST ${endpoint.path}")
            val username = call.getUsername()
            var location = call.receive<Location>()
            location = service.createUserLocation(username, location)
            call.okData(location)
        }

        delete(endpoint.serverIdTemplate) {
            Log.logDebug("Routing: DELETE ${endpoint.path}")
            val username = call.getUsername()
            val locationId = call.getIdOrThrow()
            service.deleteUserLocation(username, locationId)
            call.okData(true)
        }
    }
}