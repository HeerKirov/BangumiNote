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
//@Component
//class AnimeServiceImplCopy(@Autowired val dao: Dao): AnimeService {
//    override fun create(obj: ServiceSet<Anime>, appendItem: Set<String>?): ServiceSet<Anime> {
//        return dao.dao<ServiceSet<Anime>> {
//            //添加UserBelong依赖并推进uid
//            val user = obj.obj.user!!
//            obj.obj.userBelongId = user.incUid(Anime::class)
//            //append item
//            val appends = HashMap<String, Any?>()
//            //处理Series依赖。有New时将体替换为new，没有时检查原有的series。
//            //Series可能加入append item。
//            if(obj.contains("seriesNew")) {//添加新的Series
//                val new_series = obj["seriesNew"] as Series
//                initializeUBModel(new_series, user, Series::class) //初始化Series的必备内容
//                val seriesId = this.create(new_series) as Int
//                obj.obj.seriesId = seriesId //由于series的id记录在anime内，所以它的创建在这之前
//                if(appendItem!=null&&"series" in appendItem)appends.put("series", new_series)
//            }else if(obj.obj.seriesId != null) {//手动提供Series
//                val local_series = this.query(Series::class).where(Restrictions.eq("id", obj.obj.seriesId)).first()
//                //检查是否存在
//                local_series ?: ModelWithPrimaryKeyNotFound("Series", obj.obj.seriesId!!.toString())
//                //检查Series的userbelong是否属于当前用户。
//                if(local_series!!.userBelong != user.id) throw UserForbidden("Series", local_series.id!!.toString())
//                if(appendItem!=null&&"series" in appendItem)appends.put("series", local_series)
//            }else{
//                if(appendItem!=null&&"series" in appendItem)appends.put("series", null)
//            }
//            //构造主要内容，并获得主键。
//            val pkey = this.create(obj.obj) as Int
//            //处理Author依赖。
//            val append_authors = ArrayList<Author>()
//            if(obj.contains("authorNew")) {//创建新的Author
//                val authors = obj["authorNew"] as List<Author>
//                for(author in authors) {
//                    initializeUBModel(author, user, Author::class)
//                    val authorId = this.create(author) as Int
//                    this.create(AuthorToAnime(authorId = authorId, animeId = pkey))//由于author是many to many关系，依赖于anime的主键，所以在这之后。
//                }
//                append_authors.addAll(authors)
//            }
//            if(obj.contains("author")) {//附加已经存在的Author
//                val authorIds = obj["author"] as List<Int>
//                val authors = this.query(Author::class).where(Restrictions_in("id", authorIds)).all()
//                for(author in authors) {
//                    if(author.userBelong != user.id)throw UserForbidden("Author", author.id!!.toString())
//                }
//                append_authors.addAll(authors)
//            }
//            if(appendItem!=null&&"author" in appendItem)appends.put("author", append_authors)
//            //最后提交对user的更改，因为中途会用到对user的修改。
//            this.update(user)
//            val ret = ServiceSet(obj.obj, if(appends.isNotEmpty())appends else null)
//            ret
//        }
//    }
//
//    override fun update(obj: ServiceSet<Anime>, appendItem: Set<String>?): ServiceSet<Anime> {
//        return dao.dao<ServiceSet<Anime>> {
//            //获得User并检查
//            val user = this.query(User::class).where(Restrictions.eq("id", obj.obj.userBelong)).first()
//            user?:throw ModelWithPrimaryKeyNotFound("User", obj.obj.userBelong)
//            //append item
//            val appends = HashMap<String, Any?>()
//            //处理Series依赖。有New时将其替换为new，没有时检查原有的series的合法性。
//            if(obj.contains("seriesNew")) {//添加新的Series
//                val new_series = obj["seriesNew"] as Series
//                initializeUBModel(new_series, user, Series::class)
//                val seriesId = this.create(new_series) as Int
//                obj.obj.seriesId = seriesId
//                if(appendItem!=null&&"series" in appendItem)appends.put("series", new_series)
//            }else if(obj.obj.seriesId != null) {//手动提供Series
//                val local_series = this.query(Series::class).where(Restrictions.eq("id", obj.obj.seriesId)).first()
//                //检查是否存在
//                local_series ?: ModelWithPrimaryKeyNotFound("Series", obj.obj.seriesId!!.toString())
//                //检查Series的userbelong是否属于当前用户。
//                if(local_series!!.userBelong != user.id) throw UserForbidden("Series", local_series.id!!.toString())
//                if(appendItem!=null&&"series" in appendItem)appends.put("series", local_series)
//            }else{
//                if(appendItem!=null&&"series" in appendItem)appends.put("series", null)
//            }
//            //提交主要内容的更新并提取主键。
//            this.update(obj.obj)
//            val pkey = obj.obj.id!!
//            //处理Author依赖。
//            if(obj.contains("author")) {//需要先处理已存在内容。
//                val authorids = obj["author"] as List<Int> //所有待替换的author的id
//                //为了节约资源，在这里采取分部分处理策略。
//                //首先筛选所有author to anime中属于anime.id但是不包括于author ids的条目删除;
//                //然后从author ids中查找那些author属于user.id、author to anime(author.id, anime.id)项目不存在的，创建新的。
//                //那些没动的就不会修改。
//                val middles = this.query(AuthorToAnime::class).where(Restrictions.eq("animeId", pkey)).all()
//                val add_middle_ids = ArrayList(authorids)
//                val exists_middle_ids = ArrayList<Int>()
//                for(middle in middles) {//选取所有当前的中间件。
//                    //删除不在新表内的中间件。
//                    if(middle.authorId!! !in authorids)this.delete(middle)
//                    //上面备份了一个整个的author id表。下面如果一个中间件在新表内就把他移除，这样最后add middle ids里只剩需要新添加的了。
//                    else {
//                        exists_middle_ids.add(middle.authorId!!)
//                        add_middle_ids.removeIf { it == middle.authorId }
//                    }
//                }
//                //查询所有马上需要创建的authors。
//                val authors = this.query(Author::class).where(Restrictions_in("id", add_middle_ids)).all()
//                for(author in authors) {
//                    if(author.userBelong != user.id)throw UserForbidden("Author", author.id!!.toString())
//                    this.create(AuthorToAnime(authorId = author.id, animeId = pkey))
//                }
//            }
//            if(obj.contains("authorNew")) {//然后再处理新建的内容。
//                val authors = obj["authorNew"] as List<Author>
//                for(author in authors) {
//                    initializeUBModel(author, user, Author::class)
//                    val authorId = this.create(author) as Int
//                    this.create(AuthorToAnime(authorId = authorId, animeId = pkey))//由于author是many to many关系，依赖于anime的主键，所以在这之后。
//                }
//            }
//            if(appendItem!=null&&"author" in appendItem){
//                val appendAuthorMiddles = this.query(AuthorToAnime::class).where(Restrictions.eq("animeId", obj.obj.id)).all()
//                val idList = appendAuthorMiddles.map { it.authorId }
//                val appendAuthors = this.query(Author::class).where(Restrictions_in("id", idList)).all()
//                appends.put("author", appendAuthors)
//            }
//            val ret = ServiceSet(obj.obj, if(appends.isNotEmpty())appends else null)
//            ret
//        }
//    }
//
//    override fun delete(obj: ServiceSet<Anime>, appendItem: Set<String>?) {
//        dao.dao { this.delete(obj.obj) }
//    }
//
//    override fun queryList(feature: QueryFeature?, appendItem: Set<String>?): QueryAllStruct<ServiceSet<Anime>> {
//        return dao.dao<QueryAllStruct<ServiceSet<Anime>>> {
//            val qAll = this.query(Anime::class).feature(feature).qAll()
//            QueryAllStruct(qAll.content.map { q ->
//                val ret = ServiceSet(q)
//                if(appendItem!=null){
//                    if("author" in appendItem) {
//                        val appendAuthorMiddles = this.query(com.heerkirov.bangumi.model.AuthorToAnime::class).where(Restrictions.eq("animeId", q.id)).all()
//                        val idList = appendAuthorMiddles.map { it.authorId }
//                        val appendAuthors = this.query(com.heerkirov.bangumi.model.Author::class).where(Restrictions_in("id", idList)).all()
//                        ret.push("author", appendAuthors)
//                    }
//                    if("series" in appendItem) {
//                        val appendSeries = this.query(com.heerkirov.bangumi.model.Series::class).where(Restrictions.eq("id", q.seriesId)).first()
//                        ret.push("series", appendSeries)
//                    }
//                }
//                ret
//            }, qAll.index, qAll.count)
//        }
//    }
//
//    override fun queryAll(feature: QueryFeature?, appendItem: Set<String>?): List<ServiceSet<Anime>> {
//        return dao.dao<List<ServiceSet<Anime>>> {
//            val qAll = this.query(Anime::class).feature(feature).all()
//            qAll.map { q ->
//                val ret = ServiceSet(q)
//                if(appendItem!=null){
//                    if("author" in appendItem) {
//                        val appendAuthorMiddles = this.query(AuthorToAnime::class).where(Restrictions.eq("animeId", q.id)).all()
//                        val idList = appendAuthorMiddles.map { it.authorId }
//                        val appendAuthors = this.query(Author::class).where(Restrictions_in("id", idList)).all()
//                        ret.push("author", appendAuthors)
//                    }
//                    if("series" in appendItem) {
//                        val appendSeries = this.query(Series::class).where(Restrictions.eq("id", q.seriesId)).first()
//                        ret.push("series", appendSeries)
//                    }
//                }
//                ret
//            }
//        }
//    }
//
//    override fun queryGet(index: Int, feature: QueryFeature?, appendItem: Set<String>?): ServiceSet<Anime>? {
//        return dao.dao<ServiceSet<Anime>?> {
//            val q = this.query(Anime::class).feature(feature).get(index)
//            if(q != null){
//                val ret = ServiceSet(q)
//                if(appendItem!=null){
//                    if("author" in appendItem) {
//                        val appendAuthorMiddles = this.query(AuthorToAnime::class).where(Restrictions.eq("animeId", q.id)).all()
//                        val idList = appendAuthorMiddles.map { it.authorId }
//                        val appendAuthors = this.query(Author::class).where(Restrictions_in("id", idList)).all()
//                        ret.push("author", appendAuthors)
//                    }
//                    if("series" in appendItem) {
//                        val appendSeries = this.query(Series::class).where(Restrictions.eq("id", q.seriesId)).first()
//                        if(appendSeries!=null)ret.push("series", appendSeries)
//                    }
//                }
//                ret
//            }else null
//        }
//    }
//
//    override fun queryFirst(feature: QueryFeature?, appendItem: Set<String>?): ServiceSet<Anime>? {
//        return dao.dao<ServiceSet<Anime>?> {
//            val q = this.query(Anime::class).feature(feature).first()
//            if(q != null){
//                val ret = ServiceSet(q)
//                if(appendItem!=null){
//                    if("author" in appendItem) {
//                        val appendAuthorMiddles = this.query(AuthorToAnime::class).where(Restrictions.eq("animeId", q.id)).all()
//                        val idList = appendAuthorMiddles.map { it.authorId }
//                        val appendAuthors = this.query(Author::class).where(Restrictions_in("id", idList)).all()
//                        ret.push("author", appendAuthors)
//                    }
//                    if("series" in appendItem) {
//                        val appendSeries = this.query(Series::class).where(Restrictions.eq("id", q.seriesId)).first()
//                        ret.push("series", appendSeries)
//                    }
//                }
//                ret
//            }else null
//        }
//    }
//
//    override fun queryExists(feature: QueryFeature?): Boolean {
//        return dao.dao<Boolean> { this.query(Anime::class).feature(feature).exists() }
//    }
//
//}