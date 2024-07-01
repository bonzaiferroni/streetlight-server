package streetlight.server.data.services

import streetlight.model.User
import streetlight.server.data.DataService

class UserService : DataService<User, UserEntity>("users", UserEntity) {
    override suspend fun createEntity(data: User): UserEntity.() -> Unit = {
        name = data.name
        email = data.email
        password = data.password
    }

    override fun UserEntity.toData() = User(
        this.id.value,
        this.name,
        this.email,
        this.password
    )

    override suspend fun updateEntity(data: User): (UserEntity) -> Unit = {
        it.name = data.name
        it.email = data.email
        it.password = data.password
    }
}

