package com.heerkirov.bangumi.service

import com.heerkirov.bangumi.dao.Dao
import com.heerkirov.bangumi.dao.DatabaseMiddleware
import com.heerkirov.bangumi.dao.QueryAllStruct
import com.heerkirov.bangumi.dao.QueryFeature
import com.heerkirov.bangumi.model.Bangumi
import com.heerkirov.bangumi.model.User
import com.heerkirov.bangumi.model.Diary
import com.heerkirov.bangumi.model.Episode
import com.heerkirov.converter.toDateTimeNumber
import com.heerkirov.converter.toNumber
import org.hibernate.criterion.Restrictions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.max

/**新·Diary Bangumi Episode 之间的交互：
 * 1. create diary时，name和totalEpisode从bangumi继承，且bangumi的watching设为True。
 * 2. update diary/bangumi时，为对方也更新name和totalEpisode属性。
 * 3. 任何时间 totalEpisode >= publishEpisode >= finishedEpisode。
 * 4. completed属性自动设置，在finishedEpisode == totalEpisode时有效。反之亦然。
 * 5. completed == True时，自动设置bangumi的finishedTime，而False时也会自动设为null。
 * 6. 如果bangumi有episode，在CREATE Diary/操作Episode时，会根据episode设定publishEpisode/finishedEpisode/Plan。
 * 7. UPDATE Diary的finishedEpisode时，自动为已存在的Episode设置finishedTime。
 * 8. 任何时间，plan的数量不能超过total-publish。
 */
@Service
class DiaryServiceImpl(@Autowired private val dao: Dao): DiaryService {
    companion object {
        fun analysisEpisodes(diary: Diary?, episodes: List<Episode>) {
            if(diary != null && episodes.isNotEmpty()) {
                val today = Calendar.getInstance()
                var maxPublishSerial = 0
                var maxFinishedSerial = 0
                val plan = ArrayList<String>()
                for (episode in episodes) {
                    episode.publishTime?.let {
                        if (it >= today) {//发布日期在今天之后，应当加入plan。
                            plan.add(it.toNumber().toString())
                        } else {//发布日期在今天之前，应当计入published统计。
                            maxPublishSerial = max(maxPublishSerial, episode.serial!!)
                        }
                    }
                    episode.finishedTime?.let {
                        if (it < today) {
                            maxFinishedSerial = max(maxFinishedSerial, episode.serial!!)
                        }
                    }
                }
                diary.publishPlan = plan
                diary.finishedEpisode = maxFinishedSerial
                diary.publishEpisode = maxPublishSerial
            }
        }
    }
    override fun create(obj: ServiceSet<Diary>, appendItem: Set<String>?): ServiceSet<Diary> {
        return dao.dao<ServiceSet<Diary>> {
            val user = this.query(User::class).where(Restrictions.eq("id", obj.obj.userBelong)).first()?:throw ModelWithPrimaryKeyNotFound("User", obj.obj.userBelong)
            obj.obj.userBelongId = user.incUid(Diary::class)
            //唯一检查
            if(obj.obj.bangumi != null && this.query(Diary::class).where(Restrictions.eq("bangumi", obj.obj.bangumi)).exists()) throw UniqueCheck("Diary")
            mappingTreat(obj.obj, "bangumi", Bangumi::class, user, notNull = true, allowNew = false)
            val bangumi = obj.obj.bangumi!!
            val diary = obj.obj
            //1
            diary.name = bangumi.name
            diary.totalEpisode = bangumi.playQuantity?:12
            //3
            if(diary.publishEpisode==null)diary.publishEpisode = 0
            if(diary.finishedEpisode==null)diary.finishedEpisode = 0
            if(diary.publishEpisode!! > diary.totalEpisode!!) diary.publishEpisode = diary.totalEpisode
            if(diary.finishedEpisode!! > diary.publishEpisode!!) diary.finishedEpisode = diary.publishEpisode
            //4
            diary.completed = (diary.finishedEpisode == diary.totalEpisode)
            //5
            if(diary.completed && bangumi.finishedTime == null) bangumi.finishedTime = Calendar.getInstance()
            else if(!diary.completed && bangumi.finishedTime != null) bangumi.finishedTime = null
            //6
            analysisEpisodes(diary, this.query(Episode::class).where(Restrictions.eq("bangumi_id", bangumi.id)).all())
            //8
            if(diary.publishPlan.size > diary.totalEpisode!! - diary.publishEpisode!!) {
                throw ServiceRuntimeException("Plan quantity cannot be more than 'total - publish'.")
            }

            this.update(user) //提交user的更改。
            this.create(obj.obj)
            ServiceSet(obj.obj)
        }
    }

