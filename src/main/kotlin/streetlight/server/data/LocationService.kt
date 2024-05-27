package streetlight.server.data

import streetlight.model.Location
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

class LocationEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : EntityClass<Int, LocationEntity>(LocationService.LocationTable)

    var name by LocationService.LocationTable.name
    var latitude by LocationService.LocationTable.latitude
    var longitude by LocationService.LocationTable.longitude
    var area by AreaEntity referencedOn LocationService.LocationTable.area
}

class LocationService(private val database: Database) {
    object LocationTable : IntIdTable() {
        val name = text("name")
        val latitude = double("latitude")
        val longitude = double("longitude")
        val area = reference(
            name = "area_id",
            foreign = AreaService.AreaTable,
            onDelete = ReferenceOption.CASCADE
        ).uniqueIndex()
    }

    init {
        transaction(database) {
            SchemaUtils.create(LocationTable)
        }
    }

    suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }

    suspend fun create(location: Location): Int = dbQuery {
        val a = AreaEntity.findById(location.areaId) ?: return@dbQuery -1
        LocationEntity.new {
            name = location.name
            latitude = location.latitude
            longitude = location.longitude
            area = a
        }.id.value
    }

    suspend fun read(id: Int): Location? = dbQuery {
        LocationEntity.findById(id)
            ?.let {
                Location(
                    it.id.value,
                    it.name,
                    it.latitude,
                    it.longitude,
                    it.area.id.value
                )
            }
    }

    suspend fun readAll(): List<Location> = dbQuery {
        LocationEntity.all().map {
            Location(
                it.id.value,
                it.name,
                it.latitude,
                it.longitude,
                it.area.id.value
            )
        }
    }

    suspend fun update(id: Int, location: Location) = dbQuery {
        LocationEntity.findById(id)?.let {
            it.name = location.name
            it.latitude = location.latitude
            it.longitude = location.longitude
            AreaEntity.findById(location.areaId)?.let {
                    a -> it.area = a
            }
        }
    }

    suspend fun delete(id: Int) = dbQuery {
        LocationEntity.findById(id)?.delete()
    }
}

