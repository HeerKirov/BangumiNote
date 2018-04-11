package com.heerkirov.bangumi.controller.converter

import com.heerkirov.bangumi.model.base.ModelInterface
import com.heerkirov.bangumi.service.ServiceSet
import com.heerkirov.converter.*
import kotlin.reflect.KClass
import kotlin.reflect.full.safeCast

//适用于带内部代换的数组结构的转换器。
class ModelListConverter<T>(private val subClazz: KClass<T>, val notNull: Boolean = true, private val converter: ModelConverter<T>? = null): AbstractConverter<List<T?>, List<Any?>>(List::class as KClass<List<T?>>) where T: ModelInterface {

    override fun new(json: Any): List<T?> {
        //将json array转换为代换后的model array。
        val validate = validateNew(json)
        return validate.mapIndexed { index, item ->
            item?.let{ i ->// 非null
                converter?.new(i)?:subClazz.safeCast(i)?:throw ConvertGoalTypeError(clazz.simpleName!!)
            }?: if(!notNull){null}else throw ConvertArrayNulError(index)//是null
        }
    }

    override fun update(json: Any, goal: Any): List<T?> {
        return new(json)
    }

    override fun validateNew(json: Any): List<Any?> {
        List::class.safeCast(json)?.let { return it }?:throw ConvertTypeError("Content cannot be converted to type List")
    }

    override fun validateUpdate(json: Any): List<Any?> {
        return validateNew(json)
    }

    override fun parse(goal: Any): Any {
        //将model array转换为代换后的json array。
        val resource = clazz.safeCast(goal)?:throw ConvertGoalTypeError(clazz.simpleName!!)
        return resource.mapIndexed { index, item ->
            if(item!=null) {
                converter?.parse(item)?:subClazz.safeCast(item)?:throw ConvertGoalTypeError(clazz.simpleName!!)
            }else if(!notNull) null else throw ConvertArrayNulError(index)
        }
    }

    fun serviceNew(json: Any): List<ServiceSet<T>?> {
        val validate = validateNew(json)
        return validate.mapIndexed { index, item ->
            if(item!=null){
                converter?.serviceNew(item)?:throw ConvertGoalTypeError(clazz.simpleName!!)
            }else if(!notNull){null}else throw ConvertArrayNulError(index)//是null
        }
    }
    fun serviceParse(goal: List<ServiceSet<T>?>): Any {
        return goal.mapIndexed { index, item ->
            if(item!=null) {
                converter?.serviceParse(item)?:throw ConvertGoalTypeError(clazz.simpleName!!)
            }else if(!notNull) null else throw ConvertArrayNulError(index)
        }
    }
}