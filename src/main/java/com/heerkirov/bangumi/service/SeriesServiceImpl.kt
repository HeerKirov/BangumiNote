package com.heerkirov.bangumi.service

import com.heerkirov.bangumi.dao.Dao
import com.heerkirov.bangumi.dao.DatabaseMiddleware
import com.heerkirov.bangumi.dao.QueryAllStruct
import com.heerkirov.bangumi.dao.QueryFeature
import com.heerkirov.bangumi.model.Series
import com.heerkirov.bangumi.model.User
import org.hibernate.criterion.Restrictions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service

@Component
class SeriesServiceImpl(@Autowired private val dao: Dao) : SeriesService {
    override fun create(obj: ServiceSet<Series>, appendItem: Set<String>?): ServiceSet<Series> {
        return dao.dao<ServiceSet<Series>> {
            val user = this.query(User::class).where(Restrictions.eq("id", obj.obj.userBelong)).first()?:throw ModelWithPrimaryKeyNotFound("User", obj.obj.userBelong)
            obj.obj.userBelongId = user.incUid(Series::class)
            this.update(user) //提交user的更改。
            this.create(obj.obj)
            ServiceSet(obj.obj)
        }
    }

    override fun update(obj: ServiceSet<Series>, appendItem: Set<String>?): ServiceSet<Series> {
        return dao.dao<ServiceSet<Series>> {
            this.update(obj.obj)
            ServiceSet(obj.obj)
        }
    }

    override fun delete(obj: ServiceSet<Series>, appendItem: Set<String>?) {
        dao.dao { this.delete(obj.obj) }
    }

    override fun queryList(feature: QueryFeature?, appendItem: Set<String>?): QueryAllStruct<ServiceSet<Series>> {
        return dao.dao<QueryAllStruct<ServiceSet<Series>>> {
            val qAll = this.query(Series::class).feature(feature).qAll()
            QueryAllStruct(qAll.content.map { ServiceSet(it) }, qAll.index, qAll.count)
        }
    }

    override fun queryAll(feature: QueryFeature?, appendItem: Set<String>?): List<ServiceSet<Series>> {
        return dao.dao<List<ServiceSet<Series>>> {
            val qAll = this.query(Series::class).feature(feature).all()
            qAll.map { ServiceSet(it) }
        }
    }

    override fun queryGet(index: Int, feature: QueryFeature?, appendItem: Set<String>?): ServiceSet<Series>? {
        return dao.dao<ServiceSet<Series>?> {
            val q = this.query(Series::class).feature(feature).get(index)
            if(q != null)ServiceSet(q)else null
        }
    }

    override fun queryFirst(feature: QueryFeature?, appendItem: Set<String>?): ServiceSet<Series>? {
        return dao.dao<ServiceSet<Series>?> {
            val q = this.query(Series::class).feature(feature).first()
            if(q != null)ServiceSet(q)else null
        }
    }

    override fun queryExists(feature: QueryFeature?): Boolean {
        return dao.dao<Boolean> { this.query(Series::class).feature(feature).exists() }
    }
}