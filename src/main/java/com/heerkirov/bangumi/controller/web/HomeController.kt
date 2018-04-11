package com.heerkirov.bangumi.controller.web

import com.heerkirov.bangumi.controller.base.HtmlController
import com.heerkirov.bangumi.controller.ktml.home.HomeView
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping

@Controller @RequestMapping("/web/home")
class HomeController : HtmlController() {
    @RequestMapping("") fun homePage() = servlet(HomeView::class)

    @Autowired val homeView: HomeView? = null
}