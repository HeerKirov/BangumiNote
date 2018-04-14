package com.heerkirov.bangumi.controller.ktml.data.anime

import com.heerkirov.bangumi.controller.ktml.WideTemplateBasic
import com.heerkirov.bangumi.controller.ktml.data.DataTemplateBasic
import com.heerkirov.ktml.builder.ConstProxy
import com.heerkirov.ktml.builder.HtmlView
import com.heerkirov.ktml.builder.impl
import com.heerkirov.ktml.element.text
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**需要注意的是它直接继承了WideTemplate。
 */
@Component
class AnimeListView(@Autowired p: ConstProxy): HtmlView(WideTemplateBasic::class, p, {
    impl("TITLE", text("番组 - 数据库 - ${proxyStr("val_logo")}"))
    impl("NAV_BAR", text("""
    {title: "数据库", link: "${proxyURL("web_data")}"},
    {title: "番组", link: "#"}
    """.trimIndent()))
    impl("SCRIPT", text("""
        var typeMapping = {
            novel: "小说",
            comic: "漫画",
            game: "游戏",
            origin: "原创",
            other: "其他"
        };
        var rest = restful.newlist({url: "${proxyURL("api_content_anime")}"}).appendparams(requestparams);
        build_list(${'$'}("#api-panel")).info({
            title: "番组",
            createUrl: "${proxyURL("web_data_anime_create")}",
            visibleControl: true,
            visibleGroup: [
                {name: "常规信息", items: ["uid", "name", "series", "author", "create_time"], defaultItem: true},
                {name: "名称", items: ["uid", "name", "origin_name", "other_name", "keyword"]},
                {name: "来源", items: ["uid", "name", "origin_name", "type", "series", "author"]},
                {name: "主观评价", items: ["uid", "name", "score_like", "score_patient"]},
                {name: "制作评价", items: ["uid", "name", "make_make", "make_drama", "make_music", "make_person", "make_background"]},
                {name: "限制级评级", items: ["uid", "name", "level_r18", "level_r18g"]},
                {name: "更新时间", items: ["uid", "name", "create_time", "update_time"]}
            ],
            content: [
                {header: "ID", field: "uid", sortable: true, link: function (i) {return "${proxyURL("web_data_anime_detail")}" + i.id}},
                {header: "名称", field: "name", sortable: true, link: function (i) {return "${proxyURL("web_data_anime_detail")}" + i.id}},
                {header: "原名", field: "origin_name", sortable: true},
                {header: "其他名", field: "other_name", sortable: true},

                {header: "原作", field: "type", sortable: true, type: "mapping", typeInfo: {map: typeMapping}},
                {header: "关键字", field: "keyword", sortable: true},
                {header: "系列", field: "series", type: "source", sortable: true, typeInfo: {subField: ["name"]}, link: function(i) {return "${proxyURL("web_data_series_detail")}" + i.id}},
                {header: "作者", field: "author", type: "source", sortable: true, typeInfo: {many: true, subField: ["name"]}, link: function(i) {return "${proxyURL("web_data_author_detail")}" + i.id}},

                {header: "评价:喜爱", field: "score_like", sortable: true},
                {header: "评价:耐看", field: "score_patient", sortable: true},
                {header: "R18评级", field: "level_r18", sortable: true},
                {header: "R18G评级", field: "level_r18g", sortable: true},
                {header: "制作", field: "make_make", sortable: true},
                {header: "剧本", field: "make_drama", sortable: true},
                {header: "音乐", field: "make_music", sortable: true},
                {header: "人物", field: "make_person", sortable: true},
                {header: "背景", field: "make_background", sortable: true},

                {header: "条目创建时间", field: "create_time", sortable: true, type: "datetime"},
                {header: "最后更新时间", field: "update_time", sortable: true, type: "datetime"}
            ]
        }).rest(rest.request).build();
    """.trimIndent()))
    /**
     *      1. 新功能：用点分割field，会按照层级结构搜索json。基于这个功能，link功能也会升级，为link function传入的参数会依次列出所有的中间层级。（已完成）
     *      2. 新组件：Mapping。用于展示固定内容。给出一个映射，将json值映射成目标值再展出。(已完成)
     *      3. 升级组件：Text 添加many选项，可以处理list. （已完成）
     *      4. 功能重构：添加自选显示列的功能，可以开启/关闭此功能。（已完成）
     *      5. 功能升级：添加按组显示预定好的列组的功能。（已完成）
     */
})