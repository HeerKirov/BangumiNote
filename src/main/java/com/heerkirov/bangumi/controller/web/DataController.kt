package com.heerkirov.bangumi.controller.web

import com.heerkirov.bangumi.controller.base.HtmlController
import com.heerkirov.bangumi.controller.ktml.data.DataNavView
import com.heerkirov.bangumi.controller.ktml.data.series.*
import com.heerkirov.bangumi.controller.ktml.data.author.*
import com.heerkirov.bangumi.controller.ktml.data.company.*
import com.heerkirov.bangumi.controller.ktml.data.anime.*
import com.heerkirov.bangumi.controller.ktml.data.bangumi.BangumiCreateView
import com.heerkirov.bangumi.controller.ktml.data.bangumi.BangumiDetailView
import com.heerkirov.bangumi.controller.ktml.data.bangumi.BangumiListView
import com.heerkirov.bangumi.controller.ktml.data.tag.TagCreateView
import com.heerkirov.bangumi.controller.ktml.data.tag.TagDetailView
import com.heerkirov.bangumi.controller.ktml.data.tag.TagListView
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

    @RequestMapping("/companies") fun companyList() = servlet(CompanyListView::class)
    @RequestMapping("/companies/create") fun companyCreate() = servlet(CompanyCreateView::class)
    @RequestMapping("/companies/info/{id}") fun companyInfo(@PathVariable id: Int) = servlet(CompanyDetailView::class, hashMapOf<String, Any?>("id" to id))

    @RequestMapping("/animes") fun animeList() = servlet(AnimeListView::class)
    @RequestMapping("/animes/create") fun animeCreate() = servlet(AnimeCreateView::class)
    @RequestMapping("/animes/info/{id}") fun animeInfo(@PathVariable id: Int) = servlet(AnimeDetailView::class, hashMapOf<String, Any?>("id" to id))

    @RequestMapping("/bangumis") fun bangumiList() = servlet(BangumiListView::class)
    @RequestMapping("/bangumis/create") fun bangumiCreate() = servlet(BangumiCreateView::class)
    @RequestMapping("/bangumis/info/{id}") fun bangumiInfo(@PathVariable id: Int) = servlet(BangumiDetailView::class, hashMapOf<String, Any?>("id" to id))

    @RequestMapping("/tags") fun tagList() = servlet(TagListView::class)
    @RequestMapping("/tags/create") fun tagCreate() = servlet(TagCreateView::class)
    @RequestMapping("/tags/info/{id}") fun tagInfo(@PathVariable id: Int) = servlet(TagDetailView::class, hashMapOf<String, Any?>("id" to id))

    @Autowired val dataNavView: DataNavView? = null

    @Autowired val seriesListView: SeriesListView? = null
    @Autowired val seriesCreateView: SeriesCreateView? = null
    @Autowired val seriesDetailView: SeriesDetailView? = null

    @Autowired val authorListView: AuthorListView? = null
    @Autowired val authorCreateView: AuthorCreateView? = null
    @Autowired val authorDetailView: AuthorDetailView? = null

    @Autowired val companyListView: CompanyListView? = null
    @Autowired val companyCreateView: CompanyCreateView? = null
    @Autowired val companyDetailView: CompanyDetailView? = null

    @Autowired val animeListView: AnimeListView? = null
    @Autowired val animeCreateView: AnimeCreateView? = null
    @Autowired val animeDetailView: AnimeDetailView? = null

    @Autowired val bangumiListView: BangumiListView? = null
    @Autowired val bangumiCreateView: BangumiCreateView? = null
    @Autowired val bangumiDetailView: BangumiDetailView? = null

    @Autowired val tagListView: TagListView? = null
    @Autowired val tagCreateView: TagCreateView? = null
    @Autowired val tagDetailView: TagDetailView? = null
}