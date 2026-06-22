package streetlight.server.db.services

import klutch.db.DbService
import streetlight.model.data.EditLogId
import streetlight.model.data.EditType
import streetlight.server.db.tables.QuorumTable
import streetlight.server.model.DaoFacade

class QuestionService(private val dao: DaoFacade): DbService() {

    suspend fun initiateQuestion(editLogId: EditLogId, editType: EditType) = dbQuery {

    }
}