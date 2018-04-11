package com.heerkirov.bangumi.controller.filter

import com.heerkirov.bangumi.dao.DatabaseMiddleware
import com.heerkirov.bangumi.dao.QueryFeature
import com.heerkirov.bangumi.model.base.ModelInterface
import org.hibernate.criterion.Criterion
import org.hibernate.criterion.Order
import org.hibernate.criterion.Restrictions
import org.jetbrains.annotations.Mutable
import kotlin.reflect.KClass

/*用于查询的参数过滤器。功能：
    1. 在query时，提供泛搜索功能。提供一个关键字，自动在指定的字段列表中进行OR LIKE模式匹配。
    2. 在query时，提供精确筛选功能。根据提供的关键字，对每种不同的字段进行指定类型的精准匹配。
    3. 在query时，提供排序功能。提供一个带ASC/DESC的字段列表，根据列表进行优先级排序。
    4. 在返回数组结果时，套用内容，自动根据参数进行分页。分页参数有两个，pageFirst和pageMaxResult。
*/
class Filter(private val searchMap: Array<String> = arrayOf(),
             private val orderMap: Array<OrderField> = arrayOf(),
             private val filterMap: Array<FilterField<*>> = arrayOf(),
             private val pageable: Boolean = true) {
    companion object {
        //默认的最大单页数据数目
        const val PAGE_MAX_RESULT: Int = 20
        //默认的参数名
        const val SEARCH_NAME = "search"
        const val PAGE_FIRST_NAME = "pageFirst"
        const val PAGE_MAX_RESULT_NAME = "pageMaxResult"
        const val ORDER_NAME = "order"
    }
    //筛选出所有带有default标记的order field的下标。
    private val defaultOrderMapIndex: Set<Int> by lazy { HashSet<Int>().also { set -> orderMap.forEachIndexed { index, it -> if(it.default!=null)set.add(index) } } }
    private val enableOrderMaxIndex: Map<String, Int> by lazy {HashMap<String, Int>().also{map -> orderMap.forEachIndexed{index, it -> map.put(it.name, index)}}}

    fun filterParameters(parameters: Map<String, Any?>): QueryFeature {
        //特殊参数规则：
        //1. order参数用于order排序
        //2. search参数用于search泛搜索
        //3. pageFirst & pageMaxResult参数用于分页。不过在分页没有开启的情况下无效，但还是不推荐使用这些名称作为filter参数
        //4. 其他参数都作为filter参数
        val retFilters = ArrayList<Criterion>()
        val retOrders = ArrayList<Order>()
        val innerFilters = HashMap<String, ArrayList<Criterion>>()
        val innerOrders = HashMap<String, ArrayList<Order>>()
        var pageFirst: Int? = null
        var pageMaxResult: Int? = null
        fun<T, R> HashMap<T, ArrayList<R>>.mutPut(key: T, value: R) {
            if(this.containsKey(key)) {
                val list = this.remove(key)!!
                list.add(value)
                this.put(key, list)
            }else{
                this.put(key, kotlin.collections.arrayListOf(value))
            }
        }
        fun<T, R> HashMap<T, ArrayList<R>>.mutPutAll(key: T, value: List<R>) {
            if(this.containsKey(key)) {
                val list = this.remove(key)!!
                list.addAll(value)
                this.put(key, list)
            }else{
                this.put(key, kotlin.collections.ArrayList(value))
            }
        }
        //处理filter参数
        //从filter获得的所有where条件使用AND进行连接。
        filterMap.forEach { i ->
            if(i.default!=null){//该filter默认施加
                if(i.customFilter!=null) {//存在自定义行为，调用自定义行为生成所需的filter而不是自己生成
                    val result = i.customFilter.invoke(i.default)
                    if(i.innerJoin!=null)innerFilters.mutPut(i.innerJoin, result)
                    else retFilters.add(result)
                }else{//自己生成default filter
                    if(i.filterTypes.isNotEmpty()){
                        if(i.innerJoin!=null)innerFilters.mutPut(i.innerJoin, i.filterTypes.first().getCriterion(i.nameOfModel, i.default))
                        else retFilters.add(i.filterTypes.first().getCriterion(i.nameOfModel, i.default))
                    }//else 不会生效
                }
            }else{//非默认施加。从parameters中搜索匹配的filter参数。形式固定为"$name__$type"。单一的name会被视为EQ。
                //如果parameter存在多个，会同时被添加到限定条件内并以OR条件连接。至于能生效几个就管不着了。
                if(parameters.containsKey(i.name)){//确认省略后缀的参数，默认为EQ。
                    val param = parameters[i.name]
                    //此处的嵌套结构：
                    //  doParameterByOr：自动分析param的可能结构并调用委托处理所有string参数
                    //  委托：处理一个string参数，用来加入retFilters
                    //  convert： 将string参数转换为可用的T的值
                    param.analysisList {
                        i.customFilter?.invoke(i.convert(it))?:FilterType.EQ.getCriterion(i.nameOfModel, i.convert(it))
                    }.recursiveConnect { a, b ->Restrictions.or(a, b)}?.let{
                        if(i.innerJoin!=null)innerFilters.mutPut(i.innerJoin, it)
                        else retFilters.add(it)
                    }
                }
                i.filterTypes.forEach { type ->
                    val paramName = type.getParamName(i.name)
                    if(parameters.containsKey(paramName)){
                        val param = parameters[paramName]
                        param.analysisList {
                            i.customFilter?.invoke(i.convert(it))?:type.getCriterion(i.nameOfModel, i.convert(it))
                        }.recursiveConnect { a, b ->Restrictions.or(a, b)}?.let{
                            if(i.innerJoin!=null)innerFilters.mutPut(i.innerJoin, it)
                            else retFilters.add(it)
                        }
                    }
                }
            }
        }
        //处理search参数
        //从search获得的内容会转换为N条where条件。同一个parameter下的条件用AND连接，多个parameter下的多个条件用OR连接。
        if(parameters.containsKey(SEARCH_NAME)){
            val retSearch = ArrayList<Criterion>()
            val param = parameters[SEARCH_NAME]
            searchMap.forEach { searchName ->
                param.analysisList { FilterType.LIKE.getCriterion(searchName, it) }.recursiveConnect { a, b ->Restrictions.and(a, b) }?.let { retSearch.add(it) }
            }
            retSearch.recursiveConnect { a, b -> Restrictions.or(a, b) }?.let { retFilters.add(it) }
        }
        //处理order参数
        //从order参数获得的String会依次被列入排序。
        if(parameters.containsKey(ORDER_NAME)){
            val param = parameters[ORDER_NAME]
            param.analysisList { p ->
                if(p.isNotBlank()){
                    val desc: Boolean
                    val pName: String
                    if(p.startsWith('-')){//DESC
                        desc = true
                        pName = p.substring(1)
                    }else{//ASC
                        desc = false
                        pName = p
                    }
                    //已经获得了pName。
                    if(enableOrderMaxIndex.containsKey(pName)){
                        val orderField = orderMap[enableOrderMaxIndex[pName]!!]
                        if(orderField.customOrder!=null){
                            if(orderField.innerJoin!=null) innerOrders.mutPutAll(orderField.innerJoin, orderField.customOrder.invoke(orderField, desc))
                            else retOrders.addAll(orderField.customOrder.invoke(orderField, desc))
                        }else{
                            if(orderField.innerJoin!=null) innerOrders.mutPut(orderField.innerJoin, if(desc)Order.desc(orderField.nameOfModel)else Order.asc(orderField.nameOfModel))
                            else retOrders.add(if(desc)Order.desc(orderField.nameOfModel)else Order.asc(orderField.nameOfModel))
                        }
                    }
                }
            }
        }else{//在没有order参数时，会启用默认排序。
            defaultOrderMapIndex.forEach { i ->
                val orderField = orderMap[i]
                val desc = orderField.default!!.toLowerCase() == "desc"
                if(orderField.customOrder!=null){
                    retOrders.addAll(orderField.customOrder.invoke(orderField, desc))
                }else{
                    retOrders.add(if(desc)Order.desc(orderField.nameOfModel)else Order.asc(orderField.nameOfModel))
                }
            }
        }
        //处理分页参数
        if(parameters.containsKey(PAGE_FIRST_NAME)){
            pageFirst = parameters[PAGE_FIRST_NAME].analysisList { it }.firstOrNull()?.toIntOrNull()
        }
        if(parameters.containsKey((PAGE_MAX_RESULT_NAME))){
            pageMaxResult = parameters[PAGE_MAX_RESULT_NAME].analysisList { it }.firstOrNull()?.toIntOrNull()
        }
        return QueryFeature(retFilters, retOrders, innerFilters, innerOrders, null, pageFirst, pageMaxResult)
    }

    /*用于处理排序行为。该类规定对某一种字段的排序处理。包括：
        该字段在参数中的名称，以及自定义该字段在model中的名称。
        可以自定义排序行为。传入DESC值，传出要自定义的Order序列。
        可以将field设为default，这样在没有任何order参数时，带有default标记的field会作为默认排序器。
     */
    class OrderField(val name: String, private val modelName: String? = null, val innerJoin: String? = null,
                     val default: String? = null,
                     val customOrder: (OrderField.(Boolean)->List<Order>)? = null) {
        val nameOfModel: String get() = modelName?:name
    }
    /*用于处理过滤行为。该类规定对某一种字段的过滤处理。包括：
        该字段在参数中的名称，以及自定义该字段在model中的名称。
        该字段的类型，以及该字段可用的过滤行为。也可以自定义过滤行为。
        可以将field的default设置为一个默认值，带有default标记的field会作为默认过滤器，总是起作用。
            如果不存在自定义行为，会从type取第一个做default的TYPE；如果type不存在，该效果不会生效；如果custom存在，会覆盖默认的default行为。
        需要为域设定一个转换器，用于将从参数列表获取到的string类型的参数转换为指定的T类型。不过如果参数是基本类型，会自动给予一个转换器。
        自定义行为传入一个Any参数代表经过转换后的域参数；传出自定义情况下生成的限定条件。
     */
    class FilterField<out T>(private val clazz: KClass<T>,
                         val name: String, private val modelName: String? = null, val innerJoin: String? = null,
                         val types: Array<FilterType>? = null,
                         val default: T? = null,
                         private val converter: ((String)->T)? = null,
                         val customFilter: ((Any)->Criterion)? = null) where T: Any {
        val nameOfModel: String get() = modelName?:name
        val filterTypes: Array<FilterType> by lazy {
            types?://在type为空时，尝试按照常用类型施加一个默认列表。
                when(clazz) {
                    Int::class -> arrayOf(FilterType.EQ, FilterType.NEQ, FilterType.GT, FilterType.LT, FilterType.GE, FilterType.LE)
                    Float::class -> arrayOf(FilterType.EQ, FilterType.NEQ, FilterType.GT, FilterType.LT, FilterType.GE, FilterType.LE)
                    Double::class -> arrayOf(FilterType.EQ, FilterType.NEQ, FilterType.GT, FilterType.LT, FilterType.GE, FilterType.LE)
                    Boolean::class -> arrayOf(FilterType.EQ, FilterType.NEQ)
                    Byte::class -> arrayOf(FilterType.EQ, FilterType.NEQ, FilterType.GT, FilterType.LT, FilterType.GE, FilterType.LE)
                    String::class -> arrayOf(FilterType.EQ, FilterType.NEQ)
                    else -> emptyArray()
                }
        }
        fun convert(p: String): T {
            if(converter!=null){
                try{return converter.invoke(p)}catch(e: Exception){throw FilterConvertFailedException(name)}
            }else{
                try{
                    when(clazz) {
                        Int::class -> return p.toInt() as T
                        Float::class -> return p.toFloat() as T
                        Double::class -> return p.toDouble() as T
                        Boolean::class -> return p.toBoolean() as T
                        Byte::class -> return p.toByte() as T
                        String::class -> return p as T
                    }
                }catch(e: Exception){throw FilterConvertFailedException(name)}
                throw FilterNoConverterException(name)
            }
        }
    }
    enum class FilterType{
        EQ, NEQ,
        GT, LT, GE, LE,
        LIKE

    }

}

