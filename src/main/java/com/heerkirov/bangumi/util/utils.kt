package com.heerkirov.bangumi.util

import org.hibernate.criterion.Criterion
import org.hibernate.criterion.Restrictions
import java.lang.reflect.Method
import java.util.*

fun String.toFirstUppercase(): String {
    if(this.isEmpty())return this
    else return this.substring(0, 1).toUpperCase()+this.substring(1)
}
fun String.toFirstLowercase(): String {
    if(isEmpty())return this
    else return substring(0, 1).toLowerCase()+substring(1)
}
fun String.leftTrip(s: CharSequence): String {
    //检查去掉左右空格的字符串的左侧。如果不包含指定的字符串，就额外追加该字符串。
    val ret = this.trim()
    if(ret.substring(0..s.length) != s) return "$s$ret"
    else return ret
}

fun<T> Iterable<T>.join(mid: String, make: (T)->String): String {
    val sb = StringBuffer()
    var first = true
    this.map {
        if(first)first = false else sb.append(mid)
        sb.append(make(it))
    }
    return sb.toString()
}
fun<T> Iterable<T>.join(mid: String): String = this.join(mid){it.toString()}

/**SQL语句中，如果in查询的数组长度为0,会报SQL语法错误。为了避免这个错误，必须用一个特别的函数替代in函数，使其在长度为0时返回一个恒定false值。
 */
fun Restrictions_in(propertyName: String, value: Collection<*>): Criterion {
    if(value.isNotEmpty())return Restrictions.`in`(propertyName, value)
    else return Restrictions.idEq(Int.MIN_VALUE) //这是一个危险值，这潜在要求任何model的id不能为负极限。为了构造一个恒等于false的值，只能这么写。
}

fun<T> Class<T>.getDeclaredMethodByName(name: String): Method {
    this.declaredMethods.forEach {
        if(it.name == name)return it
    }
    throw NoSuchFieldException("${this.name}.$name")
}

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
            if(sp.size > 0)sp[0].toIntOrNull()?:0 else 0,
            if(sp.size > 1)sp[1].toIntOrNull()?:0 else 0,
            if(sp.size > 2)sp[2].toIntOrNull()?:0 else 0,
            if(sp.size > 3)sp[3].toIntOrNull()?:0 else 0,
            if(sp.size > 4)sp[4].toIntOrNull()?:0 else 0,
            if(sp.size > 5)sp[5].toIntOrNull()?:0 else 0,
            if(sp.size > 6)sp[6].toIntOrNull()?:0 else 0
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