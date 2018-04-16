package com.heerkirov.bangumi.controller.web

import com.heerkirov.bangumi.controller.base.HtmlController
import com.heerkirov.bangumi.controller.ktml.login.DocumentView
import com.heerkirov.bangumi.controller.ktml.login.PasswordView
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping

@Controller
@RequestMapping("/web/self")
class DocumentController : HtmlController() {
    @RequestMapping("/document")
    fun documentPage() = servlet(DocumentView::class)

    @RequestMapping("/password")
    fun passwordPage() = servlet(PasswordView::class)

    @Autowired val documentView: DocumentView? = null
    @Autowired val passwordView: PasswordView? = null
}