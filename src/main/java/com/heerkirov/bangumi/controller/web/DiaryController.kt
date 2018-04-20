package com.heerkirov.bangumi.controller.web

import com.heerkirov.bangumi.controller.base.HtmlController
import com.heerkirov.bangumi.controller.ktml.diary.DiaryView
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping

@Controller
@RequestMapping("/web/diaries")
class DiaryController: HtmlController() {
    @RequestMapping("")
    fun diaryPage() = servlet(DiaryView::class)

    @Autowired val diaryView: DiaryView? = null
}