package streetlight.server.routes

import kampfire.model.HttpProblem
import kampfire.model.Ok
import klutch.server.getApi
import streetlight.model.Api
import streetlight.model.data.toRecordId
import streetlight.server.model.ApiScope
import streetlight.server.service.readSkillContent

fun ApiScope.serveSkills() {
    getApi(Api.Skills.ReadContent, { it.toRecordId() }) {
        readSkillContent(it.data)?.let { content -> Ok(content) } ?: HttpProblem.NotFound
    }
}
