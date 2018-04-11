package com.heerkirov.bangumi.controller.converter

import com.heerkirov.bangumi.model.base.ModelException
import com.heerkirov.bangumi.model.base.ModelInterface
import com.heerkirov.bangumi.service.ServiceSet
import com.heerkirov.converter.*
import java.util.HashMap
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.safeCast

//特别为model实现的具有field审查的代换器。
//能够分别控制每一个不同的field的输出配置以及输出变换方式。
//ModelConverter有另一套对接控制器层的实现方案。
open class ModelConverter<T>(clazz: KClass<T>, private val fields: Array<Field<*, *>>): AbstractConverter<T, Map<String, Any?>>(clazz) where T: ModelInterface {
    //这个列表表示从Service层查询时，需要附加的append item。
    val serviceParseSource: Set<String> by lazy { fields.filter { it.allowToJson && it.source }.map { it.name }.toSet() }

    override fun new(json: Any): T {
        //经过验证的、传入的json内容
        val validate = validateUpdate(json)
        //过会儿要返回的内容
        val result = HashMap<String, Any?>()
        //开始json -> model的中间转换。
        fields.forEach { field ->
            if(field.allowToObject&&field.allowInCreate) {//只有开启ato的field才会参与这个转换。
                /*
                    状态判定：
                        1. jsonValue存在
                            1. jsonValue非null
                                1. jsonValue非blank ---- 设定值为jsonValue
                                2. jsonValue是blank && not blank ---- ERROR： blank
                                3. jsonValue是blank && allow blank ---- 设定值为jsonValue(blank)
                            2. jsonValue是null && not null ---- ERROR： null
                            3. jsonValue是null && allow null ---- 设定值为jsonValue(null)
                        2. jsonValue不存在
                            1. required
                                1. default存在 ---- 设定值为default
                                2. default不存在 ---- ERROR： required
                            2. not required ---- skip
                 */
                if(validate.containsKey(field.nameOfJson)){
                    val jsonValue = validate[field.nameOfJson]
                    if(jsonValue!=null){
                        if(field.converter!=null) {//在这一步之后先进行convert转换，然后对model值进行blank评估。
                            //基于field的converter转换得到新内容
                            val convertResult = field.converter!!.new(jsonValue)
                            if(isNotBlankContent(convertResult)||!field.notBlank){//评估转换后的值
                                //可以传入，然后将内容放入结果
                                result.put(field.name, convertResult)
                            }else throw ConvertFieldBlankError(field.nameOfJson) //ERROR
                        }else{//没有转换器。
                            if(isNotBlankContent(jsonValue)||!field.notBlank){//因为没有转换过程，依然评估json原值
                                result.put(field.name, jsonValue) //在没有转换器的情况下直接传入json原值。
                            }else throw ConvertFieldBlankError(field.nameOfJson) //ERROR
                        }
                    }else if(!field.notNull){
                        result.put(field.name, null)
                    }else throw ConvertFieldNullError(field.nameOfJson) //ERROR
                }else{
                    if(field.required){
                        if(field.default!=null){
                            //可以传入,直接传入default值
                            result.put(field.name, field.default)
                        }else throw ConvertFieldRequiredError(field.nameOfJson) //ERROR
                    }//else skip
                }
            }
        }
        return try {
            clazz.createInstance().also { it.new(result) }
        }catch(e: ModelException){
            throw ConvertActionError(e.message ?: "")
        }
    }
    override fun update(json: Any, goal: Any): T {
        return partialUpdate(json, goal, false)
    }
    open fun partialUpdate(json: Any, goal: Any, partial: Boolean = true): T {
        //一个额外的更新函数。它使converter能够有选择地更新model的一部分。在这个函数下，required不生效，并且使用partial check函数做内容检查。
        //经过验证的、传入的json内容
        val validate = if(partial) validatePartialUpdate(json) else validateUpdate(json)
        //update动作需要的、从原goal获得的json内容
        val baseValue = clazz.safeCast(goal)?.toMap()?:throw ConvertGoalTypeError(clazz.simpleName!!)
        //过会儿要返回的内容
        val result = HashMap<String, Any?>()
        //开始json -> model的中间转换。
        fields.forEach { field ->
            if(field.allowToObject&&field.allowInUpdate) {//只有开启ato的field才会参与这个转换。
                /*
                    状态判定：
                        1. jsonValue存在
                            1. jsonValue非null
                                1. jsonValue非blank ---- 设定值为jsonValue
                                2. jsonValue是blank && not blank ---- ERROR： blank
                                3. jsonValue是blank && allow blank ---- 设定值为jsonValue(blank)
                            2. jsonValue是null && not null ---- ERROR： null
                            3. jsonValue是null && allow null ---- 设定值为jsonValue(null)
                        2. jsonValue不存在
                            [如果是partial更新，那么直接跳过;如果是all更新，执行下面的内容]
                            1. required
                                1. default存在 ---- 设定值为default
                                2. default不存在 ---- ERROR： required
                            2. not required ---- skip
                 */
                if(validate.containsKey(field.nameOfJson)){
                    val jsonValue = validate[field.nameOfJson]
                    if(jsonValue!=null){
                        if(field.converter!=null) {//在这一步之后先进行convert转换，然后对model值进行blank评估。
                            //update动作需要额外做一个从goal获取原值的动作。
                            val baseFieldValue = baseValue[field.name]
                            //然后基于field的converter转换得到新内容
                            val convertResult = if(baseFieldValue!=null)field.converter!!.update(jsonValue, baseFieldValue) else field.converter!!.new(jsonValue)
                            if(isNotBlankContent(convertResult)||!field.notBlank){//评估转换后的值
                                //可以传入，然后将内容放入结果
                                result.put(field.name, convertResult)
                            }else throw ConvertFieldBlankError(field.nameOfJson) //ERROR
                        }else{//没有转换器。
                            if(isNotBlankContent(jsonValue)||!field.notBlank){//因为没有转换过程，依然评估json原值
                                result.put(field.name, jsonValue) //在没有转换器的情况下直接传入json原值。
                            }else throw ConvertFieldBlankError(field.nameOfJson) //ERROR
                        }
                    }else if(!field.notNull){
                        result.put(field.name, null)
                    }else throw ConvertFieldNullError(field.nameOfJson) //ERROR
                }else if(!partial){
                    if(field.required){
                        if(field.default!=null){
                            //可以传入,直接传入default值
                            result.put(field.name, field.default)
                        }else throw ConvertFieldRequiredError(field.nameOfJson) //ERROR
                    }//else skip
                }//else 直接结束。不执行对required的判定。
            }
        }
        return try {
            clazz.safeCast(goal)?.let { it.also { it.update(result) } }?:throw ConvertGoalTypeError(clazz.simpleName!!)
        }catch(e: ModelException){
            throw ConvertActionError(e.message ?: "")
        }
    }

