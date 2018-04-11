package com.heerkirov.bangumi.dao

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository

@Repository
class Dao(@Autowired private val database: DatabaseEngine) {
    fun dao(): DatabaseMiddleware = DatabaseMiddleware(database.session())
    fun<RET> dao(action: DatabaseMiddleware.()->RET): RET {
        val session = database.session()
        val d = DatabaseMiddleware(session)
        val ret = d.action()
        d.commitAndClose()
        return ret
    }
    fun dao(action: DatabaseMiddleware.()->Unit) {
        val session = database.session()
        val d = DatabaseMiddleware(session)
        d.action()
        d.commitAndClose()
    }
}