private fun Filter.FilterType.getCriterion(propertyName: String, value: Any): Criterion {
    return when(this){
        Filter.FilterType.EQ -> Restrictions.eq(propertyName, value)
        Filter.FilterType.NEQ -> Restrictions.ne(propertyName, value)
        Filter.FilterType.GT -> Restrictions.gt(propertyName, value)
        Filter.FilterType.LT -> Restrictions.lt(propertyName, value)
        Filter.FilterType.GE -> Restrictions.ge(propertyName, value)
        Filter.FilterType.LE -> Restrictions.le(propertyName, value)
        Filter.FilterType.LIKE -> Restrictions.ilike(propertyName, "%$value%")
    }
}
private fun Filter.FilterType.getParamName(modelName: String): String {
    return "${modelName}__${this.name.toLowerCase()}"
}
fun<T> Any?.analysisList(action: (String)->T): List<T> {
    val ret = ArrayList<T>()
    if(this!=null){
        if(this is String) {
            ret.add(action(this))
        }else if(this is Array<*>){
            this.forEach {
                if(it!=null){
                    if(it is String)ret.add(action(it))
                    else ret.add(action(it.toString()))
                }
            }
        }else ret.add(action(this.toString()))
    }
    return ret
}
private fun<T> List<T>.recursiveConnect(connect: (T, T)->T): T? {
    if(this.isEmpty())return null
    else if(this.size==1)return this[0]
    else {
        var orCriterion: T? = null
        this.forEach {
            if(orCriterion==null)orCriterion = it
            else orCriterion = connect(orCriterion!!, it)
        }
        return orCriterion
    }
}


class FilterNoConverterException(fieldName: String): Exception("Filter field '$fieldName' have no converter")

class FilterConvertFailedException(fieldName: String): Exception("Filter field '$fieldName' converting to std.type was failed")