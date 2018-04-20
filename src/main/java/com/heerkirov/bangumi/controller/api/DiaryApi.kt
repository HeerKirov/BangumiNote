package com.heerkirov.bangumi.controller.api

import com.heerkirov.bangumi.controller.base.UserBelongRestfulController
import com.heerkirov.bangumi.controller.converter.IdConverter
import com.heerkirov.bangumi.controller.converter.ModelConverter
import com.heerkirov.bangumi.controller.filter.Filter
import com.heerkirov.bangumi.model.Bangumi
import com.heerkirov.bangumi.model.Diary
import com.heerkirov.bangumi.service.DiaryService
import com.heerkirov.converter.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping

@Controller @RequestMapping("/api/diaries")
class DiaryApi(@Autowired private val diaryService: DiaryService): UserBelongRestfulController<Diary, Int>(Diary::class) {
    override val service = diaryService

    override val converter: ModelConverter<Diary> = ModelConverter(Diary::class, arrayOf(
        ModelConverter.Field("id", allowToObject = false, converter = IntConverter()),
        ModelConverter.Field("uid", allowToObject = false, converter = IntConverter()),
        ModelConverter.Field("bangumi", allowInUpdate = false, converter = IdConverter(Bangumi::class, arrayOf(
                ModelConverter.Field("id", allowToObject = false, converter = IntConverter()),
                ModelConverter.Field("uid", allowToObject = false, converter = IntConverter()),
                ModelConverter.Field("serial", allowToObject = false, converter = IntConverter()),
                ModelConverter.Field("name", allowToObject = false, converter = StringConverter())
        ))),
        ModelConverter.Field("name", allowInCreate = false, notBlank = true, converter = StringConverter()),
        ModelConverter.Field("totalEpisode", jsonName = "total_episode", allowInCreate = false, notBlank = true, converter = IntConverter()),
        ModelConverter.Field("publishEpisode", jsonName = "publish_episode", required = false, converter = IntConverter()),
        ModelConverter.Field("finishedEpisode", jsonName = "finished_episode", required = false, converter = IntConverter()),
        ModelConverter.Field("completed", jsonName = "is_completed", allowToObject = false, converter = BooleanConverter()),
        ModelConverter.Field("publishPlan", jsonName = "publish_plan", converter = ListConverter(String::class, converter = DateTimeStringConverter())),
        ModelConverter.Field("createTime", jsonName = "create_time", allowToObject = false, converter = DateTimeConverter()),
        ModelConverter.Field("updateTime", jsonName = "update_time", allowToObject = false, converter = DateTimeConverter())
    ))
    override val filter: Filter = Filter(searchMap = arrayOf("name"),
            orderMap = arrayOf(
                    Filter.OrderField("id"),
                    Filter.OrderField("uid"),
                    Filter.OrderField("name"),
                    Filter.OrderField("total_episode", modelName = "totalEpisode"),
                    Filter.OrderField("publish_episode", modelName = "publishEpisode"),
                    Filter.OrderField("finishedEpisode", modelName = "finished_episode"),
                    Filter.OrderField("is_completed", modelName = "isCompleted"),
                    Filter.OrderField("publish_time", modelName = "publishTime"),
                    Filter.OrderField("finished_time", modelName = "finishedTime"),
                    Filter.OrderField("create_time", modelName = "createTime"),
                    Filter.OrderField("update_time", modelName = "updateTime")),
            filterMap = arrayOf(
                    Filter.FilterField(Int::class, "id"),
                    Filter.FilterField(Int::class, "uid"),
                    Filter.FilterField(Int::class, "total_episode", modelName = "totalEpisode"),
                    Filter.FilterField(Int::class, "publish_episode", modelName = "publishEpisode"),
                    Filter.FilterField(Int::class, "finished_episode", modelName = "finishedEpisode"),
                    Filter.FilterField(String::class, "name")))
}