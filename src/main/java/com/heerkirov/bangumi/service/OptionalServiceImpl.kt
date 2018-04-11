package com.heerkirov.bangumi.service

import com.heerkirov.bangumi.dao.Dao
import com.heerkirov.bangumi.model.Optional
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service



@Component class OptionalServiceImpl(@Autowired private val dao: Dao): OptionalService {
    init {
        dao.dao{
            if(!queryAuto(Optional::class).exists()){
                create(Optional(allowRegister = false))
            }
        }
    }
    override var allowRegister: Boolean
        get() = dao.dao().queryAuto(Optional::class).first()!!.allowRegister
        set(value) {
            dao.dao { this.updateAuto(Optional::class).set("allowRegister" to value).commit() }
        }
}