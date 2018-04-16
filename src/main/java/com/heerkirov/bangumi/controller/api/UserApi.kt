package com.heerkirov.bangumi.controller.api

import com.heerkirov.bangumi.controller.base.*
import com.heerkirov.bangumi.controller.converter.ModelConverter
import com.heerkirov.bangumi.model.User
import com.heerkirov.bangumi.service.*
import com.heerkirov.converter.ConvertError
import com.heerkirov.converter.Converter
import com.heerkirov.converter.DateTimeConverter
import com.heerkirov.converter.StringConverter
import org.hibernate.criterion.Restrictions
import org.springframework.stereotype.Controller
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import java.util.*
import javax.servlet.http.HttpServletRequest
import kotlin.collections.HashMap

@Controller
@RequestMapping("/api/user")
class UserApi(@Autowired private val security: Security,
              @Autowired private val userService: UserService,
              @Autowired private val optionalService: OptionalService,
              @Autowired private val httpRequest: HttpServletRequest) : ApiController() {

    override fun request(): HttpServletRequest = httpRequest
    override fun security(): Security = security

    @RequestMapping("/login", method = [RequestMethod.POST])
    fun login() = viewWithoutBasicAuth {
        if(security.currentUser()!=null){
            throw BadRequestException("You have been logged in.")
        }else{
            //首先尝试从authorization获得认证信息。
            val authorization = request().getHeader("Authorization")
            val body = contentBodyObject()
            if(authorization!=null){
                if(security.doLogin(authorization)){
                    mapOf("message" to "OK")
                }else{
                    throw ForbiddenException("Login failed. Please check your username or password.")
                }
            }else if(body!=null&&body.containsKey("username")&&body.containsKey("password")){
                if(security.doLogin(body.getString("username"), body.getString("password"))){
                    mapOf("message" to "OK")
                }else{
                    throw ForbiddenException("Login failed. Please check your username or password.")
                }
            }else{
                throw ForbiddenException("Login failed. Log information is empty.")
            }

        }
    }
    @RequestMapping("/logout", method = [RequestMethod.POST])
    fun logout() = view(auth(true)) {
        security.doLogout()
        mapOf("message" to "OK")
    }
    @RequestMapping("/register", method = [RequestMethod.POST])
    fun register() = viewWithoutBasicAuth {
        if(optionalService.allowRegister){
            val body = try {contentBodyObject()!!}catch(e: NullPointerException) {
                throw BadRequestException("Information format is wrong.", HttpKeyword.INFORMATION_FORMAT_WRONG)
            }
            val user = try{ registerConverter.new(body) }catch(e: ConvertError){
                throw BadRequestException(e.message!!)
            }
            if(security.doRegister(user)){
                mapOf("message" to "OK")
            }else{
                throw BadRequestException("Register failed. User id is exists.", HttpKeyword.USER_EXISTS)
            }
        }else{
            throw ForbiddenException("Cannot register because administrator forbid it.", HttpKeyword.REGISTER_FORBIDDEN)
        }
    }
    @RequestMapping("/current", method = [RequestMethod.GET])
    fun getCurrentUser() = view(auth(true)) {
        val model = security.currentUser()
        if(model!=null){
            try { currentUserConverter.parse(model) }catch(e: ConvertError){
                throw BadRequestException(e.message!!)
            }
        }else{
            throw NotFoundException()
        }
    }
    @RequestMapping("/current", method = [RequestMethod.PUT])
    fun putCurrentUser() = view(auth(true)) {
        val contentBody = try {contentBodyObject()!!}catch(e: NullPointerException) {
            throw BadRequestException("Information format is wrong.", HttpKeyword.INFORMATION_FORMAT_WRONG)
        }
        val model = security.currentUser()
        if(model!=null){
            val updateModel = try {
                currentUserConverter.partialUpdate(contentBody, model)
            }catch(e: ConvertError){
                throw BadRequestException(e.message!!)
            }
            try{userService.update(ServiceSet(updateModel))}
            catch(e: ServiceRuntimeException) {
                throw BadRequestException(e.message!!)
            }
            currentUserConverter.parse(updateModel)
        }else{
            throw NotFoundException()
        }
    }
    @RequestMapping("/password", method = [RequestMethod.GET])
    fun getPassword() = view(auth(true)) {
        HashMap<String, Any>()
    }
    @RequestMapping("/password", method = [RequestMethod.PUT])
    fun setPassword() = view(auth(true)) {
        val contentBody = try {contentBodyObject()!!}catch(e: NullPointerException) {
            throw BadRequestException("Information format is wrong.", HttpKeyword.INFORMATION_FORMAT_WRONG)
        }
        if(contentBody.containsKey("old_password")&&contentBody.containsKey("new_password")) {
            val old_password = contentBody["old_password"].toString()
            val new_password = contentBody["new_password"].toString()
            if(security.changePassword(old_password, new_password)) {
                mapOf("message" to "OK")
            }else{
                throw BadRequestException("Password Wrong.")
            }
        }else{
            throw BadRequestException("No enough information.", HttpKeyword.NO_ENOUGH_INFORMATION)
        }
    }

    private val registerConverter = ModelConverter(User::class, arrayOf<ModelConverter.Field<*, *>>(
            ModelConverter.Field("id", notBlank = true, converter = Converter(String::class)),
            ModelConverter.Field("name", notBlank = true, converter = Converter(String::class)),
            ModelConverter.Field("password", allowToJson = false, notBlank = true, converter = Converter(String::class))
    ))
    private val currentUserConverter = ModelConverter(User::class, arrayOf<ModelConverter.Field<*, *>>(
            ModelConverter.Field("id", allowToObject = false, converter = Converter(String::class)),
            ModelConverter.Field("name", converter = Converter(String::class)),
            ModelConverter.Field("admin", jsonName = "is_admin", allowToObject = false,  converter = Converter(Boolean::class)),
            ModelConverter.Field("createTime", jsonName = "create_time", allowToObject = false,  converter = DateTimeConverter()),
            ModelConverter.Field("updateTime", jsonName = "update_time", allowToObject = false,  converter = DateTimeConverter()),
            ModelConverter.Field("lastLogin", jsonName = "last_login", allowToObject = false,  converter = DateTimeConverter())
    ))
}