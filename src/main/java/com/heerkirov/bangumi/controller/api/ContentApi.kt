package com.heerkirov.bangumi.controller.api

import com.heerkirov.bangumi.controller.base.UserBelongRestfulController
import com.heerkirov.bangumi.controller.converter.*
import com.heerkirov.bangumi.controller.filter.*
import com.heerkirov.bangumi.model.Anime
import com.heerkirov.bangumi.model.Author
import com.heerkirov.bangumi.model.Company
import com.heerkirov.bangumi.model.Series
import com.heerkirov.bangumi.service.*
import com.heerkirov.converter.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping

@Controller @RequestMapping("/api/content/series")
class SeriesApi(@Autowired private val seriesService: SeriesService): UserBelongRestfulController<Series, Int>(Series::class) {
    override val service = seriesService
    override val converter = ModelConverter(Series::class, arrayOf(
            ModelConverter.Field("id", allowToObject = false, converter = Converter(Int::class)),
            ModelConverter.Field("uid", allowToObject = false, converter = Converter(Int::class)),
            ModelConverter.Field("name", notBlank = true, converter = Converter(String::class)),
            ModelConverter.Field("createTime", jsonName = "create_time", allowToObject = false, converter = DateTimeConverter()),
            ModelConverter.Field("updateTime", jsonName = "update_time", allowToObject = false, converter = DateTimeConverter())
    ))
    override val filter: Filter = Filter(searchMap = arrayOf("name"),
            orderMap = arrayOf(
                    Filter.OrderField("id"),
                    Filter.OrderField("uid"),
                    Filter.OrderField("name"),
                    Filter.OrderField("create_time", modelName = "createTime", default = "desc"),
                    Filter.OrderField("update_time", modelName = "updateTime")),
            filterMap = arrayOf(
                    Filter.FilterField(Int::class, "id"),
                    Filter.FilterField(Int::class, "uid"),
                    Filter.FilterField(String::class, "name")))
}

