package com.heerkirov.bangumi.controller.web

import com.heerkirov.bangumi.controller.base.HtmlController
import com.heerkirov.bangumi.controller.ktml.data.DataNavView
import com.heerkirov.bangumi.controller.ktml.data.series.*
import com.heerkirov.bangumi.controller.ktml.data.author.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping

@Controller @RequestMapping("/web/data")
class DataController : HtmlController() {
    @RequestMapping("") fun navPage() = servlet(DataNavView::class)

    @RequestMapping("/series") fun seriesList() = servlet(SeriesListView::class)
    @RequestMapping("/series/create") fun seriesCreate() = servlet(SeriesCreateView::class)
    @RequestMapping("/series/info/{id}") fun seriesInfo(@PathVariable id: Int) = servlet(SeriesDetailView::class, hashMapOf<String, Any?>("id" to id))

    @RequestMapping("/authors") fun authorList() = servlet(AuthorListView::class)
    @RequestMapping("/authors/create") fun authorCreate() = servlet(AuthorCreateView::class)
    @RequestMapping("/authors/info/{id}") fun authorInfo(@PathVariable id: Int) = servlet(AuthorDetailView::class, hashMapOf<String, Any?>("id" to id))

    @Autowired val dataNavView: DataNavView? = null
    @Autowired val seriesListView: SeriesListView? = null
    @Autowired val seriesCreateView: SeriesCreateView? = null
    @Autowired val seriesDetailView: SeriesDetailView? = null
    @Autowired val authorListView: AuthorListView? = null
    @Autowired val authorCreateView: AuthorCreateView? = null
    @Autowired val authorDetailView: AuthorDetailView? = null
}