    override fun validateNew(json: Any): Map<String, Any?> {
        if(json is Map<*, *>)return json as Map<String, Any?>
        else throw ConvertTypeError()
    }
    override fun validateUpdate(json: Any): Map<String, Any?> {
        return validateNew(json)
    }
    protected open fun validatePartialUpdate(json: Any): Map<String, Any?> {
        return validateNew(json)
    }

    override fun parse(goal: Any): Any {
        //向json的代换简单一些，不需要做合法性检查。
        //检查goal的类型并获得基础内容的map结构。
        val resource = clazz.safeCast(goal)?.toMap()?:throw ConvertGoalTypeError(clazz.simpleName!!)
        //等会要传出的内容。
        val result = HashMap<String, Any?>()
        //开始model->json的中间代换。
        fields.forEach { field ->
            if(field.allowToJson){//只有开启了atj开关的field才会代换。
                if(resource.containsKey(field.name)) {
                    val modelValue = resource[field.name]
                    if(modelValue!=null){
                        if(isNotBlankContent(modelValue)||!field.notBlank){
                            if(field.converter!=null) {
                                //有转换器，进行parse转换。
                                val convertResult = field.converter!!.parse(modelValue)
                                result.put(field.nameOfJson, convertResult)
                            }else{
                                //没有转换器，直接放入结果。
                                result.put(field.nameOfJson, modelValue)
                            }
                        }else throw ConvertFieldBlankError(field.nameOfJson)
                    }else if(field.notNull) {
                        throw ConvertFieldNullError(field.nameOfJson)
                    }else{
                        result.put(field.nameOfJson, null)
                    }
                }else{
                    if(field.required){
                        if(field.default!=null){
                            if(field.converter!=null) {
                                //有转换器，进行parse转换。
                                val convertResult = field.converter!!.parse(field.default)
                                result.put(field.nameOfJson, convertResult)
                            }else{
                                //没有转换器，直接放入结果。
                                result.put(field.nameOfJson, field.default)
                            }
                        }else throw ConvertFieldRequiredError(field.nameOfJson)
                    }//else skip
                }
            }
        }
        return result
    }

