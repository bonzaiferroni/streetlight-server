package streetlight.server.db.tables

import kampfire.api.TableId
import kampfire.api.toSlug
import kampfire.api.toUsername
import klutch.utils.eq
import org.jetbrains.exposed.v1.core.Coalesce
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ExpressionWithColumnType
import org.jetbrains.exposed.v1.core.Join
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.core.intLiteral
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.Query
import org.jetbrains.exposed.v1.jdbc.select
import streetlight.model.data.MediaPost
import streetlight.model.data.EventPost
import streetlight.model.data.GalaxyTrace
import streetlight.model.data.LocationPost
import streetlight.model.data.Post
import streetlight.model.data.PostCursor
import streetlight.model.data.PostType
import streetlight.model.data.SortDirection
import streetlight.server.utils.toRecordId
import kotlin.uuid.Uuid

object GalaxyPostAspect {
    val PostColumns = listOf(
        PostTable.id,
        PostTable.galaxyId,
        PostTable.mediaId,
        PostTable.galaxySlug,
        PostTable.galaxyName,
        PostTable.postType,
        PostTable.username,
        PostTable.title,
        PostTable.text,
        PostTable.starCount,
        PostTable.lean,
        PostTable.createdAt,
        PostTable.updatedAt,
    )

    val GalaxyPostColumns = (EventLocationColumns + LocationAspect.columns + PostColumns + MediaColumns).distinct()

    private fun baseQuery() = PostTable
        .leftJoin(EventTable)
        .join(LocationTable, JoinType.LEFT, PostTable.locationId, LocationTable.id)
        .leftJoin(MediaTable)

    fun query() = baseQuery()
        .select(GalaxyPostColumns)

    fun queryCursor(cursor: PostCursor) = baseQuery()
        .joinCursor(cursor)
        .select(if (cursor is PostCursor.Mark) GalaxyPostColumns + MarkCount else GalaxyPostColumns)

    val MarkCount = Coalesce(PostMarkCountTable.count, intLiteral(0))

    fun afterCursor(cursor: PostCursor): Op<Boolean>? {
        val postId = cursor.postId ?: return null
        return when (cursor) {
            is PostCursor.Time -> cursor.recordAt?.let {
                afterCursor(PostTable.createdAt, it, PostTable.id, postId, cursor.direction)
            }
            is PostCursor.Lean -> cursor.postLean?.let {
                afterCursor(PostTable.lean, it, PostTable.id, postId, cursor.direction)
            }
            is PostCursor.Mark -> cursor.count?.let {
                afterCursor(MarkCount, it, PostTable.id, postId, cursor.direction)
            }
        }
    }
}

fun Join.joinCursor(cursor: PostCursor): Join {
    if (cursor is PostCursor.Mark) {
        return join(PostMarkCountTable, JoinType.LEFT, PostTable.id, PostMarkCountTable.postId,
            additionalConstraint = { PostMarkCountTable.galaxyMarkId.eq(cursor.markId) })
    }
    return this
}

fun Query.orderByCursor(cursor: PostCursor): Query {
    val order = when (cursor.direction) {
        SortDirection.Descending -> SortOrder.DESC_NULLS_LAST
        SortDirection.Ascending -> SortOrder.ASC_NULLS_LAST
    }
    return when (cursor) {
        is PostCursor.Mark -> orderBy(GalaxyPostAspect.MarkCount to order, PostTable.id to order)
        is PostCursor.Lean -> orderBy(PostTable.lean to order, PostTable.id to order)
        is PostCursor.Time -> orderBy(PostTable.createdAt to order, PostTable.id to order)
    }
}



fun <T : Comparable<T>> afterCursor(
    sortColumn: ExpressionWithColumnType<T>,
    sortValue: T,
    idColumn: Column<EntityID<Uuid>>,
    idValue: TableId<Uuid>,
    direction: SortDirection,
): Op<Boolean> = when (direction) {
    SortDirection.Descending ->
        sortColumn.less(sortValue) or (sortColumn.eq(sortValue) and idColumn.less(idValue.value))
    SortDirection.Ascending ->
        sortColumn.greater(sortValue) or (sortColumn.eq(sortValue) and idColumn.greater(idValue.value))
}

fun ResultRow.toGalaxyPost() = when (this[PostTable.postType]) {
    PostType.Event -> EventPost(
        event = this.toEventLocation(),
        post = this.toPost(),
    )
    PostType.Location -> LocationPost(
        post = this.toPost(),
        location = this.toLocation(),
    )
    PostType.Media -> MediaPost(
        media = this.toMedia(),
        post = this.toPost()
    )
}

fun ResultRow.toPost() = Post(
    postId = this[PostTable.id].toRecordId(),
    galaxy = toGalaxyTrace(),
    postType = this[PostTable.postType],
    username = this[PostTable.username]?.toUsername(),
    title = this[PostTable.title],
    text = this[PostTable.text],
    lightCount = this[PostTable.starCount],
    lean = this[PostTable.lean],
    markCount = getOrNull(GalaxyPostAspect.MarkCount),
    createdAt = this[PostTable.createdAt],
    updatedAt = this[PostTable.updatedAt],
)

fun ResultRow.toGalaxyTrace() = GalaxyTrace(
    galaxyId = this[PostTable.galaxyId].toRecordId(),
    name = this[PostTable.galaxyName] ?: error("galaxy name not found"),
    slug = this[PostTable.galaxySlug]?.toSlug() ?: error("galaxy slug not found"),
)