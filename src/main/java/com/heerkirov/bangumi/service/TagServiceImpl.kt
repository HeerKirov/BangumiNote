package com.heerkirov.bangumi.service

import com.heerkirov.bangumi.dao.Dao
import com.heerkirov.bangumi.dao.QueryAllStruct
import com.heerkirov.bangumi.dao.QueryFeature
import com.heerkirov.bangumi.model.Tag
import com.heerkirov.bangumi.model.User
import org.hibernate.criterion.Restrictions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class TagServiceImpl(@Autowired private val dao: Dao): TagService {
    override fun create(obj: ServiceSet<Tag>, appendItem: Set<String>?): ServiceSet<Tag> {
        return dao.dao<ServiceSet<Tag>> {
            val user = this.query(User::class).where(Restrictions.eq("id", obj.obj.userBelong)).first()?:throw ModelWithPrimaryKeyNotFound("User", obj.obj.userBelong)
            obj.obj.userBelongId = user.incUid(Tag::class)
            obj.obj.parent?.let { parent -> //非递归验证。
                if(parent.id != null && parent.id == obj.obj.id)throw RecursiveDependence("Tag", "parent")
            }
            mappingTreat(obj.obj, "parent", Tag::class, user, allowNew = false)
            this.update(user) //提交user的更改。
            this.create(obj.obj)
            ServiceSet(obj.obj)
        }
    }

    override fun update(obj: ServiceSet<Tag>, appendItem: Set<String>?): ServiceSet<Tag> {
        return dao.dao<ServiceSet<Tag>> {
            val user = this.query(User::class).where(Restrictions.eq("id", obj.obj.userBelong)).first()?:throw ModelWithPrimaryKeyNotFound("User", obj.obj.userBelong)
            obj.obj.parent?.let { parent -> //非递归验证。
                if(parent.id != null && parent.id == obj.obj.id)throw RecursiveDependence("Tag", "parent")
            }
            mappingTreat(obj.obj, "parent", Tag::class, user, allowNew = false)
            this.update(obj.obj)
            ServiceSet(obj.obj)
        }
    }

    override fun delete(obj: ServiceSet<Tag>, appendItem: Set<String>?) {
        dao.dao { this.delete(obj.obj) }
    }

    override fun queryList(feature: QueryFeature?, appendItem: Set<String>?): QueryAllStruct<ServiceSet<Tag>> {
        return dao.dao<QueryAllStruct<ServiceSet<Tag>>> {
            val qAll = this.query(Tag::class).feature(feature).qAll()
            QueryAllStruct(qAll.content.map { ServiceSet(it) }, qAll.index, qAll.count)
        }
    }

    override fun queryAll(feature: QueryFeature?, appendItem: Set<String>?): List<ServiceSet<Tag>> {
        return dao.dao<List<ServiceSet<Tag>>> {
            val qAll = this.query(Tag::class).feature(feature).all()
            qAll.map { ServiceSet(it) }
        }
    }

    override fun queryGet(index: Int, feature: QueryFeature?, appendItem: Set<String>?): ServiceSet<Tag>? {
        return dao.dao<ServiceSet<Tag>?> {
            val q = this.query(Tag::class).feature(feature).get(index)
            if(q != null)ServiceSet(q)else null
        }
    }

    override fun queryFirst(feature: QueryFeature?, appendItem: Set<String>?): ServiceSet<Tag>? {
        return dao.dao<ServiceSet<Tag>?> {
            val q = this.query(Tag::class).feature(feature).first()
            if(q != null)ServiceSet(q)else null
        }
    }

    override fun queryExists(feature: QueryFeature?): Boolean {
        return dao.dao<Boolean> { this.query(Tag::class).feature(feature).exists() }
    }
}