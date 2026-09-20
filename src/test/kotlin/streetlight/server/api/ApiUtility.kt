package streetlight.server.api

import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kampfire.api.DeleteEndpoint
import kampfire.api.GetByIdEndpoint
import kampfire.api.GetEndpoint
import kampfire.api.PathBuilder
import kampfire.api.PostEndpoint
import kampfire.api.QueryEndpoint
import kampfire.model.Outcome
import kampfire.model.OutcomeSerializer
import kampfire.model.toProblem
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.serializer

suspend inline fun <reified Sent : Any, reified Returned, E : PostEndpoint<Sent, Returned>> ApiTestScope.postApi(
    endpoint: E,
    body: Sent,
): Outcome<Returned> = http.post(endpoint.path) {
    contentType(ContentType.Application.Json)
    setBody(body)
}.toOutcome()

suspend inline fun <reified Returned> ApiTestScope.postApi(
    endpoint: PostEndpoint<Unit, Returned>,
): Outcome<Returned> = http.post(endpoint.path).toOutcome()

suspend inline fun <reified Sent : Any, E : DeleteEndpoint<Sent>> ApiTestScope.deleteApi(
    endpoint: E,
    body: Sent,
): Outcome<Boolean> = http.delete(endpoint.path) {
    contentType(ContentType.Application.Json)
    setBody(body)
}.toOutcome()

suspend inline fun <reified Returned, E : GetEndpoint<Returned>> ApiTestScope.getApi(
    endpoint: E,
    noinline block: (PathBuilder.(E) -> Unit)? = null,
): Outcome<Returned> = http.get(resolvePath(endpoint.path, endpoint, block)).toOutcome()

suspend inline fun <Id, reified Returned, E : GetByIdEndpoint<Id, Returned>> ApiTestScope.getApi(
    endpoint: E,
    id: Id,
    noinline block: (PathBuilder.(E) -> Unit)? = null,
): Outcome<Returned> = http.get(resolvePath("${endpoint.path}/$id", endpoint, block)).toOutcome()

suspend inline fun <reified Sent, reified Returned> ApiTestScope.getApi(
    endpoint: QueryEndpoint<Sent, Returned>,
    query: String?,
): Outcome<Returned> {
    val url = if (!query.isNullOrEmpty()) "${endpoint.path}?$query" else endpoint.path
    return http.get(url).toOutcome()
}

fun <E> resolvePath(path: String, endpoint: E, block: (PathBuilder.(E) -> Unit)?): String {
    val block = block ?: return path
    return PathBuilder(path).apply { block(endpoint) }.build()
}

@OptIn(ExperimentalSerializationApi::class)
suspend inline fun <reified T> HttpResponse.toOutcome(): Outcome<T> =
    if (status != HttpStatusCode.OK) {
        status.toProblem()
    } else {
        Cbor.decodeFromByteArray(OutcomeSerializer(serializer<T>()), body<ByteArray>())
    }
