package com.heerkirov.bangumi.service

import com.heerkirov.bangumi.dao.Dao
import com.heerkirov.bangumi.dao.DatabaseMiddleware
import com.heerkirov.bangumi.dao.QueryAllStruct
import com.heerkirov.bangumi.dao.QueryFeature
import com.heerkirov.bangumi.model.*
import org.hibernate.criterion.Restrictions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class BangumiServiceImpl(@Autowired val dao: Dao): BangumiService {
    /*TODO 接下来要做的
        5. 创建tag相关api和页面。
        6. 想一个方案，使bangumi在update时，能够有选择地更新anime的commit，而不是一股脑全更新了。
        7. 添加新的前端控件，用于处理布尔值。
    */
    override fun create(obj: ServiceSet<Bangumi>, appendItem: Set<String>?): ServiceSet<Bangumi> {
        return dao.dao<ServiceSet<Bangumi>> {
            //添加UserBelong依赖并推进uid
            val user = this.query(User::class).where(Restrictions.eq("id", obj.obj.userBelong)).first()?:throw ModelWithPrimaryKeyNotFound("User", obj.obj.userBelong)
            obj.obj.userBelongId = user.incUid(Bangumi::class)
            //处理anime的附加问题。
            mappingTreat(obj.obj, "anime", Anime::class, user, allowNew = false, notNull = true)
            //处理company的附加问题。
            mappingSetTreat(obj.obj, "companyList", Company::class, user)
            mappingSetTreat(obj.obj, "tagList", Tag::class, user)
            //构造主要内容，并获得主键。
            val pkey = this.create(obj.obj) as Int
            //处理commit
            analysisCommit(obj.obj.anime!!)
            //最后提交对user的更改，因为中途会用到对user的修改。
            this.update(user)
            val ret = ServiceSet(obj.obj)
            ret
        }
    }

    override fun update(obj: ServiceSet<Bangumi>, appendItem: Set<String>?): ServiceSet<Bangumi> {
        return dao.dao<ServiceSet<Bangumi>> {
            //获得User并检查是否非空。
            val user = this.query(User::class).where(Restrictions.eq("id", obj.obj.userBelong)).first()?:throw ModelWithPrimaryKeyNotFound("User", obj.obj.userBelong)
            //处理anime的附加问题。
            mappingTreat(obj.obj, "anime", Anime::class, user, allowNew = false, notNull = true)
            //处理company的附加问题。
            mappingSetTreat(obj.obj, "companyList", Company::class, user)
            mappingSetTreat(obj.obj, "tagList", Tag::class, user)
            this.update(obj.obj)
            //处理commit
            analysisCommit(obj.obj.anime!!)
            val pkey = obj.obj.id!!
            val ret = ServiceSet(obj.obj)
            ret
        }
    }

    private fun DatabaseMiddleware.analysisCommit(anime: Anime) {
        //这个函数用来处理bangumi的commit的问题。
        //在更新bangumi时，需要把commit相关信息向上更新。
        val bangumiList = this.query(Bangumi::class).where(Restrictions.eq("anime", anime)).all()
        anime.scoreLike = bangumiList.mapNotNull { it.scoreLike }.average()
        anime.scorePatient = bangumiList.mapNotNull { it.scorePatient }.average()
        anime.makeMake = bangumiList.mapNotNull { it.makeMake }.average()
        anime.makeDrama = bangumiList.mapNotNull { it.makeDrama }.average()
        anime.makeMusic = bangumiList.mapNotNull { it.makeMusic }.average()
        anime.makePerson = bangumiList.mapNotNull { it.makePerson }.average()
        anime.makeBackground = bangumiList.mapNotNull { it.makeBackground }.average()
        anime.levelR18 = bangumiList.mapNotNull { it.levelR18 }.average()
        anime.levelR18G = bangumiList.mapNotNull { it.levelR18G }.average()
        val tagList = HashSet<Tag>()
        anime.tagList = tagList
        this.update(anime)
        bangumiList.forEach { tagList.addAll(it.tagList) }
        anime.tagList = tagList
        this.update(anime)
    }

    override fun delete(obj: ServiceSet<Bangumi>, appendItem: Set<String>?) {
        dao.dao { this.delete(obj.obj) }
    }

    override fun queryList(feature: QueryFeature?, appendItem: Set<String>?): QueryAllStruct<ServiceSet<Bangumi>> {
        return dao.dao<QueryAllStruct<ServiceSet<Bangumi>>> {
            val qAll = this.query(Bangumi::class).feature(feature).joinSelect(fetchSelectList).qAll()
            QueryAllStruct(qAll.content.map { ServiceSet(it) }, qAll.index, qAll.count)
        }
    }

    override fun queryAll(feature: QueryFeature?, appendItem: Set<String>?): List<ServiceSet<Bangumi>> {
        return dao.dao<List<ServiceSet<Bangumi>>> {
            val qAll = this.query(Bangumi::class).feature(feature).joinSelect(fetchSelectList).all()
            qAll.map { ServiceSet(it) }
        }
    }

    override fun queryGet(index: Int, feature: QueryFeature?, appendItem: Set<String>?): ServiceSet<Bangumi>? {
        return dao.dao<ServiceSet<Bangumi>?> {
            val q = this.query(Bangumi::class).feature(feature).joinSelect(fetchSelectList).get(index)
            if(q != null){ServiceSet(q)}else null
        }
    }

    override fun queryFirst(feature: QueryFeature?, appendItem: Set<String>?): ServiceSet<Bangumi>? {
        return dao.dao<ServiceSet<Bangumi>?> {
            val q = this.query(Bangumi::class).feature(feature).joinSelect(fetchSelectList).first()
            if(q != null){ServiceSet(q)}else null
        }
    }

    override fun queryExists(feature: QueryFeature?): Boolean {
        return dao.dao<Boolean> { this.query(Bangumi::class).feature(feature).joinSelect(fetchSelectList).exists() }
    }

    private val fetchSelectList = listOf("tagList", "companyList")
}