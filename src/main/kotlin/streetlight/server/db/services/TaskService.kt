package streetlight.server.db.services

import klutch.db.DbService
import streetlight.model.data.EditCompletion
import streetlight.model.data.TaskCompletion
import streetlight.server.model.DaoFacade

class TaskService(val dao: DaoFacade): DbService() {
    suspend fun completeTask(completion: TaskCompletion) = dbQuery {
        when (completion) {
            is EditCompletion -> completeEdit(completion)
        }
    }

    private fun completeEdit(completion: EditCompletion) {
        
    }
}