package com.heerkirov.bangumi.service

import com.heerkirov.bangumi.model.User
import org.apache.commons.codec.digest.DigestUtils
import java.util.*

class SecuritySalt(private val salt: String = "") {
    fun setPassword(user: User, newPassword: String): User {
        val rs = getRandomSalt(user)
        val pw = encryption(newPassword, rs)
        user.password = pw
        user.salt = rs
        return user
    }
    fun checkPasword(user: User, password: String): Boolean {
        val pw = encryption(password, user.salt)
        return pw == user.password
    }
    private fun encryption(password: String, randomSalt: String): String {
        //加盐加密。
        return DigestUtils.sha384Hex("$salt^${password.reversed()}.$randomSalt")
    }
    private fun getRandomSalt(user: User): String {
        //计算一串变长随机字符串。
        val ret = StringBuilder("${user.id.hashCode()}-SALT-")
        val rand = Random()
        for(i in 0..12) ret.append(rand.nextInt(128).toChar())
        return ret.toString()
    }
}