    //new函数会使用json的JSON内容产生一个新的obj，并将附带的请求信息包含在结果中。
    fun serviceNew(json: Any): ServiceSet<T> {
        val validate = validateUpdate(json)//经过验证的、传入的json内容
        val result = HashMap<String, Any?>()//过会儿要返回的obj的内容
        val appendParams = HashMap<String, Any?>()//过会儿要返回的附加内容
        //开始json -> model的中间转换。
        fields.forEach { field ->
            if(field.allowToObject&&field.allowInCreate) {//只有开启ato的field才会参与这个转换。
                if(validate.containsKey(field.nameOfJson)){
                    val jsonValue = validate[field.nameOfJson]
                    if(jsonValue!=null){
                        if(field.converter!=null) {//在这一步之后先进行convert转换，然后对model值进行blank评估。
                            //基于field的converter转换得到新内容
                            val convertResult = field.converter!!.new(jsonValue)
                            if(isNotBlankContent(convertResult)||!field.notBlank){//评估转换后的值
                                //可以传入，然后将内容放入结果
                                if(field.source)appendParams.put(field.name, convertResult)
                                else result.put(field.name, convertResult)
                            }else throw ConvertFieldBlankError(field.nameOfJson) //ERROR
                        }else{//没有转换器。
                            if(isNotBlankContent(jsonValue)||!field.notBlank){//因为没有转换过程，依然评估json原值
                                if(field.source)appendParams.put(field.name, jsonValue)
                                else result.put(field.name, jsonValue) //在没有转换器的情况下直接传入json原值。
                            }else throw ConvertFieldBlankError(field.nameOfJson) //ERROR
                        }
                    }else if(!field.notNull){
                        if(field.source)appendParams.put(field.name, null)
                        else result.put(field.name, null)
                    }else throw ConvertFieldNullError(field.nameOfJson) //ERROR
                }else{
                    if(field.required){
                        if(field.default!=null){
                            //可以传入,直接传入default值
                            if(field.source)appendParams.put(field.name, field.default)
                            else result.put(field.name, field.default)
                        }else throw ConvertFieldRequiredError(field.nameOfJson) //ERROR
                    }//else skip
                }
            }
        }
        val obj = try {
            clazz.createInstance().also { it.new(result) }
        }catch(e: ModelException){
            throw ConvertActionError(e.message ?: "")
        }
        return ServiceSet(obj, if(appendParams.isNotEmpty())appendParams else null)
    }
    //update函数会使用json的JSON内容更新传入的goal，然后返回goal和附带的请求信息。
    fun serviceUpdate(json: Any, goal: Any): ServiceSet<T> {
        return servicePartialUpdate(json, goal, false)
    }
    fun servicePartialUpdate(json: Any, goal: Any, partial: Boolean = true): ServiceSet<T> {
        //一个额外的更新函数。它使converter能够有选择地更新model的一部分。在这个函数下，required不生效，并且使用partial check函数做内容检查。
        val validate = if(partial) validatePartialUpdate(json) else validateUpdate(json)//经过验证的、传入的json内容
        //update动作需要的、从原goal获得的json内容
        val baseValue = clazz.safeCast(goal)?.toMap()?:throw ConvertGoalTypeError(clazz.simpleName!!)
        val result = HashMap<String, Any?>()//过会儿要返回的内容
        val appendParams = HashMap<String, Any?>()//过会儿要附加的内容
        //开始json -> model的中间转换。
        fields.forEach { field ->
            if(field.allowToObject&&field.allowInUpdate) {//只有开启ato的field才会参与这个转换。
                if(validate.containsKey(field.nameOfJson)){
                    val jsonValue = validate[field.nameOfJson]
                    if(jsonValue!=null){
                        if(field.converter!=null) {//在这一步之后先进行convert转换，然后对model值进行blank评估。
                            //update动作需要额外做一个从goal获取原值的动作。
                            val baseFieldValue = baseValue[field.name]
                            //然后基于field的converter转换得到新内容
                            val convertResult = if(baseFieldValue!=null)field.converter!!.update(jsonValue, baseFieldValue) else field.converter!!.new(jsonValue)
                            if(isNotBlankContent(convertResult)||!field.notBlank){//评估转换后的值
                                //可以传入，然后将内容放入结果
                                if(field.source)appendParams.put(field.name, convertResult)
                                else result.put(field.name, convertResult)
                            }else throw ConvertFieldBlankError(field.nameOfJson) //ERROR
                        }else{//没有转换器。
                            if(isNotBlankContent(jsonValue)||!field.notBlank){//因为没有转换过程，依然评估json原值
                                if(field.source)appendParams.put(field.name, jsonValue)
                                else result.put(field.name, jsonValue) //在没有转换器的情况下直接传入json原值。
                            }else throw ConvertFieldBlankError(field.nameOfJson) //ERROR
                        }
                    }else if(!field.notNull){
                        if(field.source)appendParams.put(field.name, null)
                        else result.put(field.name, null)
                    }else throw ConvertFieldNullError(field.nameOfJson) //ERROR
                }else if(!partial){
                    if(field.required){
                        if(field.default!=null){
                            //可以传入,直接传入default值
                            if(field.source)appendParams.put(field.name, field.default)
                            else result.put(field.name, field.default)
                        }else throw ConvertFieldRequiredError(field.nameOfJson) //ERROR
                    }//else skip
                }//else 直接结束。不执行对required的判定。
            }
        }
        val obj = try {
            clazz.safeCast(goal)?.let { it.also { it.update(result) } }?:throw ConvertGoalTypeError(clazz.simpleName!!)
        }catch(e: ModelException){
            throw ConvertActionError(e.message ?: "")
        }
        return ServiceSet(obj, if(appendParams.isNotEmpty())appendParams else null)
    }
    //parse函数接收一个包含附加信息的obj，并将其全盘转换为一个json结构返回。
    fun serviceParse(goal: ServiceSet<T>): Any {
        //向json的代换简单一些，不需要做合法性检查。
        //检查goal的类型并获得基础内容的map结构。
        val resource = goal.obj.toMap()
        val appendSource = goal.component2()?: hashMapOf() //提取附加资源.

        val result = HashMap<String, Any?>()//等会要传出的内容。
        //开始model->json的中间代换。
        fields.forEach { field ->
            if(field.allowToJson){//只有开启了atj开关的field才会代换。
                if((field.source&&appendSource.containsKey(field.name))||((!field.source)&&resource.containsKey(field.name))) {
                    val modelValue = if(field.source)appendSource[field.name] else resource[field.name]
                    if(modelValue!=null){
                        if(isNotBlankContent(modelValue)||!field.notBlank){
                            if(field.converter!=null) {
                                //有转换器，进行parse转换。
                                val convertResult = field.converter!!.parse(modelValue)
                                result.put(field.nameOfJson, convertResult)
                            }else{
                                //没有转换器，直接放入结果。
                                result.put(field.nameOfJson, modelValue)
                            }
                        }else throw ConvertFieldBlankError(field.nameOfJson)
                    }else if(field.notNull) {
                        throw ConvertFieldNullError(field.nameOfJson)
                    }else{
                        result.put(field.nameOfJson, null)
                    }
                }else{
                    if(field.required){
                        if(field.default!=null){
                            if(field.converter!=null) {
                                //有转换器，进行parse转换。
                                val convertResult = field.converter!!.parse(field.default)
                                result.put(field.nameOfJson, convertResult)
                            }else{
                                //没有转换器，直接放入结果。
                                result.put(field.nameOfJson, field.default)
                            }
                        }else throw ConvertFieldRequiredError(field.nameOfJson)
                    }//else skip
                }
            }
        }
        return result
    }

