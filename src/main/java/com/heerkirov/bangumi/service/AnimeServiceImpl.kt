package com.heerkirov.bangumi.service

import com.heerkirov.bangumi.dao.Dao
import com.heerkirov.bangumi.dao.DatabaseMiddleware
import com.heerkirov.bangumi.dao.QueryAllStruct
import com.heerkirov.bangumi.dao.QueryFeature
import com.heerkirov.bangumi.model.*
import com.heerkirov.bangumi.util.Restrictions_in
import org.hibernate.criterion.Restrictions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import java.util.*

/** Anime的服务层有需要特殊处理的地方。
 * Anime的Series/Author是指向相应model的外键，在创建时通常指定外键，但也应当支持直接创建操作。
 * 在create/update时，同步检查series/authors的正确性。通常包含用户所属的正确性。
 * create-set服务：
 *      [authorNew: List<Author>]同步创建Author
 *      [author: List<Int>]将这些Author加入。两个选项不冲突。
 *      [seriesNew: Series]同步创建Series
 * update-set服务：
 *      [authorNew: List<Author>]创建新的Author并附加
 *      [author: List<Int>]将author列表设定为这些内容。new得到的内容会附加在这之后。
 *      [seriesNew: Series]创建一个新Series
 */
@Component
class AnimeServiceImpl(@Autowired val dao: Dao): AnimeService {
    override fun create(obj: ServiceSet<Anime>, appendItem: Set<String>?): ServiceSet<Anime> {
        return dao.dao<ServiceSet<Anime>> {
            //添加UserBelong依赖并推进uid
            val user = this.query(User::class).where(Restrictions.eq("id", obj.obj.userBelong)).first()?:throw ModelWithPrimaryKeyNotFound("User", obj.obj.userBelong)
            obj.obj.userBelongId = user.incUid(Anime::class)
           //处理series的附加问题。
            mappingTreat(obj.obj, "series", Series::class, user)
            //处理author的附加问题。
            //mappingManyTreat(obj.obj, "authorList", Author::class, user, AuthorToAnime::class, "animeId", "authorId")
            mappingSetTreat(obj.obj, "authorList", Author::class, user)
            //CREATE时不需要处理tagList。
            //构造主要内容，并获得主键。
            this.create(obj.obj)
            //最后提交对user的更改，因为中途会用到对user的修改。
            this.update(user)
            val ret = ServiceSet(obj.obj)
            ret
        }
    }

    override fun update(obj: ServiceSet<Anime>, appendItem: Set<String>?): ServiceSet<Anime> {
        return dao.dao<ServiceSet<Anime>> {
            //获得User并检查是否非空。
            val user = this.query(User::class).where(Restrictions.eq("id", obj.obj.userBelong)).first()?:throw ModelWithPrimaryKeyNotFound("User", obj.obj.userBelong)
            //obj.obj.userBelongId = user.incUid(Anime::class)
            mappingTreat(obj.obj, "series", Series::class, user)
            //mappingManyTreat(obj.obj, "authorList", Author::class, user, AuthorToAnime::class, "animeId", "authorId")
            mappingSetTreat(obj.obj, "authorList", Author::class, user)
            mappingSetTreat(obj.obj, "tagList", Tag::class, user)
            this.update(obj.obj)
            val ret = ServiceSet(obj.obj)
            ret
        }
    }

    override fun delete(obj: ServiceSet<Anime>, appendItem: Set<String>?) {
        dao.dao { this.delete(obj.obj) }
    }

    override fun queryList(feature: QueryFeature?, appendItem: Set<String>?): QueryAllStruct<ServiceSet<Anime>> {
        return dao.dao<QueryAllStruct<ServiceSet<Anime>>> {
            val qAll = this.query(Anime::class).feature(feature).joinSelect(fetchSelectList).qAll()
            QueryAllStruct(qAll.content.map { ServiceSet(it) }, qAll.index, qAll.count)
        }
    }

    override fun queryAll(feature: QueryFeature?, appendItem: Set<String>?): List<ServiceSet<Anime>> {
        return dao.dao<List<ServiceSet<Anime>>> {
            val qAll = this.query(Anime::class).feature(feature).joinSelect(fetchSelectList).all()
            qAll.map { ServiceSet(it) }
        }
    }

    override fun queryGet(index: Int, feature: QueryFeature?, appendItem: Set<String>?): ServiceSet<Anime>? {
        return dao.dao<ServiceSet<Anime>?> {
            val q = this.query(Anime::class).feature(feature).joinSelect(fetchSelectList).get(index)
            if(q != null){ServiceSet(q)}else null
        }
    }

    override fun queryFirst(feature: QueryFeature?, appendItem: Set<String>?): ServiceSet<Anime>? {
        return dao.dao<ServiceSet<Anime>?> {
            val q = this.query(Anime::class).feature(feature).joinSelect(fetchSelectList).first()
            if(q != null){ServiceSet(q)}else null
        }
    }

    override fun queryExists(feature: QueryFeature?): Boolean {
        return dao.dao<Boolean> { this.query(Anime::class).feature(feature).joinSelect(fetchSelectList).exists() }
    }

    private val fetchSelectList = listOf("authorList", "tagList")

}