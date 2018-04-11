package com.heerkirov.bangumi.service

import com.heerkirov.bangumi.dao.Dao
import com.heerkirov.bangumi.dao.DatabaseMiddleware
import com.heerkirov.bangumi.dao.QueryAllStruct
import com.heerkirov.bangumi.dao.QueryFeature
import com.heerkirov.bangumi.model.Author
import com.heerkirov.bangumi.model.User
import org.hibernate.criterion.Restrictions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service

@Component
class AuthorServiceImpl(@Autowired private val dao: Dao): AuthorService {
    override fun create(obj: ServiceSet<Author>, appendItem: Set<String>?): ServiceSet<Author> {
        return dao.dao<ServiceSet<Author>> {
            val user = obj.obj.user
            if(user!=null){
                obj.obj.userBelongId = user.incUid(Author::class)
                this.update(user) //提交user的更改。
            }
            this.create(obj.obj)
            ServiceSet(obj.obj)
        }
    }

    override fun update(obj: ServiceSet<Author>, appendItem: Set<String>?): ServiceSet<Author> {
        return dao.dao<ServiceSet<Author>> {
            this.update(obj.obj)
            ServiceSet(obj.obj)
        }
    }

    override fun delete(obj: ServiceSet<Author>, appendItem: Set<String>?) {
        dao.dao { this.delete(obj.obj) }
    }

    override fun queryList(feature: QueryFeature?, appendItem: Set<String>?): QueryAllStruct<ServiceSet<Author>> {
        return dao.dao<QueryAllStruct<ServiceSet<Author>>> {
            val qAll = this.query(Author::class).feature(feature).qAll()
            QueryAllStruct(qAll.content.map { ServiceSet(it) }, qAll.index, qAll.count)
        }
    }

    override fun queryAll(feature: QueryFeature?, appendItem: Set<String>?): List<ServiceSet<Author>> {
        return dao.dao<List<ServiceSet<Author>>> {
            val qAll = this.query(Author::class).feature(feature).all()
            qAll.map { ServiceSet(it) }
        }
    }

    override fun queryGet(index: Int, feature: QueryFeature?, appendItem: Set<String>?): ServiceSet<Author>? {
        return dao.dao<ServiceSet<Author>?> {
            val q = this.query(Author::class).feature(feature).get(index)
            if(q != null)ServiceSet(q)else null
        }
    }

    override fun queryFirst(feature: QueryFeature?, appendItem: Set<String>?): ServiceSet<Author>? {
        return dao.dao<ServiceSet<Author>?> {
            val q = this.query(Author::class).feature(feature).first()
            if(q != null)ServiceSet(q)else null
        }
    }

    override fun queryExists(feature: QueryFeature?): Boolean {
        return dao.dao<Boolean> { this.query(Author::class).feature(feature).exists() }
    }

}