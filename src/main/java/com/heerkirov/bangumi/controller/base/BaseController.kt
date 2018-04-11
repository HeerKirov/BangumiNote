package com.heerkirov.bangumi.controller.base

import com.alibaba.fastjson.JSONArray
import com.alibaba.fastjson.JSONException
import com.alibaba.fastjson.JSONObject
import com.alibaba.fastjson.serializer.SerializerFeature
import com.heerkirov.bangumi.service.Security
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.servlet.ModelAndView
import javax.servlet.http.HttpServletRequest


abstract class BaseController {
    //定义所有视图共有的行为。

    /*
    该方法的作用：写在所有的控制器方法内内含，用于包揽控制器层 - 实际业务层中间的所有中介验证。
    1. 进行权限验证。包括登录验证/目标用户验证/管理员验证.
    2. 进行协商管理，根据目标类型，将目标输出到view/json。
     */

    abstract fun security(): Security
    abstract fun request(): HttpServletRequest

    fun auth(isAuthenticated: Boolean = false, userId: String? = null, isAdmin: Boolean = false): SecurityParam {
        return SecurityParam(isAuthenticated, userId, isAdmin)
    }

    class SecurityParam(val isAuthenticated: Boolean = false,
                        val userId: String? = null,
                        val isAdmin: Boolean = false)
}

abstract class WebController : BaseController() {
    //为Web页面准备的控制器。内容包括：
    //1. 为view重写的控制方法
    //2. 为html页面准备的错误响应

    fun<RET> view(action: () -> RET): RET = view(auth(), action)
    fun<RET> view(securityParam: SecurityParam, action: () -> RET): RET {
        if(security().permit(securityParam.isAuthenticated, securityParam.userId, securityParam.isAdmin)) {
            return action()
        }else{
            //用户权限认证失败。抛出forbidden错误
            throw ForbiddenException()
        }
    }

    //用于快速抛出异常的内部函数。
    private fun standErrorView(title: String, content: String): String {
        request().let {
            it.setAttribute("error_title", title)
            it.setAttribute("error_message", content)
        }
        return "template/error/error"
    }
    //所有方法通用的404映射。
    @RequestMapping("/*")
    fun<RET> notFound(): RET { throw NotFoundException() }
    //400
    @ExceptionHandler(/*新条目修改*/BadRequestException::class)
    @ResponseStatus(/*新条目修改*/HttpStatus.BAD_REQUEST)
    fun /*新条目修改*/errorBadRequest(e: HttpStatusException) = standErrorView("400 Bad Request", e.message!!)
    //403
    @ExceptionHandler(/*新条目修改*/ForbiddenException::class)
    @ResponseStatus(/*新条目修改*/HttpStatus.FORBIDDEN)
    fun /*新条目修改*/errorForbidden(e: HttpStatusException) = standErrorView("403 Forbidden", "您的权限被阻止了。")
    //404
    @ExceptionHandler(/*新条目修改*/NotFoundException::class)
    @ResponseStatus(/*新条目修改*/HttpStatus.NOT_FOUND)
    fun /*新条目修改*/errorNotFound(e: HttpStatusException) = standErrorView("404 Not Found", "没有找到页面。")
    //405
    @ExceptionHandler(/*新条目修改*/MethodNotAllowedException::class)
    @ResponseStatus(/*新条目修改*/HttpStatus.METHOD_NOT_ALLOWED)
    fun /*新条目修改*/errorMethodNotAllowed(e: HttpStatusException) = standErrorView("405 Method Not Allowed", "使用了不正确的方法请求页面。")
}

