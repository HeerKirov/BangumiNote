package com.heerkirov.bangumi.controller.web

import com.heerkirov.bangumi.controller.base.HtmlController
import com.heerkirov.bangumi.controller.ktml.login.WebLoginView
import com.heerkirov.bangumi.controller.ktml.login.WebRegisterView
import com.heerkirov.bangumi.service.Security
import com.heerkirov.ktml.builder.HtmlView
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.servlet.ModelAndView
import javax.servlet.http.HttpServletResponse
import kotlin.reflect.KClass

@Controller
@RequestMapping("/BangumiNote/web")
class WebLoginController : HtmlController() {
    override val securityLogined: Boolean = false
    override fun<V: HtmlView> servlet(view: KClass<V>, viewModel: HashMap<String, Any?>): Any {
        //重写控制器的判定行为。对于登录系列的view，在已经登录的情况下会重定向到主页。
        if(security().currentUser()!=null)return redirect(homeViewName)
        else return super.servlet(view, viewModel)
    }
    @RequestMapping("/login")
    fun loginPage() = servlet(WebLoginView::class)
    @RequestMapping("/register")
    fun registerPage() = servlet(WebRegisterView::class)

    @Autowired val webLoginView: WebLoginView? = null
    @Autowired val webRegisterView: WebRegisterView? = null
}