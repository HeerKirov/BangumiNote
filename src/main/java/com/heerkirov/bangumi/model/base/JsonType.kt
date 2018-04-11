package com.heerkirov.bangumi.model.base

import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONException
import org.hibernate.engine.spi.SharedSessionContractImplementor
import org.hibernate.usertype.UserType
import java.io.Serializable
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types
import java.util.*
import kotlin.collections.HashMap

class JsonType : UserType {
    private val TYPES: IntArray = IntArray(1) {Types.OTHER}

    override fun sqlTypes(): IntArray = TYPES

    override fun returnedClass(): Class<*> = HashMap::class.java

    override fun equals(p0: Any?, p1: Any?): Boolean = Objects.equals(p0, p1)

    override fun hashCode(p0: Any?): Int = Objects.hashCode(p0)

    override fun nullSafeGet(rs: ResultSet?, strings: Array<out String>?, si: SharedSessionContractImplementor?, o: Any?): Any? {
        val s = rs!!.getString(strings!![0])
        if(rs.wasNull())return null
        return try {
            HashMap(JSON.parseObject(s))
        }catch (e: JSONException) {
            return null
        }
    }

    override fun nullSafeSet(ps: PreparedStatement?, o: Any?, i: Int, si: SharedSessionContractImplementor?) {
        if(o==null){
            ps!!.setNull(i, Types.OTHER)
        }else{
            try {
                ps!!.setObject(i, JSON.toJSONString(o), Types.OTHER)
            }catch (e: JSONException) {
                ps!!.setNull(i, Types.OTHER)
            }
        }
    }

    override fun deepCopy(o: Any?): Any? {
        return try {
            if(o!=null){ HashMap(JSON.parseObject(JSON.toJSONString(o))) }else{ null }
        }catch (e: JSONException) {
            return o
        }
    }

    override fun isMutable(): Boolean = true

    override fun disassemble(o: Any?): Serializable {
        return JSON.toJSONString(o)
    }

    override fun assemble(s: Serializable?, o: Any?): Any {
        return HashMap(JSON.parseObject(s as String))
    }

    override fun replace(original: Any?, target: Any?, owner: Any?): Any? {
        return deepCopy(original)
    }
}