    /*
        对每一个field的行为进行详细控制的中间端点。
        包含一个field使用的converter。包含其他配置选项。
        name: model.field的名称。默认情况下也作为json中项的key。
        jsonName: 特别指定将其转换为json时的key的名称。
        source: 这表示此信息从append的附属信息中取得。附属规则：
                1. jsonName此时表示为在json结构中的名称
                2. name表示从哪一项附加数据中取值。
                3. obj to json时，数据从Service层通过附加参数appendItem取得。
                4. json to obj时，会将子结构生成的数据附加提交给Service层。
                        如果required=false且没有数据，不会提交任何内容。
                        如果requered=true但是有default，会提交default。
                5. 此效果双向有效。但是鉴于很多情况下双向的source内容不同步，建议写两套。
        allowToJson: 在执行将model转换为json时，本field有效。默认有效。
        allowInCreate: 在to model转换且方法为new时，本field有效。默认有效。
        allowInUpdate: 在to model转换且方法为update时，本field有效。默认有效。
        allowToObject: 在执行将json转换为model时，本field有效。默认有效。
        required: 在双向转换时，本field必须存在。默认是必须存在的。
        notNull: 非null。如果设为true，在任何一向的转换中如果值为null则抛出错误。
        notBlank: 非空。如果设为true，则在任何一向转换中，如果遇到空字符串、空map、空array则会抛出错误。
        default: 默认值。如果model->json时field为null且required，会补充该值;在json->model时如果required=false且对应值不存在，会自动补该值。
     */
    class Field<R, S>(val name: String, val jsonName: String? = null, val source: Boolean = false,
                      val allowToJson: Boolean = true, val allowToObject: Boolean = true,
                      val allowInCreate: Boolean = true, val allowInUpdate: Boolean = true,
                      val required: Boolean = true, val notNull: Boolean = true, val notBlank: Boolean = false,
                      val default: R? = null,
                      var converter: AbstractConverter<R, S>? = null) where R: Any, S: Any {
        //方便调用的属性。
        val nameOfJson: String get() = jsonName?:name
    }

