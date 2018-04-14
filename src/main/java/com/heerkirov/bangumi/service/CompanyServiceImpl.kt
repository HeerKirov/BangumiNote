package com.heerkirov.bangumi.service

import com.heerkirov.bangumi.dao.Dao
import com.heerkirov.bangumi.dao.DatabaseMiddleware
import com.heerkirov.bangumi.dao.QueryAllStruct
import com.heerkirov.bangumi.dao.QueryFeature
import com.heerkirov.bangumi.model.Company
import com.heerkirov.bangumi.model.User
import org.hibernate.criterion.Restrictions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service

@Component class CompanyServiceImpl(@Autowired private val dao: Dao) : CompanyService {
    override fun create(obj: ServiceSet<Company>, appendItem: Set<String>?): ServiceSet<Company> {
        return dao.dao<ServiceSet<Company>> {
            val user = this.query(User::class).where(Restrictions.eq("id", obj.obj.userBelong)).first()?:throw ModelWithPrimaryKeyNotFound("User", obj.obj.userBelong)
            obj.obj.userBelongId = user.incUid(Company::class)
            this.update(user) //提交user的更改。
            this.create(obj.obj)
            ServiceSet(obj.obj)
        }
    }

    override fun update(obj: ServiceSet<Company>, appendItem: Set<String>?): ServiceSet<Company> {
        return dao.dao<ServiceSet<Company>> {
            this.update(obj.obj)
            ServiceSet(obj.obj)
        }
    }

    override fun delete(obj: ServiceSet<Company>, appendItem: Set<String>?) {
        dao.dao { this.delete(obj.obj) }
    }

    override fun queryList(feature: QueryFeature?, appendItem: Set<String>?): QueryAllStruct<ServiceSet<Company>> {
        return dao.dao<QueryAllStruct<ServiceSet<Company>>> {
            val qAll = this.query(Company::class).feature(feature).qAll()
            QueryAllStruct(qAll.content.map { ServiceSet(it) }, qAll.index, qAll.count)
        }
    }

    override fun queryAll(feature: QueryFeature?, appendItem: Set<String>?): List<ServiceSet<Company>> {
        return dao.dao<List<ServiceSet<Company>>> {
            val qAll = this.query(Company::class).feature(feature).all()
            qAll.map { ServiceSet(it) }
        }
    }

    override fun queryGet(index: Int, feature: QueryFeature?, appendItem: Set<String>?): ServiceSet<Company>? {
        return dao.dao<ServiceSet<Company>?> {
            val q = this.query(Company::class).feature(feature).get(index)
            if(q != null)ServiceSet(q)else null
        }
    }

    override fun queryFirst(feature: QueryFeature?, appendItem: Set<String>?): ServiceSet<Company>? {
        return dao.dao<ServiceSet<Company>?> {
            val q = this.query(Company::class).feature(feature).first()
            if(q != null)ServiceSet(q)else null
        }
    }

    override fun queryExists(feature: QueryFeature?): Boolean {
        return dao.dao<Boolean> { this.query(Company::class).feature(feature).exists() }
    }

}