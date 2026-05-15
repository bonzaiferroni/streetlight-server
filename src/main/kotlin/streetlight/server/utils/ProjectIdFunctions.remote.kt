package streetlight.server.utils

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import streetlight.model.data.ProjectId
import streetlight.model.data.toProjectId
import java.util.UUID
import kotlin.uuid.Uuid

inline fun <reified T: ProjectId> ResultRow.toProjectId(column: Column<EntityID<Uuid>>): T =
    this[column].value.toProjectId()

inline fun <reified T: ProjectId> ResultRow.toProjectIdOrNull(column: Column<EntityID<Uuid>?>): T? =
    this.getOrNull(column)?.value?.toProjectId()

inline fun <reified T: ProjectId> EntityID<Uuid>.toProjectId(): T = value.toProjectId()

// fun ResultRow.toUserId(column: Column<EntityID<UUID>>) = UserId(this[column].value.toStringId())

// fun ResultRow.toUserIdOrNull(column: Column<EntityID<UUID>?>) = this[column]?.value?.toStringId()?.let { UserId(it) }