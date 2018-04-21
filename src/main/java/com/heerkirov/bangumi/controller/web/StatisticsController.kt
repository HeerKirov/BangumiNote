package com.heerkirov.bangumi.controller.web

import com.heerkirov.bangumi.controller.base.HtmlController
import com.heerkirov.bangumi.controller.ktml.diary.DiaryView
import com.heerkirov.bangumi.controller.ktml.statistics.StatisticsNavView
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping

@Controller
@RequestMapping("/web/statistics")
class StatisticsController: HtmlController() {
    @RequestMapping("")
    fun navPage() = servlet(StatisticsNavView::class)

    @Autowired val statisticsNavView: StatisticsNavView? = null
}