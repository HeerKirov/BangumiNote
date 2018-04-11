package com.heerkirov.bangumi.model.base

import com.heerkirov.bangumi.model.User
import com.heerkirov.bangumi.util.getDeclaredMethodByName
import com.heerkirov.bangumi.util.toFirstUppercase
import java.util.*
import kotlin.collections.HashMap
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.safeCast

interface ModelInterface {
    //实现该方法，使model可以借助map进行构造。
    fun new(map: Map<String, Any?>)
    //实现该方法，使model可以借助map进行内容更新。
    fun update(map: Map<String, Any?>)
    //实现该方法，使model能够在指定条件下输出它部分内容的map信息。
    fun toMap(vararg params: String): Map<String, Any?>

    //实现该方法，以完成model在借助map信息构造时完成对map的内容的合法性检查。
    fun validateNew(map: Map<String, Any?>): Map<String, Any?>
    //实现该方法，以完成mode在借助map进行部分内容更新时完成对map内容的合法性检查。
    fun validateUpdate(map: Map<String, Any?>): Map<String, Any?>

    fun<T> get(fieldName: String): T?
    fun<T> set(fieldName: String, value: T?)
}

interface UserBelongInterface : ModelInterface {
    var userBelong: User
    var userBelongId: Int
}
interface DateTimeInterface : ModelInterface {
    var createFieldTime: Calendar
    var updateFieldTime: Calendar
}
//提供一个model interface的默认实现。该实现基于反射进行默认实现。
abstract class Model : ModelInterface {
    override fun new(map: Map<String, Any?>) {
        map.map { (k, v) ->
            try {
//                val field = this::class.java.getDeclaredField(k)
//                field.set(this, v)
                val field = this::class.java.getDeclaredMethodByName("set"+k.toFirstUppercase())
                field.invoke(this, v)
            }catch(e: NoSuchMethodException){
                throw ModelException("No such field: $k")
            }
        }
    }

    override fun update(map: Map<String, Any?>) {
        new(map)
    }

    override fun toMap(vararg params: String): Map<String, Any?> {
        val ret = HashMap<String, Any?>()
        this::class.memberProperties.map { i ->
            if(params.isEmpty()||i.name in params)ret.put(i.name, i.call(this))
        }
        return ret
    }

    override fun validateNew(map: Map<String, Any?>): Map<String, Any?> {
        return map
    }

    override fun validateUpdate(map: Map<String, Any?>): Map<String, Any?> = validateNew(map)

    override fun <T> get(fieldName: String): T? {
        for(property in this::class.memberProperties) {
            if(property.name == fieldName) {
                return property.call(this) as T?
            }
        }
        throw NoSuchFieldException("${this::class.simpleName}.$fieldName")
    }

    override fun <T> set(fieldName: String, value: T?) {
        val property = this::class.java.getDeclaredMethodByName("set" + fieldName.toFirstUppercase())
        property.invoke(this, value)
    }
}
//提供一个datetime interface的默认实现。
abstract class DTModel : Model(), DateTimeInterface {
    private val setCreateMethod by lazy {this::class.java.getDeclaredMethod("setCreateTime", Calendar::class.java)}
    private val getCreateMethod by lazy {this::class.java.getDeclaredMethod("getCreateTime")}
    private val setUpdateMethod by lazy {this::class.java.getDeclaredMethod("setUpdateTime", Calendar::class.java)}
    private val getUpdateMethod by lazy {this::class.java.getDeclaredMethod("getUpdateTime")}
    override var createFieldTime: Calendar
        get() = Calendar::class.safeCast(getCreateMethod.invoke(this))!!
        set(value) {setCreateMethod.invoke(this, value)}
    override var updateFieldTime: Calendar
        get() = Calendar::class.safeCast(getUpdateMethod.invoke(this))!!
        set(value) {setUpdateMethod.invoke(this, value)}
}
//提供一个实现了UserBelong接口的默认实现。它通过反射查找默认的userId字段。
abstract class UBModel : DTModel(), UserBelongInterface {
    private val setMethod by lazy { this::class.java.getDeclaredMethod("setUser", User::class.java) }
    private val getMethod by lazy { this::class.java.getDeclaredMethod("getUser") }
    private val setUMethod by lazy {
        //巨坑：kt的func(Int)翻译为java时是func(java.lang.Integer)，但是Int::class.java翻译过去是int。
        this::class.java.getDeclaredMethod("setUid", java.lang.Integer::class.java)
    }
    private val getUMethod by lazy { this::class.java.getDeclaredMethod("getUid") }
    override var userBelong: User
        get() = User::class.safeCast(getMethod.invoke(this))!!
        set(value) {setMethod.invoke(this, value)}
    override var userBelongId: Int
        get() = Int::class.safeCast(getUMethod.invoke(this))!!
        set(value) {setUMethod.invoke(this, value)}
}



class ModelException(message: String): Exception(message)