@Controller @RequestMapping("/api/content/authors")
class AuthorApi(@Autowired private val authorService: AuthorService): UserBelongRestfulController<Author, Int>(Author::class) {
    override val service = authorService
    override val converter = ModelConverter(Author::class, arrayOf(
            ModelConverter.Field("id", allowToObject = false, converter = Converter(Int::class)),
            ModelConverter.Field("uid", allowToObject = false, converter = Converter(Int::class)),
            ModelConverter.Field("name", notBlank = true, converter = Converter(String::class)),
            ModelConverter.Field("originName", jsonName = "origin_name", notNull = false, required = false, converter = Converter(String::class)),
            ModelConverter.Field("createTime", jsonName = "create_time", allowToObject = false, converter = DateTimeConverter()),
            ModelConverter.Field("updateTime", jsonName = "update_time", allowToObject = false, converter = DateTimeConverter())
    ))
    override val filter: Filter = Filter(searchMap = arrayOf("name", "originName"),
            orderMap = arrayOf(
                    Filter.OrderField("id"),
                    Filter.OrderField("uid"),
                    Filter.OrderField("name"),
                    Filter.OrderField("origin_name", modelName = "originName"),
                    Filter.OrderField("create_time", modelName = "createTime", default = "desc"),
                    Filter.OrderField("update_time", modelName = "updateTime")),

            filterMap = arrayOf(
                    Filter.FilterField(Int::class, "id"),
                    Filter.FilterField(Int::class, "uid"),
                    Filter.FilterField(String::class, "name"),
                    Filter.FilterField(String::class, "origin_name", modelName = "originName")))
}
@Controller @RequestMapping("/api/content/companies")
class CompanyApi(@Autowired private val companyService: CompanyService): UserBelongRestfulController<Company, Int>(Company::class) {
    override val service = companyService
    override val converter = ModelConverter(Company::class, arrayOf(
            ModelConverter.Field("id", allowToObject = false, converter = Converter(Int::class)),
            ModelConverter.Field("uid", allowToObject = false, converter = Converter(Int::class)),
            ModelConverter.Field("name", notBlank = true, converter = Converter(String::class)),
            ModelConverter.Field("originName", jsonName = "origin_name", notNull = false, required = false, converter = Converter(String::class)),
            ModelConverter.Field("createTime", jsonName = "create_time", allowToObject = false, converter = DateTimeConverter()),
            ModelConverter.Field("updateTime", jsonName = "update_time", allowToObject = false, converter = DateTimeConverter())
    ))
    override val filter: Filter = Filter(searchMap = arrayOf("name", "originName"),
            orderMap = arrayOf(
                    Filter.OrderField("id"),
                    Filter.OrderField("uid"),
                    Filter.OrderField("name"),
                    Filter.OrderField("origin_name", modelName = "originName"),
                    Filter.OrderField("create_time", modelName = "createTime", default = "desc"),
                    Filter.OrderField("update_time", modelName = "updateTime")),

            filterMap = arrayOf(
                    Filter.FilterField(Int::class, "id"),
                    Filter.FilterField(Int::class, "uid"),
                    Filter.FilterField(String::class, "name"),
                    Filter.FilterField(String::class, "origin_name", modelName = "originName")))
}
@Controller @RequestMapping("/api/content/animes")
class AnimeApi(@Autowired private val animeService: AnimeService): UserBelongRestfulController<Anime, Int>(Anime::class) {
    override val service = animeService
    private val series_converter: ModelConverter<Series> = IdConverter(Series::class, arrayOf(
            ModelConverter.Field("id", allowToObject = false, converter = IntConverter()),
            ModelConverter.Field("uid", allowToObject = false, converter = IntConverter()),
            ModelConverter.Field("name", notBlank = true, converter = StringConverter())
    ))
    private val author_converter: ModelConverter<Author> = IdConverter(Author::class, arrayOf(
            ModelConverter.Field("id", allowToObject = false, converter = IntConverter()),
            ModelConverter.Field("uid", allowToObject = false, converter = IntConverter()),
            ModelConverter.Field("name", notBlank = true, converter = StringConverter()),
            ModelConverter.Field("originName", jsonName = "origin_name", notNull = false, required = false, converter = StringConverter())
    ))
    //有关series_id类引用的验证已经放入了service层。
    override val converter = ModelConverter(Anime::class, arrayOf(
            ModelConverter.Field("id", allowToObject = false, converter = IntConverter()),
            ModelConverter.Field("uid", allowToObject = false, converter = IntConverter()),
            ModelConverter.Field("name", notBlank = true, converter = StringConverter()),
            ModelConverter.Field("originName", jsonName = "origin_name", notNull = false, required = false, converter = StringConverter()),
            ModelConverter.Field("otherName", jsonName = "other_name", notNull = false, required = false, converter = StringConverter()),

            ModelConverter.Field("type", notBlank = true, default = "other", converter = StringConverter(limitSet = setOf("novel", "comic", "game", "origin", "other"))),
            ModelConverter.Field("keyword", notNull = false, required = false, converter = StringConverter()),
            ModelConverter.Field("series", notNull = false, converter = series_converter),
            ModelConverter.Field("authorList", jsonName = "author", converter = SetConverter(Author::class, converter = author_converter)),
            ModelConverter.Field("createTime", jsonName = "create_time", allowToObject = false, converter = DateTimeConverter()),
            ModelConverter.Field("updateTime", jsonName = "update_time", allowToObject = false, converter = DateTimeConverter()),

            ModelConverter.Field("scoreLike", jsonName = "score_like", required = false, notNull = false, converter = DoubleConverter()),
            ModelConverter.Field("scorePatient", jsonName = "score_patient", required = false, notNull = false, converter = DoubleConverter()),
            ModelConverter.Field("makeMake", jsonName = "make_make", required = false, notNull = false, converter = DoubleConverter()),
            ModelConverter.Field("makeDrama", jsonName = "make_drama", required = false, notNull = false, converter = DoubleConverter()),
            ModelConverter.Field("makeMusic", jsonName = "make_music", required = false, notNull = false, converter = DoubleConverter()),
            ModelConverter.Field("makePerson", jsonName = "make_person", required = false, notNull = false, converter = DoubleConverter()),
            ModelConverter.Field("makeBackground", jsonName = "make_background", required = false, notNull = false, converter = DoubleConverter()),
            ModelConverter.Field("levelR18", jsonName = "level_r18", required = false, notNull = false, converter = DoubleConverter()),
            ModelConverter.Field("levelR18G", jsonName = "level_r18g", required = false, notNull = false, converter = DoubleConverter())
    ))
    override val filter: Filter = Filter(searchMap = arrayOf("name", "originName", "otherName", "keyword"), //TODO 最好想个办法能筛选othername和keyword……这么搞就失去用array的意义了……
            orderMap = arrayOf(
                    Filter.OrderField("id"),
                    Filter.OrderField("uid"),
                    Filter.OrderField("name"),
                    Filter.OrderField("origin_name", modelName = "originName"),
                    Filter.OrderField("other_name", modelName = "otherName"),
                    Filter.OrderField("type"),
                    Filter.OrderField("keyword"),
                    Filter.OrderField("series", modelName = "series.id"),
                    Filter.OrderField("author", modelName = "id", innerJoin = "authorList"),
                    Filter.OrderField("score_like", modelName = "scoreLike"),
                    Filter.OrderField("score_patient", modelName = "scorePatient"),
                    Filter.OrderField("make_make", modelName = "makeMake"),
                    Filter.OrderField("make_drama", modelName = "makeDrama"),
                    Filter.OrderField("make_music", modelName = "makeMusic"),
                    Filter.OrderField("make_person", modelName = "makePerson"),
                    Filter.OrderField("make_background", modelName = "makeBackground"),
                    Filter.OrderField("level_r18", modelName = "levelR18"),
                    Filter.OrderField("level_r18g", modelName = "levelR18G"),
                    Filter.OrderField("create_time", modelName = "createTime", default = "desc"),
                    Filter.OrderField("update_time", modelName = "updateTime")),
            filterMap = arrayOf(
                    Filter.FilterField(Int::class, "id"),
                    Filter.FilterField(Int::class, "uid"),
                    Filter.FilterField(String::class, "name"),
                    Filter.FilterField(String::class, "origin_name", modelName = "originName"),
                    Filter.FilterField(String::class, "type"),
                    Filter.FilterField(Int::class, "series_id", modelName = "series.id"),
                    Filter.FilterField(Int::class, "author_id", innerJoin = "authorList", modelName = "id")))



}