package com.heerkirov.bangumi.model

import com.heerkirov.bangumi.model.base.DTModel
import com.heerkirov.bangumi.model.base.JsonType
import com.heerkirov.bangumi.model.base.Model
import com.heerkirov.bangumi.model.base.UBModel
import org.hibernate.annotations.Type
import org.hibernate.annotations.TypeDef
import java.util.*
import javax.persistence.*
import kotlin.collections.HashMap
import kotlin.reflect.KClass

@Entity
@Table(name = "public.user")
@TypeDef(name = "json", typeClass = JsonType::class)
class User(
        @Id @Column(name = "id", length = 16, nullable = false) var id: String = "",
        @Column(name = "name", length = 16, nullable = false) var name: String = "",
        @Column(name = "password", length = 128, nullable = false) var password: String = "",
        @Column(name = "is_admin", nullable = false) var admin: Boolean = false,

        @Column(name = "create_time", nullable = false)var createTime: Calendar? = null,
        @Column(name = "update_time", nullable = false)var updateTime: Calendar? = null,
        @Column(name = "last_login", nullable = false)var lastLogin: Calendar? = null,

        @Column(name = "uid", nullable = false)@Type(type = "json")var uid: HashMap<String, Any?> = hashMapOf()
): DTModel() {
    fun<T> incUid(model: KClass<T>): Int where T: UBModel {
        //该函数将提取目标uid，并自动在模型内将uid后推一位。
        val name = model.simpleName!!
        if(!uid.containsKey(name)) { uid.put(name, 1) } //初值为1
        val ret = uid[name]!! as Int
        uid.put(name, ret + 1) //每次后推1
        return ret
    }
}