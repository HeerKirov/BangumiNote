package com.heerkirov.bangumi.dao

import com.heerkirov.bangumi.model.base.ModelInterface
import com.heerkirov.bangumi.util.join
import org.hibernate.Criteria
import org.hibernate.HibernateException
import org.hibernate.Session
import org.hibernate.criterion.Criterion
import org.hibernate.criterion.Order
import org.hibernate.criterion.Projection
import org.hibernate.criterion.Projections
import java.io.Serializable
import kotlin.reflect.KClass

class DatabaseMiddleware(private val session: Session) {
    val isOpen: Boolean
        get() = is_open
    private var is_open: Boolean = true
    private val tx = session.beginTransaction()

    //通过直接传入模型进行数据库操作的部分函数。
    fun create(obj: ModelInterface): Serializable {
        //TODO 修正所有的查询操作。捕获HibernateException，并在抛出之前尝试关闭连接。
        return session.save(obj)
    }
    fun update(obj: ModelInterface){
        session.saveOrUpdate(obj)
    }
    fun delete(obj: ModelInterface){
        session.delete(obj)
    }
    //通过HQL进行数据库操作的部分函数。
    fun updateAuto(table: String): UpdateMiddleware = UpdateMiddleware(this, session, table, true)
    fun deleteAuto(table: String): DeleteMiddleware = DeleteMiddleware(this, session, table, true)
    fun<T> updateAuto(clazz: KClass<T>): UpdateMiddleware where T: ModelInterface = UpdateMiddleware(this, session, clazz.simpleName!!, true)
    fun<T> deleteAuto(clazz: KClass<T>): DeleteMiddleware where T: ModelInterface = DeleteMiddleware(this, session, clazz.simpleName!!, true)
    fun<T> update(clazz: KClass<T>): UpdateMiddleware where T: ModelInterface = UpdateMiddleware(this, session, clazz.simpleName!!)
    fun<T> delete(clazz: KClass<T>): DeleteMiddleware where T: ModelInterface = DeleteMiddleware(this, session, clazz.simpleName!!)

    //进行数据库查询操作。
    fun<T> queryAuto(clazz: KClass<T>): QueryMiddleware<T> where T : ModelInterface = QueryMiddleware(this, session, clazz, true)
    fun<T> query(clazz: KClass<T>): QueryMiddleware<T> where T : ModelInterface = QueryMiddleware(this, session, clazz)
    //提交和关闭连接。
    fun commitAndClose() {
        if(tx.isActive){
            try {
                tx.commit()
            }catch(ex: HibernateException) {
                tx.rollback()
                throw ex
            }
        }
        if(session.isOpen)session.close()
    }
    fun rollback() {
        tx.rollback()
    }

    //用于执行update函数链的内部类
    class UpdateMiddleware(private val db: DatabaseMiddleware, private val session: Session, private val table: String, private var cls: Boolean = false) {
        private var whereSentence: String = ""
        private val setMap: HashMap<String, Any> = HashMap()

        fun set(vararg sets: Pair<String, Any>): UpdateMiddleware {
            setMap.putAll(sets)
            return this
        }
        fun set(sets: Map<String, Any>): UpdateMiddleware {
            setMap.putAll(sets)
            return this
        }
        fun where(vararg orCondition: String): UpdateMiddleware {
            //where语句的内部条件以and进行连接，多个where以or进行连接。
            val single = "(${orCondition.asIterable().join(")AND(")})"
            whereSentence = if(whereSentence.isNotBlank())"($whereSentence)OR($single)" else single
            return this
        }
        fun commit(): Int {
            val hql = "UPDATE $table " +
                    if(setMap.isNotEmpty())
                        " SET ${setMap.asIterable().join(", "){"${it.key}: ${it.value}"}} " else " " +
                            whereSentence
            val query = session.createQuery(hql)
            val result = query.executeUpdate()
            if(cls)db.commitAndClose()
            return result
        }

