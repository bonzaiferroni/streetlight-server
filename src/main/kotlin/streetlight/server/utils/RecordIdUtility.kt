package streetlight.server.utils

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import streetlight.model.data.RecordId
import streetlight.model.data.toRecordId
import kotlin.uuid.Uuid

inline fun <reified T: RecordId> ResultRow.toRecordId(column: Column<EntityID<Uuid>>): T =
    this[column].value.toRecordId()

inline fun <reified T: RecordId> ResultRow.toRecordIdOrNull(column: Column<EntityID<Uuid>?>): T? =
    this.getOrNull(column)?.value?.toRecordId()

inline fun <reified T: RecordId> EntityID<Uuid>.toRecordId(): T = value.toRecordId()

// fun ResultRow.toUserId(column: Column<EntityID<UUID>>) = UserId(this[column].value.toStringId())

// fun ResultRow.toUserIdOrNull(column: Column<EntityID<UUID>?>) = this[column]?.value?.toStringId()?.let { UserId(it) }