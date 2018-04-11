package com.heerkirov.bangumi.service

import com.heerkirov.bangumi.dao.*
import com.heerkirov.bangumi.model.User
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service


@Component
class UserServiceImpl(@Autowired private val dao: Dao): UserService {
    override fun create(obj: ServiceSet<User>, appendItem: Set<String>?): ServiceSet<User> {
        return dao.dao<ServiceSet<User>> {
            this.create(obj.obj)
            ServiceSet(obj.obj)
        }
    }

    override fun update(obj: ServiceSet<User>, appendItem: Set<String>?): ServiceSet<User> {
        return dao.dao<ServiceSet<User>> {
            this.update(obj.obj)
            ServiceSet(obj.obj)
        }
    }

    override fun delete(obj: ServiceSet<User>, appendItem: Set<String>?) {
        dao.dao { this.delete(obj.obj) }
    }

    override fun queryList(feature: QueryFeature?, appendItem: Set<String>?): QueryAllStruct<ServiceSet<User>> {
        return dao.dao<QueryAllStruct<ServiceSet<User>>> {
            val qAll = this.query(User::class).feature(feature).qAll()
            QueryAllStruct(qAll.content.map { ServiceSet(it) }, qAll.index, qAll.count)
        }
    }

    override fun queryAll(feature: QueryFeature?, appendItem: Set<String>?): List<ServiceSet<User>> {
        return dao.dao<List<ServiceSet<User>>> {
            val qAll = this.query(User::class).feature(feature).all()
            qAll.map { ServiceSet(it) }
        }
    }

    override fun queryGet(index: Int, feature: QueryFeature?, appendItem: Set<String>?): ServiceSet<User>? {
        return dao.dao<ServiceSet<User>?> {
            val q = this.query(User::class).feature(feature).get(index)
            if(q != null)ServiceSet(q)else null
        }
    }

    override fun queryFirst(feature: QueryFeature?, appendItem: Set<String>?): ServiceSet<User>? {
        return dao.dao<ServiceSet<User>?> {
            val q = this.query(User::class).feature(feature).first()
            if(q != null)ServiceSet(q)else null
        }
    }

    override fun queryExists(feature: QueryFeature?): Boolean {
        return dao.dao<Boolean> { this.query(User::class).feature(feature).exists() }
    }
}
