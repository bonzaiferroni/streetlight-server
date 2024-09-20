package streetlight.server.db.services

import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.lowerCase
import streetlight.model.core.Location
import streetlight.server.db.DataService
import streetlight.server.db.tables.LocationEntity
import streetlight.server.db.tables.LocationTable
import streetlight.server.db.tables.fromData
import streetlight.server.db.tables.toData

class LocationService : DataService<Location, LocationEntity>(
    LocationEntity,
    LocationEntity::fromData,
    LocationEntity::toData,
) {

    override fun getSearchOp(search: String): Op<Boolean> =
        Op.build { LocationTable.name.lowerCase() like "${search.lowercase()}%" }
}

