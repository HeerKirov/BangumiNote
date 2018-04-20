package com.heerkirov.bangumi.model

import com.heerkirov.bangumi.model.base.JsonType
import com.heerkirov.bangumi.model.base.UBModel
import org.hibernate.annotations.Type
import org.hibernate.annotations.TypeDef
import java.util.*
import javax.persistence.*
import kotlin.collections.HashMap

/**Message规则：
 * type： 信息类型。
 * content： 信息内容格式。
 * 目前可用：
 * general: {   //一条泛用消息。
 *      title: String,  //标题
 *      content: String //内容
 * }
 * diary_publish: {     //一条来自diary的更新消息。
 *      bangumi_id: Int, //该diary的bangumi的id
 *      diary_id: Int,  //该diary的id
 *      name: String,   //该diary的name
 *      old_count: Int, //在更新之前的publishEpisode
 *      new_count: Int  //更新之后的。(old, new]区间内的数值是本次更新的集数。
 * }
 */
@Entity
@Table(name = "public.message")
@TypeDef(name = "json", typeClass = JsonType::class)
class Message(
        @Id @GeneratedValue(strategy = GenerationType.SEQUENCE, generator="MESSAGE_ID_SEQ")
        @SequenceGenerator(name="MESSAGE_ID_SEQ", sequenceName="message_id_seq", allocationSize = 1)
        @Column(name = "id")var id: Int? = null,
        @Column(name = "uid", nullable = false)var uid: Int? = null,

        @Column(name = "type", nullable = false)var type: String = "",
        @Column(name = "content", nullable = false)@Type(type = "json")var content: HashMap<String, Any?> = HashMap(),

        @Column(name = "have_read", nullable = false)var haveRead: Boolean = false,

        @Column(name = "user_id", nullable = false) var userId: String? = null,
        @Column(name = "create_time", nullable = false)var createTime: Calendar? = null,
        @Column(name = "update_time", nullable = false)var updateTime: Calendar? = null
): UBModel()