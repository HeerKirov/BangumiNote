package com.heerkirov.bangumi.model.base

import org.hibernate.engine.spi.SharedSessionContractImplementor
import org.hibernate.usertype.UserType
import java.io.Serializable
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types
import java.util.*
import kotlin.collections.ArrayList

class ArrayType : UserType {
    private val TYPES: IntArray = IntArray(1) { Types.OTHER }

    override fun sqlTypes(): IntArray = TYPES

    override fun returnedClass(): Class<*> = List::class.java

    override fun equals(p0: Any?, p1: Any?): Boolean = Objects.equals(p0, p1)

    override fun hashCode(p0: Any?): Int = Objects.hashCode(p0)

    override fun nullSafeGet(rs: ResultSet?, strings: Array<out String>?, si: SharedSessionContractImplementor?, o: Any?): Any? {
        val s = rs!!.getArray(strings!![0]).array as Array<String>
        if(rs.wasNull())return null
        else {
            val arr = ArrayList<Any>()
            for(i in 0 until s.size) arr.add(s[i])
            return arr
        }
    }

    override fun nullSafeSet(ps: PreparedStatement?, o: Any?, i: Int, si: SharedSessionContractImplementor?) {
        if(o==null){
            ps!!.setNull(i, Types.ARRAY)
        }else{
            val arrayList = o as ArrayList<String>
            val array = si!!.connection().createArrayOf("text", arrayList.toArray())
            ps!!.setArray(i, array)
        }
    }

    override fun deepCopy(o: Any?): Any? {
        return o
    }

    override fun isMutable(): Boolean = false

    override fun disassemble(o: Any?): Serializable? {
        return null
    }

    override fun assemble(s: Serializable?, o: Any?): Any? {
        return null
    }

    override fun replace(original: Any?, target: Any?, owner: Any?): Any? {
        return original
    }

    private fun toStr(o: Any): String {
        val arr = o as List<*>
        return "{" + arr.joinToString(",") + "}"
    }
    private fun toArr(s: String): List<Any> {
        val str = s.trimStart('{').trimEnd('}')
        if(str.isNotBlank()) {
            val ret = str.split(',').map { it }
            return ret
        }else return listOf()
    }
}