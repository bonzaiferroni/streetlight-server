package streetlight.server.db.tables

import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.jdbc.select

val LocationPostQuery get() = PostTable
    .join(LocationTable, JoinType.LEFT, PostTable.locationId, LocationTable.id)
    .join(StarTable, JoinType.LEFT, PostTable.starId, StarTable.id)
    .select(GeneralPostColumns)

