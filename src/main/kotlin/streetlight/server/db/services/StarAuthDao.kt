package streetlight.server.db.services

//class StarAuthDao: AuthDao<StarRecord, StarId>, DbService() {
//    override suspend fun createUser(seed: UserSeed) = dbQuery {
//        val now = Clock.System.now()
//        val user = StarRecord(
//            starId = StarId.random(),
//            username = seed.request.username,
//            hashedPassword = seed.hashedPassword,
//            salt = seed.salt,
//            email = seed.request.email,
//            roles = seed.roles.toSet(),
//            createdAt = now,
//            updatedAt = now,
//        )
//
//        StarTable.insertAndGetId {
//            it.createRecord(user, seed.accountType)
//        }.let { StarId(it.value) }
//    }
//
//    override suspend fun readIdByUsername(username: Username) = dbQuery {
//        StarTable.select(StarTable.id).where { StarTable.username.eq(username) }
//            .firstOrNull()?.getOrNull(StarTable.id)?.toRecordId<StarId>()
//    }
//
//    override suspend fun readByUsernameOrEmail(identity: String): UserRecord? = dbQuery {
//        StarTable.readFirstOrNull {
//            eqIdentity(identity)
//        }?.toUserRecord()
//    }
//
//    override suspend fun readPrivateInfo(identity: String) = dbQuery {
//        StarTable.select(StarTable.name, StarTable.email)
//            .where { eqIdentity(identity) }
//            .firstOrNull()
//            ?.let { PrivateInfo(it[StarTable.name], it[StarTable.email]) }
//    }
//
//    override suspend fun readSaltExists(salt: String) = dbQuery {
//        StarTable
//            .select(StarTable.salt)
//            .where { StarTable.salt.eq(salt) }
//            .firstOrNull() != null
//    }
//}
//
//private fun eqIdentity(identity: String) =
//    (StarTable.username.lowerCase() eq identity.lowercase()) or (StarTable.email.lowerCase() eq identity.lowercase())
