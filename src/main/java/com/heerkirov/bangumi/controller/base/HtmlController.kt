package com.heerkirov.bangumi.controller.base

import com.alibaba.fastjson.JSON
import com.heerkirov.bangumi.service.Security
import com.heerkirov.bangumi.util.leftTrip
import com.heerkirov.bangumi.util.toFirstLowercase
import com.heerkirov.ktml.builder.ConstProxy
import com.heerkirov.ktml.builder.HtmlView
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.web.servlet.ModelAndView
import javax.servlet.http.HttpServletRequest
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties

/* 结合了WEB页面所需先导入信息的控制器。
并且综合了Ktml的功能。
 */
@Controller abstract class HtmlController : WebController() {
    @Autowired private val security: Security? = null
    @Autowired private val request: HttpServletRequest? = null
    override fun security(): Security = security!!
    override fun request(): HttpServletRequest = request!!

    //重写该属性的值，用于开启自动传入请求参数。
    protected open val requestParams: Boolean = true

    //重写该属性的值，开关控制器自动传入用户认证相关数据。
    protected open val securityAuthority: Boolean = true
    //重写该属性的值，控制是否要求必须登录才能使用本控制器内的资源。如果没有登录，会自动重定向到登录页面。
    protected open val securityLogined: Boolean = true
    //重写该属性的值，自定义默认的登陆页面地址。
    protected open val loginViewName: String = "web_login"

    //重写该属性的值，自定义默认的主页。
    protected open val homeViewName: String = "web_home"

    @Autowired private val proxy: ConstProxy? = null
    /**redirect系列函数将返回一个MAP的行为，使你重定向到具有urlName名称的URL。
     * url名称取自Proxy代理。
     */
    fun redirect(urlName: String, urlParam: HashMap<String, Any?> = hashMapOf()): Any {
        return ModelAndView("redirect: ${proxy!!.getString(urlName, urlParam)?.leftTrip("/")}")
    }
    fun redirect(urlName: String, customAction: (HashMap<String, Any?>) -> Unit): Any {
        val urlParam = HashMap<String, Any?>()
        customAction(urlParam)
        return redirect(urlName, urlParam)
    }

    /** servlet系列函数将返回一个MAP行为，渲染一个HTML页面，该页面的视图由viewName决定。
     */
    open fun<V: HtmlView> servlet(view: KClass<V>, viewModel: HashMap<String, Any?> = hashMapOf()): Any {
        if(securityAuthority){
            //加入用户认证相关的内容。
            val user = security().currentUser()
            if(user!=null){//已经登录，那么录入相关的必要信息。
                viewModel.let {
                    it.put("security_authority", true)
                    it.put("security_id", user.id)
                    it.put("security_name", user.name)
                    it.put("security_is_admin", user.admin)
                }
            }else if(securityLogined){//在没有登录的情况下，自动重定向到登陆页面。
                return redirect(loginViewName)
            }else{
                viewModel.put("security_authority", false)
            }
        }
        if(requestParams){
            request().let {
                val map = it.parameterMap
                if(map!=null&&map.isNotEmpty()){
                    viewModel.put("request_params", JSON.toJSONString(map))
                }
            }
        }
        return ModelAndView("template/ktml", "content", ktml(view).build(viewModel))
    }
    fun<V: HtmlView> servlet(view: KClass<V>, customAction:(HashMap<String, Any?>) -> Unit): Any {
        val viewModel = HashMap<String, Any?>()
        customAction(viewModel)
        return servlet(view, viewModel)
    }

    fun<V: HtmlView> ktml(clazz: KClass<V>): HtmlView {
        //通过反射查找需要的页面的字段。
        val title = clazz.simpleName?.toFirstLowercase()
        if(ktmlCache.containsKey(title)) {
            return ktmlCache[title]!!.call(this) as HtmlView
        }else for (property in this::class.declaredMemberProperties) {
            if(property.name == title) {
                ktmlCache.put(title, property)
                return property.call(this) as HtmlView
            }
        }
        throw RuntimeException("Ktml view $title is not found.")
    }

    private val ktmlCache: HashMap<String, KProperty1<out HtmlController, *>> = hashMapOf()
}
