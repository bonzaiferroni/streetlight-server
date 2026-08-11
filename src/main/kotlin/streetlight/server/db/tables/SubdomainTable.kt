package streetlight.server.db.tables

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.datetime.timestamp

object SubdomainTable: UuidTable("subdomain") {
    val slug = text("slug").uniqueIndex()
    val locationId = reference("location_id", LocationTable, ReferenceOption.CASCADE).nullable().uniqueIndex()
    val updatedAt = timestamp("updated_at")
    val createdAt = timestamp("created_at")
}