package streetlight.server.data.user

import streetlight.model.User
import streetlight.server.data.ApiService

class UserService : ApiService() {

    suspend fun create(user: User): Int = dbQuery {
        UserEntity.new {
            name = user.name
            email = user.email
            password = user.password
        }.id.value
    }

    suspend fun read(id: Int): User? {
        return dbQuery {
            UserEntity.findById(id)
                ?.let {
                    User(
                        it.id.value,
                        it.name,
                        it.email,
                        it.password
                    )
                }
        }
    }

    suspend fun update(id: Int, user: User) {
        dbQuery {
            UserEntity.findById(id)?.let {
                it.name = user.name
                it.email = user.email
                it.password = user.password
            }
        }
    }

    suspend fun delete(id: Int) {
        dbQuery {
            UserEntity.findById(id)?.delete()
        }
    }
}