        fun toBeClosed(): UpdateMiddleware {
            cls = true
            return this
        }

    }
    //用于执行delete函数链的内部类
    class DeleteMiddleware(private val db: DatabaseMiddleware, private val session: Session, private val table: String, private var cls: Boolean = false) {
        private var whereSentence: String = ""
        fun where(vararg orCondition: String): DeleteMiddleware {
            //where语句的内部条件以and进行连接，多个where以or进行连接。
            val single = "(${orCondition.asIterable().join(")AND(")})"
            whereSentence = if(whereSentence.isNotBlank())"($whereSentence)OR($single)" else single
            return this
        }
        fun commit(): Int {
            val hql = "DELETE FROM $table ${if(whereSentence.isNotBlank())"WHERE" else ""}$whereSentence"
            val query = session.createQuery(hql)
            val result = query.executeUpdate()
            if(cls)db.commitAndClose()
            return result
        }

        fun toBeClosed(): DeleteMiddleware {
            cls = true
            return this
        }
    }
    //用于执行query函数链的内部类
    class QueryMiddleware<out T>(private val db: DatabaseMiddleware, private val session: Session, private val clazz: KClass<T>, private var cls: Boolean = false) where T: ModelInterface {
        private var pageFirst: Int? = null
        private var pageCount: Int? = null
        private val whereList: ArrayList<Criterion> = arrayListOf()
        private val orderList: ArrayList<Order> = arrayListOf()
        private val innerWhereList: HashMap<String, ArrayList<Criterion>> = HashMap()
        private val innerOrderList: HashMap<String, ArrayList<Order>> = HashMap()
        private var projectList: Projection? = null
        fun where(condition: Criterion): QueryMiddleware<T> {
            whereList.add(condition)
            return this
        }
        fun where(inner: String, condition: Criterion): QueryMiddleware<T> {
            if(innerWhereList.containsKey(inner)) {
                val list = innerWhereList.remove(inner)!!
                list.add(condition)
                innerWhereList.put(inner, list)
            }else {
                innerWhereList.put(inner, arrayListOf(condition))
            }
            return this
        }
        fun order(condition: Order): QueryMiddleware<T> {
            orderList.add(condition)
            return this
        }
        fun order(inner: String, condition: Order): QueryMiddleware<T> {
            if(innerOrderList.containsKey(inner)) {
                val list = innerOrderList.remove(inner)!!
                list.add(condition)
                innerOrderList.put(inner, list)
            }else{
                innerOrderList.put(inner, arrayListOf(condition))
            }
            return this
        }
        fun page(first: Int, count: Int): QueryMiddleware<T> {
            pageFirst = first
            pageCount = count
            return this
        }
        fun project(content: Projection): QueryMiddleware<T> {
            projectList = content
            return this
        }
        fun feature(feature: QueryFeature? = null): QueryMiddleware<T> {
            if(feature!=null){
                if(feature.where!=null)whereList.addAll(feature.where!!)
                if(feature.order!=null)orderList.addAll(feature.order!!)
                if(feature.project!=null)projectList = feature.project
                if(feature.pageFirst!=null)pageFirst = feature.pageFirst
                if(feature.pageCount!=null)pageCount = feature.pageCount
                if(feature.innerWhere!=null)innerWhereList.putAll(feature.innerWhere!!)
                if(feature.innerOrder!=null)innerOrderList.putAll(feature.innerOrder!!)
            }
            return this
        }

        private fun Criteria.appendWhere(): Criteria {
            whereList.forEach { this.add(it) }
            return this
        }
        private fun Criteria.appendOrder(): Criteria {
            orderList.forEach { this.addOrder(it) }
            return this
        }
        private fun Criteria.appendProject(): Criteria {
            projectList?.let { this.setProjection(it) }
            return this
        }
        private fun Criteria.appendPage(first: Boolean = true, maxResult: Boolean = true): Criteria {
            if(first)pageFirst?.let{this.setFirstResult(it)}
            if(maxResult)pageCount?.let{this.setMaxResults(it)}
            return this
        }
        private fun Criteria.innerJoin(where: Boolean = true, order: Boolean = true): Criteria {
            val innerList = innerWhereList.keys.plus(innerOrderList.keys)
            var cr = this
            for(inner in innerList) {
                cr = cr.createCriteria(inner)
                if(where)innerWhereList[inner]?.forEach { cr.add(it) }
                if(order)innerOrderList[inner]?.forEach { cr.addOrder(it) }
            }
            return cr
        }

