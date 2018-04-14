package com.heerkirov.bangumi.controller.base

import com.alibaba.fastjson.JSONObject
import com.heerkirov.converter.ConvertError
import com.heerkirov.bangumi.controller.converter.ModelConverter
import com.heerkirov.bangumi.controller.converter.ModelListConverter
import com.heerkirov.bangumi.controller.filter.Filter
import com.heerkirov.bangumi.dao.QueryAllStruct
import com.heerkirov.bangumi.dao.QueryFeature
import com.heerkirov.bangumi.model.base.DTModel
import com.heerkirov.bangumi.model.base.ModelInterface
import com.heerkirov.bangumi.model.base.UBModel
import com.heerkirov.bangumi.service.RestfulService
import com.heerkirov.bangumi.service.Security
import com.heerkirov.bangumi.service.ServiceRuntimeException
import com.heerkirov.bangumi.service.ServiceSet
import org.hibernate.criterion.Restrictions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.ResponseStatus
import java.util.*
import javax.servlet.http.HttpServletRequest
import kotlin.reflect.KClass

/*综合了大多数REST API所需功能的处理器。
此处理器已经重写成对接当前RestService接口的、对接ServiceSet的处理器。
 */
abstract class RestfulController<T, in KEY>(private val clazz: KClass<T>) : ApiController() where T: ModelInterface, KEY: Any {
    @Autowired private val security: Security? = null
    @Autowired private val request: HttpServletRequest? = null
    override fun security(): Security = security!!
    override fun request(): HttpServletRequest = request!!

    //重写该字段以传入修订的允许执行的rest方法。默认是全部允许。
    protected open val methods: Set<RestfulMethod> = emptySet()

    //重写该字段以传入自定义的用户认证条件。默认需要登录认证。
    protected open val permission: SecurityParam = auth(true)

    //重写该字段以传入该视图的查询集。
    protected abstract val service: RestfulService<T>
    //重写该字段以传入该视图的序列化转换器。
    protected abstract val converter: ModelConverter<T>
    //重写该字段以传入该视图的自定义过滤器，在没有传入时使用一个空的过滤器。
    protected open val filter: Filter = Filter()
    //重写该字段以自定义单项查询中，用作查询主键的键名。
    protected open val lookup: String = "id"

    //重写该方法以返回自定义的Query限制器。
    protected open fun serviceQueryAll(service: RestfulService<T>, feature: QueryFeature? = null, appendItem: Set<String>? = null): QueryAllStruct<ServiceSet<T>> {
        return service.queryList(feature, appendItem)
    }
    protected open fun serviceQueryFirst(service: RestfulService<T>, feature: QueryFeature? = null, appendItem: Set<String>? = null): ServiceSet<T>? {
        return service.queryFirst(feature, appendItem)
    }
    //重写该字段以使用自定义的列表化转换器。
    protected open val listConverter: ModelListConverter<T> by lazy { ModelListConverter(clazz, converter = converter) }

    //重写该方法以自定义创建新model时对model施加的操作。
    protected open fun modelNew(map: JSONObject): ServiceSet<T> {
        return converter.serviceNew(map)
    }
    //重写该方法以自定义修改model时对model施加的操作。
    protected open fun modelUpdate(map: JSONObject, goal: T): ServiceSet<T> {
        return converter.serviceUpdate(map, goal)
    }
    //重写该方法以自定义partial修改model时对model施加的操作。
    protected open fun modelPartialUpdate(map: JSONObject, goal: T): ServiceSet<T> {
        return converter.servicePartialUpdate(map, goal)
    }
    //重写该方法以自定义删除model前对model施加的操作。
    protected open fun modelDelete(goal: T) {
        service.delete(ServiceSet(goal))
    }

