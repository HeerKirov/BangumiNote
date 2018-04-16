package com.heerkirov.bangumi.service

import com.heerkirov.bangumi.dao.Dao
import com.heerkirov.bangumi.dao.DatabaseMiddleware
import com.heerkirov.bangumi.dao.QueryAllStruct
import com.heerkirov.bangumi.dao.QueryFeature
import com.heerkirov.bangumi.model.Bangumi
import com.heerkirov.bangumi.model.Episode
import com.heerkirov.bangumi.model.Series
import com.heerkirov.bangumi.model.User
import org.hibernate.criterion.Restrictions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service

@Component
class EpisodeServiceImpl(@Autowired private val dao: Dao) : EpisodeService {
    override fun create(obj: ServiceSet<Episode>, appendItem: Set<String>?): ServiceSet<Episode> {
        return dao.dao<ServiceSet<Episode>> {
            val user = this.query(User::class).where(Restrictions.eq("id", obj.obj.userBelong)).first()?:throw ModelWithPrimaryKeyNotFound("User", obj.obj.userBelong)
            obj.obj.userBelongId = user.incUid(Episode::class)
            this.update(user) //提交user的更改。
            this.create(obj.obj)
            ServiceSet(obj.obj)
        }
    }

    override fun update(obj: ServiceSet<Episode>, appendItem: Set<String>?): ServiceSet<Episode> {
        return dao.dao<ServiceSet<Episode>> {
            this.update(obj.obj)
            ServiceSet(obj.obj)
        }
    }

    override fun delete(obj: ServiceSet<Episode>, appendItem: Set<String>?) {
        dao.dao { this.delete(obj.obj) }
    }

    override fun queryList(feature: QueryFeature?, appendItem: Set<String>?): QueryAllStruct<ServiceSet<Episode>> {
        return dao.dao<QueryAllStruct<ServiceSet<Episode>>> {
            val qAll = this.query(Episode::class).feature(feature).qAll()
            QueryAllStruct(qAll.content.map { ServiceSet(it) }, qAll.index, qAll.count)
        }
    }

    override fun queryAll(feature: QueryFeature?, appendItem: Set<String>?): List<ServiceSet<Episode>> {
        return dao.dao<List<ServiceSet<Episode>>> {
            val qAll = this.query(Episode::class).feature(feature).all()
            qAll.map { ServiceSet(it) }
        }
    }

    override fun queryGet(index: Int, feature: QueryFeature?, appendItem: Set<String>?): ServiceSet<Episode>? {
        return dao.dao<ServiceSet<Episode>?> {
            val q = this.query(Episode::class).feature(feature).get(index)
            if(q != null)ServiceSet(q)else null
        }
    }

    override fun queryFirst(feature: QueryFeature?, appendItem: Set<String>?): ServiceSet<Episode>? {
        return dao.dao<ServiceSet<Episode>?> {
            val q = this.query(Episode::class).feature(feature).first()
            if(q != null)ServiceSet(q)else null
        }
    }

    override fun queryExists(feature: QueryFeature?): Boolean {
        return dao.dao<Boolean> { this.query(Episode::class).feature(feature).exists() }
    }
}