    companion object {
        //判断指定内容是否是blank内容。
        private fun isBlankContent(content: Any): Boolean {
            if(content is String){
                return content.isBlank()
            }else if(content is Array<*>){
                return content.isEmpty()
            }else if(content is Collection<*>){
                return content.isEmpty()
            }else if(content is Map<*, *>){
                return content.isEmpty()
            }else{
                return false
            }
        }
        fun isNotBlankContent(content: Any): Boolean = !isBlankContent(content)
    }
}
//用于映射细节的Model转换器。专门用在对接RestService的mapping field上。
//它接受两套配置。第一套，传入一个json结构时，将其转换为一个new的model;
//第二套，传入一个idClazz类型的object时，将其认定为id转换为新对象。
class IdConverter<T>(clazz: KClass<T>, private val idClazz: KClass<*>, private val idModelName: String, fields: Array<Field<*, *>>): ModelConverter<T>(clazz, fields) where T: ModelInterface {
    constructor(clazz: KClass<T>, fields: Array<Field<*, *>>): this(clazz, Int::class, "id", fields)
    override fun new(json: Any): T {
        val idAnalyse: Any? = idClazz.safeCast(json)
        if(idAnalyse != null) {
            return try {
                clazz.createInstance().also { it.set(idModelName, idAnalyse) }
            }catch(e: ModelException){
                throw ConvertActionError(e.message ?: "")
            }
        }else return super.new(json)
    }

    override fun partialUpdate(json: Any, goal: Any, partial: Boolean): T {
        val idAnalyse: Any? = idClazz.safeCast(json)
        if(idAnalyse != null) {
            return try {
                clazz.createInstance().also { it.set(idModelName, idAnalyse) }
            }catch(e: ModelException){
                throw ConvertActionError(e.message ?: "")
            }
        }else return super.new(json)
    }
}