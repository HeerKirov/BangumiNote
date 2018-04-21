package com.heerkirov.bangumi.service

import com.heerkirov.bangumi.dao.Dao
import com.heerkirov.bangumi.model.Message
import com.heerkirov.bangumi.model.User
import com.sun.org.apache.xpath.internal.operations.Bool
import org.hibernate.criterion.Order
import org.hibernate.criterion.Restrictions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.util.*
import kotlin.collections.HashSet

@Component
class MessageServiceImpl(@Autowired private val dao: Dao): MessageService {
    override fun existAnyMessage(user: User): Boolean {
        return dao.dao<Boolean>{
            this.query(Message::class).where(Restrictions.eq("userId", user.id)).where(Restrictions.eq("haveRead", false)).exists()
        }
    }

    override fun unreadAndSetMessages(user: User): List<Message> {
        return dao.dao<List<Message>> {
            val messages = this.query(Message::class).where(Restrictions.eq("userId", user.id)).where(Restrictions.eq("haveRead", false)).order(Order.desc("createTime")).all()
            for(message in messages) {
                message.haveRead = true
                this.update(message)
            }
            messages
        }
    }

    override fun publishGeneral(lists: List<MessageService.GeneralInfo>): List<Message> {
        return dao.dao<List<Message>> {
            val ret = ArrayList<Message>()
            val users = HashSet<User>()
            for(item in lists) {
                val msg = Message(
                        userId = item.user.id,
                        createTime = Calendar.getInstance(),
                        updateTime = Calendar.getInstance(),
                        uid = item.user.incUid(Message::class),
                        type = "general",
                        content = hashMapOf("title" to item.title, "content" to item.content)
                )
                this.create(msg)
                ret.add(msg)
                users.add(item.user)
            }
            for(user in users) this.update(user)
            ret
        }
    }

    override fun publishDiaryPublish(lists: List<MessageService.DiaryPublishInfo>): List<Message> {
        return dao.dao<List<Message>> {
            val ret = ArrayList<Message>()
            val users = HashSet<User>()
            for(item in lists) {
                val user = this.query(User::class).where(Restrictions.eq("id", item.diary.userId)).first()!!
                val msg = Message(
                        userId = user.id,
                        createTime = Calendar.getInstance(),
                        updateTime = Calendar.getInstance(),
                        uid = user.incUid(Message::class),
                        type = "diary_publish",
                        content = hashMapOf(
                                "bangumi_id" to item.diary.bangumi!!.id,
                                "diary_id" to item.diary.id,
                                "name" to item.diary.name,
                                "old_count" to item.oldCount,
                                "new_count" to item.newCount
                        )
                )
                this.create(msg)
                ret.add(msg)
                users.add(user)
            }
            for(user in users) this.update(user)
            ret
        }
    }
}