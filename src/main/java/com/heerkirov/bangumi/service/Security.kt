package com.heerkirov.bangumi.service

import com.heerkirov.bangumi.dao.QueryFeature
import com.heerkirov.bangumi.model.User
import org.hibernate.criterion.Restrictions.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.*
import java.util.regex.Pattern
import javax.servlet.http.HttpSession

@Service
class Security(@Autowired private val session: HttpSession,
               @Autowired private val userService: UserService) {

    fun permit(isAuthenticated: Boolean = false, userId: String? = null, isAdmin: Boolean = false): Boolean {
        if(isAuthenticated){//有登录要求
            val user = currentUser()
            closeBasicAuth()
            if(user!=null){
                if(userId==null){
                    return ((isAdmin&&user.admin)||!isAdmin)
                }else{
                    return (user.id==userId&&(!isAdmin||(isAdmin&&user.admin)))
                }
            }else{//必须满足登录要求，否则false
                return false
            }
        }else{
            //没有要求的情况下总是true
            return true
        }
    }

    fun basicAuth(base64Code: String?): Security {
        //进行基于Basic Auth的暂时性登录验证。
        if(base64Code!=null){
            val code = String(Base64.getDecoder().decode(base64Code.split(' ').last()))
            val auth = code.split(Pattern.compile(":"), 2)
            if(auth.size>=2) {
                val ret = userService.queryFirst(QueryFeature().addWhere(and(eq("id", auth[0]), eq("password", auth[1]))))
                if(session.getAttribute("user")==null&&ret!=null){
                    ret.let {
                        it.obj.lastLogin = Calendar.getInstance()
                        userService.update(it)
                    }
                    session.setAttribute("basic", ret.obj)
                }
            }
        }
        return this
    }
    fun closeBasicAuth() {
        //手动关闭basic验证
        if(session.getAttribute("basicAuthUser")!=null){
            session.removeAttribute("basicAuthUser")
        }
    }

    fun doLogin(base64Code: String?): Boolean {
        if(base64Code!=null){
            val code = String(Base64.getDecoder().decode(base64Code.split(' ').last()))
            val auth = code.split(Pattern.compile(":"), 2)
            if(auth.size>=2) {
                return doLogin(auth[0], auth[1])
            }else{
                return false
            }
        }else{
            return false
        }
    }
    fun doLogin(username: String, password: String): Boolean {
        val ret = userService.queryFirst(QueryFeature().addWhere(and(eq("id", username), eq("password", password))))
        if(ret!=null){
            ret.let {
                it.obj.lastLogin = Calendar.getInstance()
                userService.update(it)
            }
            session.setAttribute("user", ret.obj)
            return true
        }else{
            return false
        }
    }
    fun doLogout(): Boolean {
        if(session.getAttribute("user")!=null){
            session.removeAttribute("user")
            return true
        }else{
            return false
        }
    }
    fun doRegister(user: User): Boolean {
        //注册封装。要求id不重复。
        if(userService.queryExists(QueryFeature().addWhere(eq("id", user.id)))){
            return false
        }else{
            user.let {
                it.createFieldTime = Calendar.getInstance()
                it.updateFieldTime = Calendar.getInstance()
                it.lastLogin = Calendar.getInstance()
            }
            userService.create(ServiceSet(user))
            return true
        }
    }

    fun currentUser(): User? {
        val ba = session.getAttribute("basic") as User?
        if(ba!=null){
            return ba
        }else{
            return session.getAttribute("user") as User?
        }
    }
}