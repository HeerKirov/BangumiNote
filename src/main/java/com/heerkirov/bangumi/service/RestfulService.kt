package com.heerkirov.bangumi.service

import com.heerkirov.bangumi.dao.DatabaseMiddleware
import com.heerkirov.bangumi.dao.QueryAllStruct
import com.heerkirov.bangumi.dao.QueryFeature
import com.heerkirov.bangumi.model.User
import com.heerkirov.bangumi.model.base.ModelInterface
import com.heerkirov.bangumi.model.base.UBModel
import org.hibernate.criterion.Restrictions
import java.util.*
import kotlin.collections.HashSet
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance

/**符合一整套REST规范的Service层基础接口。
 * 它有几个公用参数：
 *  1. set.obj 总是表示要提交的主要数据。
 *  2. set.content 附加的提交数据。
 *  3. appendItem 期待返回的内容中附带的附加数据。
 */
/** 所有通过映射来处理的field都要特殊处理。
 * 1. 传入一个拥有id的model，会查找这个id并赋值。
 * 2. 传入一个没有id的model，会视作创建新model。
 * */
interface RestfulService<T> where T: ModelInterface {
    fun create(obj: ServiceSet<T>, appendItem: Set<String>? = null): ServiceSet<T>
    fun update(obj: ServiceSet<T>, appendItem: Set<String>? = null): ServiceSet<T>
    fun delete(obj: ServiceSet<T>, appendItem: Set<String>? = null)
    fun queryFirst(feature: QueryFeature? = null, appendItem: Set<String>? = null): ServiceSet<T>?
    fun queryGet(index: Int, feature: QueryFeature? = null, appendItem: Set<String>? = null): ServiceSet<T>?
    fun queryAll(feature: QueryFeature? = null, appendItem: Set<String>? = null): List<ServiceSet<T>>
    fun queryList(feature: QueryFeature? = null, appendItem: Set<String>? = null): QueryAllStruct<ServiceSet<T>>
    fun queryExists(feature: QueryFeature? = null): Boolean
    /**这个函数会处理UBModel的子类型的初始化问题。
     * 1. 赋予其userBelong，并追加uid
     * 2. 初始化createTime和updateTime。
     */
    fun<T: UBModel> initializeUBModel(obj: T, user: User, incClazz: KClass<T>): T {
        obj.userBelong = user.id
        obj.userBelongId = user.incUid(incClazz)
        obj.createFieldTime = Calendar.getInstance()
        obj.updateFieldTime = Calendar.getInstance()
        return obj
    }
    /**这个函数会按标准流程处理一个ManyToOne的映射字段。
     * 1. 如果字段值为null，按照给出的notNull属性决定是否报错。
     * 2. 如果字段值有id，就视作添加已存在的model，并检查是否存在，且user所属是否合理。
     * 3. 如果没有id，视作新添加的model，按照常规流程赋予初始化，并保存。
     */
    fun<T: UBModel> DatabaseMiddleware.mappingTreat(obj: ModelInterface, fieldName: String, clazz: KClass<T>, user: User, fieldPrimaryKey: String = "id", notNull: Boolean = false, allowNew: Boolean = true) {
        val origin = obj.get<T>(fieldName)
        if(origin != null) {
            val id = origin.get<Int>(fieldPrimaryKey)
            if(id != null) {
                val model = this.query(clazz).where(Restrictions.eq(fieldPrimaryKey, id)).first()
                if(model != null) {
                    if(model.userBelong == user.id)obj.set(fieldName, model)
                    else throw UserForbidden(clazz.simpleName!!, id.toString())
                }
                else throw ModelWithPrimaryKeyNotFound(clazz.simpleName!!, id.toString())
            }else if(allowNew) {
                initializeUBModel(origin, user, clazz)
                this.create(origin)
            }else{
                throw NotAllowedNewModel(clazz.simpleName!!, fieldName)
            }
        }else if(notNull) throw NullValueError(fieldName)
    }
    /**这个函数会按照标准流程处理一个ManyToMany映射字段。
     * 首先将字段按照空列表保存一次，防止产生级联BUG。
     * 对于Set列表内的每一个origin model：
     * 1. 如果字段值有id，就视作已经存在的model，查找这些id并依次检查存在性和user所属，然后加入Set。
     * 2. 如果字段值没有id，就视作新建的model，赋予初始化，然后加入Set。
     */
    fun<T: UBModel> DatabaseMiddleware.mappingSetTreat(obj: ModelInterface, fieldName: String, clazz: KClass<T>, user: User, fieldPrimaryKey: String = "id", objPrimaryKey: String = "id") {
        val originList = obj.get<Set<T>>(fieldName)
        obj.set(fieldName, HashSet<T>())
        val objId = obj.get<Int>(objPrimaryKey)
        if(objId != null) this.update(obj) //只有已经存在的obj才会被提前刷新。
        val newList = HashSet<T>()
        if(originList != null) {
            for(origin in originList) {
                val id = origin.get<Int>(fieldPrimaryKey)
                if(id != null) {//已经存在的
                    val model = this.query(clazz).where(Restrictions.eq(fieldPrimaryKey, id)).first()
                    if(model != null) {
                        if(model.userBelong == user.id)newList.add(model)
                        else throw UserForbidden(clazz.simpleName!!, id.toString())
                    }else throw ModelWithPrimaryKeyNotFound(clazz.simpleName!!, id.toString())
                }else {//需要新建的
                    initializeUBModel(origin, user, clazz)
                    this.create(origin)
                    newList.add(origin)
                }
            }
        }
        obj.set(fieldName, newList)
    }