        fun qAll(): QueryAllStruct<T> {
            //这是一个为分页特别准备的查询函数。它除查询标准列表之外，还会查询本查询条件下的总条数，并返回首条index。
            val retContent: List<T>?
            val retIndex: Int = pageFirst?:0
            val retCount: Long?
            //首先不添加分页信息，得到总条目数目
            val cr1 = session.createCriteria(clazz.java).appendWhere().innerJoin(order = false).setProjection(Projections.rowCount())
            val ret1 = cr1.list()
            retCount = if(ret1.isNotEmpty())ret1[0] as Long else 0

            //然后添加分页信息，并得到数据
            val cr2 = session.createCriteria(clazz.java)
            cr2.appendWhere().appendOrder().innerJoin().appendProject().appendPage()
            val ret2 = cr2.list()
            retContent = ret2.map { it!!.let { it as T }  }

            if(cls)db.commitAndClose()
            return QueryAllStruct(retContent!!, retIndex, retCount!!)
        }
        fun all(): List<T> {
            val cr = session.createCriteria(clazz.java)
            cr.appendWhere().appendOrder().innerJoin().appendProject().appendPage()
            val ret = cr.list()
            if(cls)db.commitAndClose()
            return ret.map { it!!.let { it as T }  }
        }
        fun first(): T? {
            val cr = session.createCriteria(clazz.java)
            cr.appendWhere().appendOrder().innerJoin().appendProject()
            pageFirst?.let { cr.setFirstResult(it) }
            cr.setMaxResults(1)

            val ret = cr.list()
            if(cls)db.commitAndClose()
            if(ret.size>0){return ret.first()!!.let { it as T }}else{return null}

        }
        fun get(index: Int): T? {
            val cr = session.createCriteria(clazz.java)
            if(pageCount in 0..index+1) {
                return null
            }else{
                cr.appendWhere().appendOrder().innerJoin().appendProject()
                pageFirst?.let { cr.setFirstResult(it+index) }
                cr.setMaxResults(1)
                val ret = cr.list()
                if(cls)db.commitAndClose()
                if(ret.size>0){return ret[0]!!.let { it as T }}else{return null}
            }
        }
        fun count(): Long {
            val cr = session.createCriteria(clazz.java)
            cr.appendWhere().innerJoin(order = false).appendPage()
            cr.setProjection(Projections.rowCount())
            val ret = cr.list()
            if(cls)db.commitAndClose()
            return if(ret.isNotEmpty())ret[0] as Long else 0
        }
        fun exists(): Boolean {
            return count() > 0
        }

        fun toBeClosed(): QueryMiddleware<T> {
            cls = true
            return this
        }
    }
}
class QueryAllStruct<out T> (val content: List<T>, val index: Int, val count: Long)
//方便地把一组Query条件打包。
class QueryFeature(
        var where: ArrayList<Criterion>? = null,
        var order: ArrayList<Order>? = null,
        var innerWhere: HashMap<String, ArrayList<Criterion>>? = null,
        var innerOrder: HashMap<String, ArrayList<Order>>? = null,
        var project: Projection? = null,
        var pageFirst: Int? = null,
        var pageCount: Int? = null) {
    fun addWhere(c: Criterion): QueryFeature {
        if(where==null)where = ArrayList()
        where!!.add(c)
        return this
    }
    fun addOrder(c: Order): QueryFeature {
        if(order==null)order = ArrayList()
        order!!.add(c)
        return this
    }
}