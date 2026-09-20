package streetlight.server.api

import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kampfire.api.GetEndpoint
import kampfire.api.PostEndpoint
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

suspend inline fun <reified Returned, E : GetEndpoint<Returned>> ApiTestScope.getApi(
    endpoint: E,
): Outcome<Returned> = http.get(endpoint.path).toOutcome()

@OptIn(ExperimentalSerializationApi::class)
suspend inline fun <reified T> HttpResponse.toOutcome(): Outcome<T> =
    if (status != HttpStatusCode.OK) {
        status.toProblem()
    } else {
        Cbor.decodeFromByteArray(OutcomeSerializer(serializer<T>()), body<ByteArray>())
    }