abstract class ApiController: BaseController() {
    //为rest api准备的控制器。内容包括：
    //1. 为json重写的控制方法
    //2. 为json格式错误准备的错误响应
    //3. 验证basic auth用户认证
    protected fun<RET> view(action: () -> RET): ModelAndView = view(auth(), true, action)
    protected fun<RET> viewWithoutBasicAuth(action: () -> RET): ModelAndView = view(auth(), false, action)
    protected fun<RET> view(securityParam: SecurityParam, action: () -> RET): ModelAndView = view(securityParam, true, action)
    protected fun<RET> view(securityParam: SecurityParam, basicAuth: Boolean, action: () -> RET): ModelAndView {
        val req = request()
        if(security().basicAuth(if(basicAuth)req.getHeader("Authorization")else null).permit(securityParam.isAuthenticated, securityParam.userId, securityParam.isAdmin)) {
            val ret = action()
            when(getMaxContentType(req.getHeader("accept"), req.requestURL.toString())){
                "json" -> {
                    //启用json结构代换，使用fast json将内容渲染到json。
                    return ModelAndView("template/api/json", "content", JSONObject.toJSONString(ret, SerializerFeature.WriteMapNullValue))
                }
                "html" -> {
                    val user = security().currentUser()
                    //启用html结构代换，使用默认api模板将内容渲染到jsp。
                    return ModelAndView("template/api/html", mapOf<String, Any?>(
                            "url" to req.requestURL.toString(),
                            "content" to JSONObject.toJSONString(ret, SerializerFeature.WriteMapNullValue),
                            "security_authority" to (user!=null),
                            "security_id" to (user?.id),
                            "security_name" to (user?.name),
                            "security_is_admin" to (user?.admin)
                    ))
                }
                else -> throw Exception("Content Type name is not illegal")
            }
        }else{
            //用户权限认证失败。抛出forbidden错误
            throw ForbiddenException()
        }
    }
    private fun getMaxContentType(contentType: String?, url: String?): String {
        if(url!=null){
            val lastSplit = url.lastIndexOf('/')
            val lastPoint = url.lastIndexOf('.')
            if(lastPoint>=0&&lastPoint+1<url.length&&(lastSplit<0||lastPoint>lastSplit)){
                //点存在，且不存在斜线，或斜线在点之前。
                val extension = url.substring(lastPoint+1)
                //截取扩展名作为协商定义
                if(extension in contentTypeName){
                    return extension
                }else{
                    return contentTypeList[0].first
                }
            }//不存在，转到content type内容。
        }
        if(contentType!=null){
            val list = contentType.split(',', ';')
            var max = contentTypeLevel.size
            list.forEach {
                val current = contentTypeLevel[it]
                if(current!=null&&current<max)max = current
            }
            if(max<contentTypeLevel.size){
                return contentTypeList[max].first
            }else{
                return contentTypeList[0].first
            }
        }else{
            return contentTypeList[0].first
        }
    }

    //快速获取content构成的json结构的函数。
    protected fun contentBody(): Any? {
        val attr = request().getParameter("json")
        val text = attr?.toString()?:request().reader.readText()
        if(text.isNotBlank()) {
            try{
                return JSONObject.toJSON(text)
            }catch (_: JSONException){
                return null
            }
        }else{
            return null
        }
    }
    protected fun contentBodyObject(): JSONObject? {
        val attr = request().getParameter("json")
        val text = attr?.toString()?:request().reader.readText()

        if(text.isNotBlank()) {
            try {
                return JSONObject.parseObject(text)
            }catch (_: JSONException){
                return null
            }
        }else{
            return null
        }
    }
    protected fun contentBodyArray(): JSONArray? {
        val attr = request().getParameter("json")
        val text = attr?.toString()?:request().reader.readText()
        if(text.isNotBlank()) {
            try {
                return JSONObject.parseArray(text)
            }catch (_: JSONException){
                return null
            }
        }else{
            return null
        }
    }

    //用于处理Content Type的内部结构。
    private val contentTypeLevel by lazy { HashMap<String, Int>().also { contentTypeList.forEachIndexed { index, (_, v) -> it.put(v, index) } } }
    private val contentTypeName by lazy { contentTypeList.map { it.first }.toSet() }
    private val contentTypeList = arrayOf("html" to "text/html", "json" to "application/json")

    //用于快速抛出异常的内部函数。
    private fun<EX> standErrorView(e: EX) where EX: HttpStatusException = view { mapOf("error" to e.message, "keyword" to e.keyword.toKeyword()) }
    //所有方法通用的404映射。
    @RequestMapping("/*")
    fun notFound() = view {throw NotFoundException() }
    //400
    @ExceptionHandler(/*新条目修改*/BadRequestException::class)
    @ResponseStatus(/*新条目修改*/HttpStatus.BAD_REQUEST)
    fun /*新条目修改*/errorBadRequest(e: HttpStatusException) = standErrorView(e)
    //403
    @ExceptionHandler(/*新条目修改*/ForbiddenException::class)
    @ResponseStatus(/*新条目修改*/HttpStatus.FORBIDDEN)
    fun /*新条目修改*/errorForbidden(e: HttpStatusException) = standErrorView(e)
    //404
    @ExceptionHandler(/*新条目修改*/NotFoundException::class)
    @ResponseStatus(/*新条目修改*/HttpStatus.NOT_FOUND)
    fun /*新条目修改*/errorNotFound(e: HttpStatusException) = standErrorView(e)
    //405
    @ExceptionHandler(/*新条目修改*/MethodNotAllowedException::class)
    @ResponseStatus(/*新条目修改*/HttpStatus.METHOD_NOT_ALLOWED)
    fun /*新条目修改*/errorMethodNotAllowed(e: HttpStatusException) = standErrorView(e)
}