    //编写自定义的http请求时可以直接返回该方法的结果.
    protected open fun requestList() = view(permission) {
        //获得过滤结果
        val filterResult = filter.filterParameters(request().parameterMap as Map<String, Any?>)
        //获得查询结果
        val models = serviceQueryAll(service, filterResult, converter.serviceParseSource)
        //获得将查询结果转换为可json化的内容
        val resultContent = try { //分页
             listConverter.serviceParse(models.content)
        }catch(e: ConvertError){//需要捕获由converter引发的错误并转换为bad request状态。
            throw BadRequestException(e.message!!)
        }
        mapOf("content" to resultContent, "count" to models.count, "index" to models.index)
    }
    protected open fun requestCreate() = view(permission) {
        val contentBody = try {contentBodyObject()!!}catch(e: NullPointerException) {
            throw BadRequestException("Information format is wrong.", HttpKeyword.INFORMATION_FORMAT_WRONG)
        }
        try{
            val model: ServiceSet<T> = modelNew(contentBody)//通过converter产生obj和附加信息
            val modelResult = service.create(model, converter.serviceParseSource)//传递给Service层执行操作，并获得返回的可以用来回执的信息。
            converter.serviceParse(modelResult)//构造回执信息。
        }catch(e: ConvertError){
            throw BadRequestException(e.message!!)
        }catch(e: ServiceRuntimeException){
            throw BadRequestException(e.message!!)
        }
    }
    protected open fun requestRetrieve(id: KEY) = view(permission) {
        val model = serviceQueryFirst(service, QueryFeature().addWhere(Restrictions.eq(lookup, id)), converter.serviceParseSource)//获得ServiceSet，obj和附加信息。
        if(model!=null){
            try {
                converter.serviceParse(model)//通过converter将其转换为json
            }catch(e: ConvertError){
                throw BadRequestException(e.message!!)
            }
        }else{
            throw NotFoundException()
        }
    }
    protected open fun requestUpdate(@PathVariable id: KEY) = view(permission) {
        val contentBody = try {contentBodyObject()!!}catch(e: NullPointerException) {
            throw BadRequestException("Information format is wrong.", HttpKeyword.INFORMATION_FORMAT_WRONG)
        }
        val model = serviceQueryFirst(service, QueryFeature().addWhere(Restrictions.eq(lookup, id)))//获得ServiceSet，obj和附加信息。这里不需要附加信息，因为后面提交时还会获取。
        if(model!=null){
            val updateModel = try {//通过converter更新obj并获得提交的附加信息
                modelUpdate(contentBody, model.obj)
            }catch(e: ConvertError){
                throw BadRequestException(e.message!!)
            }
            val modelResult = try {//向service层提交附加信息
                service.update(updateModel, converter.serviceParseSource)
            }catch(e: ServiceRuntimeException){
                throw BadRequestException(e.message!!)
            }
            converter.serviceParse(modelResult)
        }else{
            throw NotFoundException()
        }
    }
    protected open fun requestPartialUpdate(@PathVariable id: KEY) = view(permission) {
        val contentBody = try {contentBodyObject()!!}catch(e: NullPointerException) {
            throw BadRequestException("Information format is wrong.", HttpKeyword.INFORMATION_FORMAT_WRONG)
        }
        val model = serviceQueryFirst(service, QueryFeature().addWhere(Restrictions.eq(lookup, id)), converter.serviceParseSource)
        if(model!=null){
            val updateModel = try {
                modelPartialUpdate(contentBody, model.obj)
            }catch(e: ConvertError){
                throw BadRequestException(e.message!!)
            }
            val modelResult = try {
                service.update(updateModel, converter.serviceParseSource)
            }catch(e: ServiceRuntimeException){
                throw BadRequestException(e.message!!)
            }
            converter.serviceParse(modelResult)
        }else{
            throw NotFoundException()
        }
    }
    protected open fun requestDelete(@PathVariable id: KEY) = view(permission) {
        val model = serviceQueryFirst(service, QueryFeature().addWhere(Restrictions.eq(lookup, id)))
        if(model!=null){
            modelDelete(model.obj)
            emptyMap<String, Any?>()
        }else{
            throw NotFoundException()
        }
    }

    private fun<T> checkMethod(method: RestfulMethod, action: ()->T): T {
        if(methods.isEmpty()||methods.contains(method))return action()
        else throw MethodNotAllowedException()
    }

    @RequestMapping(method = [RequestMethod.GET])fun list() = checkMethod(RestfulMethod.LIST) {requestList()}
    @RequestMapping(method = [RequestMethod.POST])@ResponseStatus(HttpStatus.CREATED)fun create() = checkMethod(RestfulMethod.CREATE) {requestCreate()}
    @RequestMapping("/{id}", method = [RequestMethod.GET])fun retrieve(@PathVariable id: KEY) = checkMethod(RestfulMethod.RETRIEVE) {requestRetrieve(id)}
    @RequestMapping("/{id}", method = [RequestMethod.PUT])fun update(@PathVariable id: KEY) = checkMethod(RestfulMethod.UPDATE) {requestUpdate(id)}
    @RequestMapping("/{id}", method = [RequestMethod.PATCH])fun partialUpdate(@PathVariable id: KEY) = checkMethod(RestfulMethod.PARTIAL_UPDATE) {requestPartialUpdate(id)}
    @RequestMapping("/{id}", method = [RequestMethod.DELETE])@ResponseStatus(HttpStatus.NO_CONTENT)fun delete(@PathVariable id: KEY) = checkMethod(RestfulMethod.DELETE) {requestDelete(id)}

}

enum class RestfulMethod {
    LIST, CREATE, RETRIEVE, UPDATE, PARTIAL_UPDATE, DELETE
}

abstract class DateFieldRestfulController<T, in KEY>(clazz: KClass<T>) : RestfulController<T, KEY>(clazz)where T: DTModel, KEY: Any{
    override fun modelNew(map: JSONObject): ServiceSet<T> {
        return super.modelNew(map).also { it.obj.createFieldTime = Calendar.getInstance() }.also { it.obj.updateFieldTime = Calendar.getInstance() }
    }

    override fun modelUpdate(map: JSONObject, goal: T): ServiceSet<T> {
        return super.modelUpdate(map, goal).also { it.obj.updateFieldTime = Calendar.getInstance() }
    }

    override fun modelPartialUpdate(map: JSONObject, goal: T): ServiceSet<T> {
        return super.modelPartialUpdate(map, goal).also { it.obj.updateFieldTime = Calendar.getInstance() }
    }
}

abstract class UserBelongRestfulController<T, in KEY>(clazz: KClass<T>) : DateFieldRestfulController<T, KEY>(clazz)where T: UBModel, KEY: Any{
    override fun serviceQueryAll(service: RestfulService<T>, feature: QueryFeature?, appendItem: Set<String>?): QueryAllStruct<ServiceSet<T>> {
        val f = feature?:QueryFeature()
        security().currentUser()?.let { user -> f.addWhere(Restrictions.eq("userId", user.id)) }
        return super.serviceQueryAll(service, f, appendItem)
    }

    override fun serviceQueryFirst(service: RestfulService<T>, feature: QueryFeature?, appendItem: Set<String>?): ServiceSet<T>? {
        val f = feature?:QueryFeature()
        security().currentUser()?.let { user -> f.addWhere(Restrictions.eq("userId", user.id)) }
        return super.serviceQueryFirst(service, f, appendItem)
    }

    override fun modelNew(map: JSONObject): ServiceSet<T> {
        return super.modelNew(map).also { ret -> security().currentUser()?.let { user -> ret.obj.userBelong = user.id } }
    }
}