    override fun update(obj: ServiceSet<Diary>, appendItem: Set<String>?): ServiceSet<Diary> {
        return dao.dao<ServiceSet<Diary>> {
            val diary = obj.obj
            val bangumi = diary.bangumi!!
            //2
            bangumi.name = diary.name
            bangumi.playQuantity = diary.totalEpisode
            //3
            if(diary.publishEpisode == null)diary.publishEpisode = 0
            if(diary.finishedEpisode == null)diary.finishedEpisode = 0
            if(diary.publishEpisode!! > diary.totalEpisode!!) diary.publishEpisode = diary.totalEpisode
            if(diary.finishedEpisode!! > diary.publishEpisode!!) diary.finishedEpisode = diary.publishEpisode
            //4
            diary.completed = (diary.finishedEpisode == diary.totalEpisode)
            //5
            if(diary.completed && bangumi.finishedTime == null) bangumi.finishedTime = Calendar.getInstance()
            else if(!diary.completed && bangumi.finishedTime != null) bangumi.finishedTime = null
            //7
            val episodes = this.query(Episode::class).where(Restrictions.eq("bangumi_id", bangumi.id)).all()
            if(episodes.isNotEmpty()) {
                for(episode in episodes) {
                    if(episode.finishedTime == null && episode.serial!! <= diary.finishedEpisode!!) {
                        episode.finishedTime = Calendar.getInstance()
                        this.update(episode)
                    }
                }
            }
            this.update(bangumi)
            this.update(diary)
            ServiceSet(obj.obj)
        }
    }

    override fun handleFinished(obj: Diary): Diary {
        //额外的函数。只用来为目标diary推进1位finished。
        return dao.dao<Diary> {
            val bangumi = obj.bangumi!!

            //3 & do it
            if(obj.publishEpisode == null)obj.publishEpisode = 0
            if(obj.finishedEpisode == null)obj.finishedEpisode = 0
            obj.finishedEpisode = obj.finishedEpisode!! + 1
            if(obj.publishEpisode!! > obj.totalEpisode!!) obj.publishEpisode = obj.totalEpisode
            if(obj.finishedEpisode!! > obj.publishEpisode!!) obj.finishedEpisode = obj.publishEpisode
            //4
            obj.completed = (obj.finishedEpisode == obj.totalEpisode)
            //5
            if(obj.completed && bangumi.finishedTime == null) {
                bangumi.finishedTime = Calendar.getInstance()
                this.update(bangumi)
            }
            else if(!obj.completed && bangumi.finishedTime != null) {
                bangumi.finishedTime = null
                this.update(bangumi)
            }
            //7
            val episodes = this.query(Episode::class).where(Restrictions.eq("bangumi_id", bangumi.id)).all()
            if(episodes.isNotEmpty()) {
                for(episode in episodes) {
                    if(episode.finishedTime == null && episode.serial!! <= obj.finishedEpisode!!) {
                        episode.finishedTime = Calendar.getInstance()
                        this.update(episode)
                    }
                }
            }
            this.update(obj)
            obj
        }
    }

    override fun delete(obj: ServiceSet<Diary>, appendItem: Set<String>?) {
        dao.dao { this.delete(obj.obj) }
    }

    override fun queryList(feature: QueryFeature?, appendItem: Set<String>?): QueryAllStruct<ServiceSet<Diary>> {
        return dao.dao<QueryAllStruct<ServiceSet<Diary>>> {
            val qAll = this.query(Diary::class).feature(feature).qAll()
            QueryAllStruct(qAll.content.map { ServiceSet(it) }, qAll.index, qAll.count)
        }
    }

    override fun queryAll(feature: QueryFeature?, appendItem: Set<String>?): List<ServiceSet<Diary>> {
        return dao.dao<List<ServiceSet<Diary>>> {
            val qAll = this.query(Diary::class).feature(feature).all()
            qAll.map { ServiceSet(it) }
        }
    }

    override fun queryGet(index: Int, feature: QueryFeature?, appendItem: Set<String>?): ServiceSet<Diary>? {
        return dao.dao<ServiceSet<Diary>?> {
            val q = this.query(Diary::class).feature(feature).get(index)
            if(q != null)ServiceSet(q)else null
        }
    }

    override fun queryFirst(feature: QueryFeature?, appendItem: Set<String>?): ServiceSet<Diary>? {
        return dao.dao<ServiceSet<Diary>?> {
            val q = this.query(Diary::class).feature(feature).first()
            if(q != null)ServiceSet(q)else null
        }
    }

    override fun queryExists(feature: QueryFeature?): Boolean {
        return dao.dao<Boolean> { this.query(Diary::class).feature(feature).exists() }
    }

    override fun analysisPlan(): List<MessageService.DiaryPublishInfo> {
        return dao.dao<List<MessageService.DiaryPublishInfo>> {
            val ret = ArrayList<MessageService.DiaryPublishInfo>()
            val diaries = this.query(Diary::class).all()
            val today = Calendar.getInstance()
            for(diary in diaries) {
                if(diary.publishPlan.isNotEmpty()) {
                    var count = 0
                    val array = ArrayList<String>()
                    for(d in diary.publishPlan) {
                        if(d.toDateTimeNumber().toCalendar() <= today) {//一个被执行的计划时间
                            count++
                        }else{//没有被执行
                            array.add(d)
                        }
                    }
                    if(count > 0) {
                        ret.add(MessageService.DiaryPublishInfo(diary, diary.publishEpisode!!, diary.publishEpisode!! + count))
                        diary.publishPlan = array
                        diary.publishEpisode = diary.publishEpisode!! + count
                        if(diary.publishEpisode!! > diary.totalEpisode!!)diary.publishEpisode = diary.totalEpisode
                        this.update(diary)
                    }
                }
            }
            ret
        }
    }
}