    /**这个函数会按照特殊流程处理一个ManyToMany映射字段。
     * 首先，收集所有存在的id构成idlist。查找Middle中所有属于obj.fieldName但是不属于idlist的，删除。
     * 然后，查找Middle中所有属于obj.fieldName且属于idlist的，这是已经存在的部分，不动。
     * 然后，排除上面存在的部分，idlist中剩下的全部添加新的Middle。
     * 最后，收集所有id不存在的，新建并创建Middle。
     */
    fun<T: UBModel, MID: ModelInterface> DatabaseMiddleware.mappingManyTreat(obj: ModelInterface, fieldName: String, clazz: KClass<T>, user: User,
                                                        middleClazz: KClass<MID>, orderKey: String, itemKey: String, objPrimaryKey: String = "id", fieldPrimaryKey: String = "id") {
        val originList = obj.get<Set<T>>(fieldName)
        if(originList!=null) {
            //抓取obj的id。
            val objId = obj.get<Int>(objPrimaryKey)
            //收集所有存在的id，表示当前存在的、希望添加进fieldName的model。
            val idList = originList.filter { it.get<Int>(fieldPrimaryKey) != null }.map { it.get<Int>(fieldPrimaryKey) }.toHashSet()
            //查询中间件，查询所有与当前obj相关的。
            val middles = this.query(middleClazz).where(Restrictions.eq(orderKey, objId)).all()
            for(middle in middles) {
                val itemId = middle.get<Int>(itemKey)
                //如果中间件的itemKey不在idList中，它需要被删除。
                if(itemId !in idList) this.delete(middle)
                //如果在，它需要被保留。同时从idList中移除此id。
                else idList.remove(itemId)
            }
            //最后idList留下的是需要新建中间件的。
            for(id in idList) {
                val newMiddle = middleClazz.createInstance()
                newMiddle.set(orderKey, objId)
                newMiddle.set(itemKey, id)
                this.create(newMiddle)
            }
            //然后筛选那些没有id的，全部新建并新建中间件。
            val newItems = originList.filter { it.get<Int>(fieldPrimaryKey) == null }
            for(item in newItems) {
                initializeUBModel(item, user, clazz)
                this.create(item)
                val newMiddle = middleClazz.createInstance()
                newMiddle.set(orderKey, objId)
                newMiddle.set(itemKey, item.get<Int>(fieldPrimaryKey))
                this.create(newMiddle)
            }
        }else{
            obj.set(fieldName, HashSet<T>())
        }
    }
}

//一个用于Service层返回数据的组件，它在返回目标model(s)的同时，也使Service层能根据请求方地要求返回一些特定的附带组件，比如其他model。
class ServiceSet<out T>(val obj: T, private var append: HashMap<String, Any?>? = null) where T: Any {
    operator fun get(key: String): Any? {
        return append?.get(key)
    }
    fun contains(key: String): Boolean {
        return append?.containsKey(key)?:false
    }
    fun push(key: String, value: Any?): ServiceSet<T> {
        if(append==null)append = HashMap()
        append!!.put(key, value)
        return this
    }
    operator fun component1(): T = obj
    operator fun component2(): HashMap<String, Any?>? = append
}
