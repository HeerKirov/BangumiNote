package com.heerkirov.bangumi.controller.api

import com.heerkirov.bangumi.controller.base.*
import com.heerkirov.bangumi.controller.converter.*
import com.heerkirov.bangumi.controller.filter.*
import com.heerkirov.bangumi.dao.QueryFeature
import com.heerkirov.bangumi.model.*
import com.heerkirov.bangumi.service.*
import com.heerkirov.converter.*
import org.hibernate.criterion.Restrictions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.ResponseStatus

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
    private val seriesSubConverter: ModelConverter<Series> = IdConverter(Series::class, arrayOf(
            ModelConverter.Field("id", allowToObject = false, converter = IntConverter()),
            ModelConverter.Field("uid", allowToObject = false, converter = IntConverter()),
            ModelConverter.Field("name", notBlank = true, converter = StringConverter())
    ))
    private val authorSubConverter: ModelConverter<Author> = IdConverter(Author::class, arrayOf(
            ModelConverter.Field("id", allowToObject = false, converter = IntConverter()),
            ModelConverter.Field("uid", allowToObject = false, converter = IntConverter()),
            ModelConverter.Field("name", notBlank = true, converter = StringConverter()),
            ModelConverter.Field("originName", jsonName = "origin_name", notNull = false, required = false, converter = StringConverter())
    ))
    private val tagSubConverter: ModelConverter<Tag> = IdConverter(Tag::class, arrayOf(
            ModelConverter.Field("id", allowToObject = false, converter = IntConverter()),
            ModelConverter.Field("uid", allowToObject = false, converter = IntConverter()),
            ModelConverter.Field("name", notBlank = true, converter = StringConverter()),
            ModelConverter.Field("description", required = false, default = "", converter = StringConverter())
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
            ModelConverter.Field("series", notNull = false, converter = seriesSubConverter),
            ModelConverter.Field("authorList", jsonName = "author", converter = SetConverter(Author::class, converter = authorSubConverter)),
            ModelConverter.Field("createTime", jsonName = "create_time", allowToObject = false, converter = DateTimeConverter()),
            ModelConverter.Field("updateTime", jsonName = "update_time", allowToObject = false, converter = DateTimeConverter()),

            ModelConverter.Field("scoreLike", jsonName = "score_like", allowToObject = false, notNull = false, converter = DoubleConverter()),
            ModelConverter.Field("scorePatient", jsonName = "score_patient", allowToObject = false, notNull = false, converter = DoubleConverter()),
            ModelConverter.Field("makeMake", jsonName = "make_make", allowToObject = false, notNull = false, converter = DoubleConverter()),
            ModelConverter.Field("makeDrama", jsonName = "make_drama", allowToObject = false, notNull = false, converter = DoubleConverter()),
            ModelConverter.Field("makeMusic", jsonName = "make_music", allowToObject = false, notNull = false, converter = DoubleConverter()),
            ModelConverter.Field("makePerson", jsonName = "make_person", allowToObject = false, notNull = false, converter = DoubleConverter()),
            ModelConverter.Field("makeBackground", jsonName = "make_background", allowToObject = false, notNull = false, converter = DoubleConverter()),
            ModelConverter.Field("levelR18", jsonName = "level_r18", allowToObject = false, notNull = false, converter = DoubleConverter()),
            ModelConverter.Field("levelR18G", jsonName = "level_r18g", allowToObject = false, notNull = false, converter = DoubleConverter()),
            ModelConverter.Field("tagList", jsonName = "tag", allowToObject = false, converter = SetConverter(Tag::class, converter = tagSubConverter))
    ))
    override val filter: Filter = Filter(searchMap = arrayOf("name", "originName", "otherName", "keyword"),
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
                    Filter.OrderField("tag", modelName = "id", innerJoin = "tagList"),
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
                    Filter.FilterField(Int::class, "author_id", innerJoin = "authorList", modelName = "id"),
                    Filter.FilterField(Int::class, "tag_id", innerJoin = "tagList", modelName = "id")))
}
@Controller @RequestMapping("/api/content/bangumis")
class BangumiApi(@Autowired private val bangumiService: BangumiService): UserBelongRestfulController<Bangumi, Int>(Bangumi::class) {
    override val service: RestfulService<Bangumi> = bangumiService
    private val animeSubConverter: ModelConverter<Anime> = IdConverter(Anime::class, arrayOf(
            ModelConverter.Field("id", allowToObject = false, converter = IntConverter()),
            ModelConverter.Field("uid", allowToObject = false, converter = IntConverter()),
            ModelConverter.Field("name", allowToObject = false, converter = StringConverter())
    ))
    private val companySubConverter: ModelConverter<Company> = IdConverter(Company::class, arrayOf(
            ModelConverter.Field("id", allowToObject = false, converter = IntConverter()),
            ModelConverter.Field("uid", allowToObject = false, converter = IntConverter()),
            ModelConverter.Field("name", notBlank = true, converter = StringConverter()),
            ModelConverter.Field("originName", jsonName = "origin_name", notNull = false, required = false, converter = StringConverter())
    ))
    private val tagSubConverter: ModelConverter<Tag> = IdConverter(Tag::class, arrayOf(
            ModelConverter.Field("id", allowToObject = false, converter = IntConverter()),
            ModelConverter.Field("uid", allowToObject = false, converter = IntConverter()),
            ModelConverter.Field("name", notBlank = true, converter = StringConverter()),
            ModelConverter.Field("description", required = false, default = "", converter = StringConverter())
    ))
    override val converter: ModelConverter<Bangumi> = ModelConverter(Bangumi::class, arrayOf(
            ModelConverter.Field("id", allowToObject = false, converter = IntConverter()),
            ModelConverter.Field("uid", allowToObject = false, converter = IntConverter()),
            ModelConverter.Field("serial", notBlank = true, converter = IntConverter()),
            ModelConverter.Field("name", notBlank = true, converter = StringConverter()),
            ModelConverter.Field("anime", converter = animeSubConverter),
            ModelConverter.Field("companyList", jsonName = "company", converter = SetConverter(Company::class, converter = companySubConverter)),

            ModelConverter.Field("publishTime", jsonName = "publish_time", notNull = false, required = false, converter = DateTimeConverter()),
            ModelConverter.Field("playType", jsonName = "play_type", notBlank = true, default = "tv", converter = StringConverter(limitSet = setOf("tv", "movie", "ova", "web", "other"))),
            ModelConverter.Field("playLength", jsonName = "play_length", notNull = false, required = false, converter = IntConverter()),
            ModelConverter.Field("playQuantity", jsonName = "play_quantity", notNull = false, required = false, converter = IntConverter()),

            ModelConverter.Field("finishedTime", jsonName = "finished_time", notNull = false, required = false, converter = DateTimeConverter()),
            ModelConverter.Field("watching", required = false, default = false, converter = BooleanConverter()),
            ModelConverter.Field("multipleTime", jsonName = "multiple_time", required = false, default = false, converter = BooleanConverter()),
            ModelConverter.Field("seenTheOriginal", jsonName = "seen_the_original", required = false, default = false, converter = BooleanConverter()),

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
            ModelConverter.Field("levelR18G", jsonName = "level_r18g", required = false, notNull = false, converter = DoubleConverter()),

            ModelConverter.Field("tagList", jsonName = "tag", converter = SetConverter(Tag::class, converter = tagSubConverter))
    ))
    override val filter: Filter = Filter(searchMap = arrayOf("name"),
            orderMap = arrayOf(
                    Filter.OrderField("id"),
                    Filter.OrderField("uid"),
                    Filter.OrderField("serial"),
                    Filter.OrderField("name"),
                    Filter.OrderField("anime", modelName = "anime.id"),
                    Filter.OrderField("company", modelName = "id", innerJoin = "companyList"),
                    Filter.OrderField("tag", modelName = "id", innerJoin = "tagList"),
                    Filter.OrderField("publish_time", modelName = "publishTime"),
                    Filter.OrderField("playType", modelName = "play_type"),
                    Filter.OrderField("playLength", modelName = "play_length"),
                    Filter.OrderField("playQuantity", modelName = "play_quantity"),
                    Filter.OrderField("finished_time", modelName = "finishedTime"),
                    Filter.OrderField("watching"),
                    Filter.OrderField("multiple_time", modelName = "multipleTime"),
                    Filter.OrderField("seen_the_original", modelName = "seenTheOriginal"),

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
                    Filter.FilterField(Int::class, "serial"),
                    Filter.FilterField(String::class, "name"),
                    Filter.FilterField(Int::class, "anime_id", modelName = "anime.id"),
                    Filter.FilterField(Int::class, "company_id", innerJoin = "companyList", modelName = "id"),
                    Filter.FilterField(Int::class, "tag_id", innerJoin = "tagList", modelName = "id"),
                    Filter.FilterField(String::class, "publish_time", modelName = "publishTime"),
                    Filter.FilterField(String::class, "play_type", modelName = "playType"),
                    Filter.FilterField(Int::class, "play_length", modelName = "playLength"),
                    Filter.FilterField(Int::class, "play_quantity", modelName = "playQuantity"),
                    Filter.FilterField(String::class, "finished_time", modelName = "finishedTime"),
                    Filter.FilterField(Boolean::class, "watching"),
                    Filter.FilterField(Boolean::class, "multiple_time", modelName = "multipleTime"),
                    Filter.FilterField(Boolean::class, "seen_the_original", modelName = "seenTheOriginal")))
}
@Controller @RequestMapping("/api/content/tags")
class TagApi(@Autowired private val tagService: TagService): UserBelongRestfulController<Tag, Int>(Tag::class) {
    override val service: RestfulService<Tag> = tagService
    private val parentSubConverter: ModelConverter<Tag> = IdConverter(Tag::class, arrayOf(
            ModelConverter.Field("id", allowToObject = false, converter = IntConverter()),
            ModelConverter.Field("uid", allowToObject = false, converter = IntConverter()),
            ModelConverter.Field("name", allowToObject = false, converter = StringConverter())
    ))
    override val converter: ModelConverter<Tag> = ModelConverter(Tag::class, arrayOf(
            ModelConverter.Field("id", allowToObject = false, converter = IntConverter()),
            ModelConverter.Field("uid", allowToObject = false, converter = IntConverter()),
            ModelConverter.Field("name", notBlank = true, converter = StringConverter()),
            ModelConverter.Field("parent", notNull = false, converter = parentSubConverter),
            ModelConverter.Field("description", notBlank = false, required = false, default = "", converter = StringConverter()),
            ModelConverter.Field("createTime", jsonName = "create_time", allowToObject = false, converter = DateTimeConverter()),
            ModelConverter.Field("updateTime", jsonName = "update_time", allowToObject = false, converter = DateTimeConverter())
    ))
    override val filter: Filter = Filter(searchMap = arrayOf("name", "description"),
            orderMap = arrayOf(
                    Filter.OrderField("id"),
                    Filter.OrderField("uid"),
                    Filter.OrderField("name"),
                    Filter.OrderField("description"),
                    Filter.OrderField("create_time", modelName = "createTime", default = "desc"),
                    Filter.OrderField("update_time", modelName = "updateTime")),
            filterMap = arrayOf(
                    Filter.FilterField(Int::class, "id"),
                    Filter.FilterField(Int::class, "uid"),
                    Filter.FilterField(String::class, "name"),
                    Filter.FilterField(Int::class, "parent", modelName = "parent.id")))
}
@Controller @RequestMapping("/api/content/bangumis/{parentId}/episodes")
class EpisodeApi(@Autowired private val episodeService: EpisodeService): UserBelongNestedController<Episode>(Episode::class) {
    override val service: RestfulService<Episode> = episodeService
    override val converter: ModelConverter<Episode> = ModelConverter(Episode::class, arrayOf(
            ModelConverter.Field("id", allowToObject = false, converter = IntConverter()),
            ModelConverter.Field("uid", allowToObject = false, converter = IntConverter()),
            ModelConverter.Field("serial", notBlank = true, converter = IntConverter()),
            ModelConverter.Field("name", notBlank = true, converter = StringConverter()),

            ModelConverter.Field("publishTime", jsonName = "publish_time", notNull = false, required = false, converter = DateTimeConverter()),
            ModelConverter.Field("finishedTime", jsonName = "finished_time", notNull = false, required = false, converter = DateTimeConverter()),

            ModelConverter.Field("createTime", jsonName = "create_time", allowToObject = false, converter = DateTimeConverter()),
            ModelConverter.Field("updateTime", jsonName = "update_time", allowToObject = false, converter = DateTimeConverter())
    ))
    override val filter: Filter = Filter(searchMap = arrayOf("name"),
            orderMap = arrayOf(
                    Filter.OrderField("id"),
                    Filter.OrderField("uid"),
                    Filter.OrderField("name"),
                    Filter.OrderField("serial", default = "asc"),
                    Filter.OrderField("publish_time", modelName = "publishTime"),
                    Filter.OrderField("finished_time", modelName = "finishedTime"),
                    Filter.OrderField("create_time", modelName = "createTime"),
                    Filter.OrderField("update_time", modelName = "updateTime")),
            filterMap = arrayOf(
                    Filter.FilterField(Int::class, "id"),
                    Filter.FilterField(Int::class, "uid"),
                    Filter.FilterField(Int::class, "serial"),
                    Filter.FilterField(String::class, "name")))

    override val parentLookup: String = "bangumi_id"

    @RequestMapping("", method = [RequestMethod.GET])fun list(@PathVariable parentId: Int) = requestList(arrayOf(parentId))
    @RequestMapping("", method = [RequestMethod.POST])@ResponseStatus(HttpStatus.CREATED)fun create(@PathVariable parentId: Int) = requestCreate(arrayOf(parentId))
    @RequestMapping("/{id}", method = [RequestMethod.GET])fun retrieve(@PathVariable parentId: Int, @PathVariable id: Int) = requestRetrieve(arrayOf(parentId, id))
    @RequestMapping("/{id}", method = [RequestMethod.PUT])fun update(@PathVariable parentId: Int, @PathVariable id: Int) = requestUpdate(arrayOf(parentId, id))
    @RequestMapping("/{id}", method = [RequestMethod.PATCH])fun partialUpdate(@PathVariable parentId: Int, @PathVariable id: Int) = requestPartialUpdate(arrayOf(parentId, id))
    @RequestMapping("/{id}", method = [RequestMethod.DELETE])@ResponseStatus(HttpStatus.NO_CONTENT)fun delete(@PathVariable parentId: Int, @PathVariable id: Int) = requestDelete(arrayOf(parentId, id))

}