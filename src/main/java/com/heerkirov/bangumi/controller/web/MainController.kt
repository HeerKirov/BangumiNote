package com.heerkirov.bangumi.controller.web

import com.heerkirov.bangumi.service.Security
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping

@Controller
class MainController(@Autowired private val security: Security) {
    @RequestMapping("/BangumiNote/")
    fun index(): String {
        if(security.currentUser()!=null){
            return "redirect:/BangumiNote/web/home"
        }else{
            return "redirect:/BangumiNote/web/login"
        }
    }
}