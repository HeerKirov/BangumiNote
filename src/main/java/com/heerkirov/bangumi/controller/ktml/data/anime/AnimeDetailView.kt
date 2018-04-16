package com.heerkirov.bangumi.controller.ktml.data.anime

import com.heerkirov.bangumi.controller.ktml.data.DataTemplateBasic
import com.heerkirov.ktml.builder.ConstProxy
import com.heerkirov.ktml.builder.HtmlView
import com.heerkirov.ktml.builder.impl
import com.heerkirov.ktml.element.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class AnimeDetailView(@Autowired p: ConstProxy): HtmlView(DataTemplateBasic::class, p, {
    impl("TITLE", text("番组 - 数据库 - ${proxyStr("val_logo")}"))
    impl("NAV_BAR", text("""
    {title: "数据库", link: "${proxyURL("web_data")}"},
    {title: "番组", link: "${proxyURL("web_data_anime_list")}"},
    {title: "详情", link: "#"}
    """.trimIndent()))
    impl("SCRIPT", text("""
        var series_rest = restful.newlist({url: "${proxyURL("api_content_series")}"});
        var author_rest = restful.newlist({url: "${proxyURL("api_content_author")}"});
        var series_type = {
            allowNull: true,
            foreignRequest: function(delegate) { series_rest.request(null, function(success, status, data) { if(success)delegate(data.content) }) },
            foreignHeader: function(json) { return "[" + json.uid + "] " + json.name },
            foreignValue: function(json) { return json.id },
            customContent: [
                {header: "名称", field: "name", type: "text", typeInfo: {length: 32, allowBlank: false}}
            ],
            showContent: function (json) { return json.name },
            link: function(json) { return "${proxyURL("web_data_series_detail")}" + json.id }
        };
        var author_type = {
            many: true,
            foreignRequest: function(delegate) { author_rest.request(null, function(success, status, data) { if(success)delegate(data.content) }) },
            foreignHeader: function(json) { return "[" + json.uid + "] " + json.name },
            foreignValue: function(json) { return json.id },
            customContent: [
                {header: "名字", field: "name", type: "text", typeInfo: {length: 32, allowBlank: false}},
                {header: "原名", field: "origin_name", type: "text", typeInfo: {length: 32, allowBlank: true, allowNull: true}}
            ],
            showContent: function (json) { return json.name },
            link: function(json) { return "${proxyURL("web_data_author_detail")}" + json.id }
        };
        var tag_type = {
            many: true,
            allowCustom: false, allowForeign: false,
            showContent: function (json) { return json.name },
            link: function(json) { return "${proxyURL("web_data_tag_detail")}" + json.id }
        };
        var typeMapping = [
            {header: "小说", value: "novel"},
            {header: "漫画", value: "comic"},
            {header: "游戏", value: "game"},
            {header: "原创", value: "origin"},
            {header: "其他", value: "other"}
        ];
        var rest = restful.newdetail({url: "${proxyURL("api_content_anime_detail", "id" to attrSafe("id"))}"});
        build_detail(${'$'}("#api-panel")).info({
            title: "番组",
            deleteUrl: "${proxyURL("web_data_anime_list")}",
            content: [
                {header: "ID", field: "uid", type: "text", writable: false},
                {header: "名称", field: "name", type: "text", typeInfo: {length: 128, allowBlank: false}},
                "hr",
                {header: "原名", field: "origin_name", type: "text", typeInfo: {length: 128, allowBlank: true, allowNull: true}},
                {header: "其他名", field: "other_name", type: "text", typeInfo: {length: 128, allowBlank: true, allowNull: true}},
                "hr",
                {header: "系列", field: "series", type: "foreignChoice", typeInfo: series_type},
                "hr",
                {header: "作者", field: "author", type: "foreignChoice", typeInfo: author_type},
                {header: "原作", field: "type", type: "mapping", typeInfo: {map: typeMapping}},
                {header: "关键字", field: "keyword", type: "text", typeInfo: {length: 128, allowBlank: true, allowNull: true}},
                {header: "标签", field: "tag", writable: false, type: "tags", typeInfo: tag_type},
                "collapse:主观评价",
                {header: "喜爱度", field: "score_like", type: "text", writable: false, typeInfo: {min: 0, max: 10, allowBlank: true, allowNull: true}},
                {header: "耐看度", field: "score_patient", type: "text", writable: false, typeInfo: {min: 0, max: 10, allowBlank: true, allowNull: true}},
                "collapse:制作评价",
                {header: "制作", field: "make_make", type: "text", writable: false, typeInfo: {min: 0, max: 10, allowBlank: true, allowNull: true}},
                {header: "剧本", field: "make_drama", type: "text", writable: false, typeInfo: {min: 0, max: 10, allowBlank: true, allowNull: true}},
                {header: "音乐", field: "make_music", type: "text", writable: false, typeInfo: {min: 0, max: 10, allowBlank: true, allowNull: true}},
                {header: "人物", field: "make_person", type: "text", writable: false, typeInfo: {min: 0, max: 10, allowBlank: true, allowNull: true}},
                {header: "背景", field: "make_background", type: "text", writable: false, typeInfo: {min: 0, max: 10, allowBlank: true, allowNull: true}},
                "collapse:限制级评级",
                {header: "R18 评级", field: "level_r18", type: "text", writable: false, typeInfo: {min: 0, max: 10, allowBlank: true, allowNull: true}},
                {header: "R18G评级", field: "level_r18g", type: "text", writable: false, typeInfo: {min: 0, max: 10, allowBlank: true, allowNull: true}},
                "end",
                "hr",
                {header: "条目创建时间", field: "create_time", type: "datetime", writable: false},
                {header: "最后修改时间", field: "update_time", type: "datetime", writable: false}
            ]
        }).rest(rest).load();
    """.trimIndent()))
    /**（已完成）
     *      1. 组件升级： Number 添加arrowButton选项，可以开启一个调整按钮。(已完成)
     *      2. 新组件： Mapping 映射。给出一组固定枚举值，放在选择框内。选择一个值，然后返回它的隐藏值。在show状态下，展示它的显值。(已完成)
     *      3. 新组件： ForeignChoice 功能复杂的内容选择器，适用于外键field。
     *                      首先，它能构造一个自己的固定内容列表，列表内容是隐藏值——显示值映射的。并且可以使它通过回调动态刷新值。
     *                      其次，它还有一个子面板，能在面板上产生一个小的CREATE面板，用来构造一个新的json object。
     *                      两部分都通过选项来开关。使用者可以自由选择某一种方案来确定一个选定值。同时，也允许many方案，可以在两种方案间组合多选。
     *                      为了更好地优化代码，可能需要重构CRATE面板的核心代码。
     *                      处于show状态下时，它的行为贴近一个text，但是有一个source参数使你从field的内容中做选择。
     *      4. 新功能： 分割条。 在content里添加string"hr"或"hr:<title>"创建一道分割条。带title时会附加一个名称。(已完成)
     *      5. 新功能： 折叠面板。在content里添加string"collapse"或"collapse:<title>"以及"end"，会collapse->collapse/end内的所有内容添加到一个折叠面板内，可以折叠起来，且默认是折叠状态。（已完成）
     */
})