package com.heerkirov.converter

import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.full.safeCast

/*
    JSON <--> Object 转换时的内容检查器。
    有如下作用：
    1. 将obj的内容按照一定规则代换为json内容。
    2. 将json的内容转化为新建的obj。
    3. 将json的内容应用到obj并进行修改。支持全部修改和部分修改。
    4. 对代换时的内容进行检查，并可以采取代换动作。
 */
abstract class AbstractConverter<T, out Origin>(protected val clazz: KClass<T>) where T: Any, Origin: Any {
    //将特定的JSON内容转换为目标类型
    abstract fun new(json: Any): T
    //使用特定的json内容更新目标实体，并返回更新后的实体。传值的实体可能不会在原实体上进行更新，需要接收结果。
    abstract fun update(json: Any, goal: Any): T
    //将目标类型转换为JSON结构。
    open fun parse(goal: Any): Any {
        return goal
    }
    //检查在new转换时传入的json结构的正确性，并返回纠正后的json结构
    abstract fun validateNew(json: Any): Origin
    //检查在update转换传入的json结构的正确性，并返回纠正后的json结构
    abstract fun validateUpdate(json: Any): Origin
}

//简单转换器。适用于json转换前后不变的简单类型。
//同时携带了基本类型的自动转换器。
open class Converter<T>(clazz: KClass<T>, private val limitSet: Set<T>? = null): AbstractConverter<T, T>(clazz) where T: Any {
    override fun new(json: Any): T {
        return validateNew(json)
    }
    //使用特定的json内容更新目标实体，并返回更新后的实体。传值的实体可能不会在原实体上进行更新，需要接收结果。
    override fun update(json: Any, goal: Any): T {
        return validateUpdate(json)
    }

    //检查在new转换时传入的json结构的正确性，并返回纠正后的json结构
    override fun validateNew(json: Any): T {
        val ret = clazz.safeCast(json)?.let { it }?:autoTurn(json)?:throw ConvertTypeError("Content cannot be converted to type ${clazz.simpleName}")
        if(limitSet!=null&&(ret !in limitSet)) throw ConvertTypeError("Content is not in limit list{${limitSet.joinToString(", ")}}")
        else return ret
    }
    //检查在update转换传入的json结构的正确性，并返回纠正后的json结构
    override fun validateUpdate(json: Any): T {
        return validateNew(json)
    }

    protected open fun autoTurn(obj: Any): T? {
        return null
    }
}

class StringConverter(limitSet: Set<String>? = null): Converter<String>(String::class, limitSet) {
    override fun autoTurn(obj: Any): String? {
        return obj.toString()
    }
}
class IntConverter(limitSet: Set<Int>? = null): Converter<Int>(Int::class, limitSet) {
    override fun autoTurn(obj: Any): Int? {
        return obj.toString().toIntOrNull()
    }
}
class DoubleConverter(limitSet: Set<Double>? = null): Converter<Double>(Double::class, limitSet) {
    override fun autoTurn(obj: Any): Double? {
        return obj.toString().toDoubleOrNull()
    }
}
class BooleanConverter(limitSet: Set<Boolean>? = null): Converter<Boolean>(Boolean::class, limitSet) {
    override fun autoTurn(obj: Any): Boolean? {
        return obj.toString().toBoolean()
    }
}

class DateTimeConverter : AbstractConverter<Calendar, String>(Calendar::class) {
    override fun new(json: Any): Calendar {
        val data = validateNew(json)
        return data.toDateTimeNumber().toCalendar()
    }

    override fun update(json: Any, goal: Any): Calendar {
        return new(json)
    }

    override fun validateNew(json: Any): String {
        return json.toString()
    }

    override fun validateUpdate(json: Any): String {
        return validateNew(json)
    }

    override fun parse(goal: Any): Any {
        return (goal as Calendar).toNumber().toString()
    }
}

//适用于带内部代换的数组结构的转换器。
class ListConverter<T>(private val subClazz: KClass<T>, val notNull: Boolean = true, private val converter: AbstractConverter<T, *>? = null): AbstractConverter<List<T?>, List<Any?>>(List::class as KClass<List<T?>>) where T: Any {

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
}
//适用于带内部代换的集合结构的转换器。
class SetConverter<T>(private val subClazz: KClass<T>, val notNull: Boolean = true, private val converter: AbstractConverter<T, *>? = null): AbstractConverter<Set<T?>, Set<Any?>>(Set::class as KClass<Set<T?>>) where T: Any {

    override fun new(json: Any): Set<T?> {
        //将json array转换为代换后的model array。
        val validate = validateNew(json)
        return validate.mapIndexed { index, item ->
            item?.let{ i ->// 非null
                converter?.new(i)?:subClazz.safeCast(i)?:throw ConvertGoalTypeError(clazz.simpleName!!)
            }?: if(!notNull){null}else throw ConvertArrayNulError(index)//是null
        }.toSet()
    }

    override fun update(json: Any, goal: Any): Set<T?> {
        return new(json)
    }

    override fun validateNew(json: Any): Set<Any?> {
        List::class.safeCast(json)?.let { return it.toSet() }?:throw ConvertTypeError("Content cannot be converted to type List")
    }

    override fun validateUpdate(json: Any): Set<Any?> {
        return validateNew(json)
    }

    override fun parse(goal: Any): Any {
        //将model set转换为代换后的json array。
        val resource = clazz.safeCast(goal)?:throw ConvertGoalTypeError(clazz.simpleName!!)
        return resource.mapIndexed { index, item ->
            if(item!=null) {
                converter?.parse(item)?:subClazz.safeCast(item)?:throw ConvertGoalTypeError(clazz.simpleName!!)
            }else if(!notNull) null else throw ConvertArrayNulError(index)
        }
    }
}


//TOOLS

fun Calendar.toNumber(): DateTimeNumber {
    val year = this[Calendar.YEAR]
    val month = this[Calendar.MONTH] + 1
    val day = this[Calendar.DAY_OF_MONTH]
    val hour = this[Calendar.HOUR_OF_DAY]
    val minute = this[Calendar.MINUTE]
    val second = this[Calendar.SECOND]
    val ms = this[Calendar.MILLISECOND]
    return DateTimeNumber(year, month, day, hour, minute, second, ms)
}
fun String.toDateTimeNumber(): DateTimeNumber {
    val sp = this.split(' ', ':', '.', '-')
    return DateTimeNumber(
            if (sp.size > 0) sp[0].toIntOrNull() ?: 0 else 0,
            if (sp.size > 1) sp[1].toIntOrNull() ?: 0 else 0,
            if (sp.size > 2) sp[2].toIntOrNull() ?: 0 else 0,
            if (sp.size > 3) sp[3].toIntOrNull() ?: 0 else 0,
            if (sp.size > 4) sp[4].toIntOrNull() ?: 0 else 0,
            if (sp.size > 5) sp[5].toIntOrNull() ?: 0 else 0,
            if (sp.size > 6) sp[6].toIntOrNull() ?: 0 else 0
    )
}


class DateTimeNumber(val year: Int, val month: Int, val day: Int, val hour: Int, val minute: Int, val second: Int, val ms: Int) {
    override fun toString(): String {
        return "$year-$month-$day $hour:$minute:$second.$ms"
    }
    fun toCalendar(): Calendar {
        val instance = Calendar.getInstance()
        instance.clear()
        instance.set(this.year, this.month - 1, this.day, this.hour, this.minute, this.second)
        instance.set(Calendar.MILLISECOND, this.ms)
        return instance
    }
}