package com.heerkirov.bangumi.controller.ktml.data.bangumi

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
class BangumiListView(@Autowired p: ConstProxy): HtmlView(WideTemplateBasic::class, p, {
    impl("TITLE", text("番剧 - 数据库 - ${proxyStr("val_logo")}"))
    impl("NAV_BAR", text("""
    {title: "数据库", link: "${proxyURL("web_data")}"},
    {title: "番剧", link: "#"}
    """.trimIndent()))
    impl("SCRIPT", text("""
        var typeMapping = {
            tv: "TV",
            web: "Web",
            ova: "OVA&OAD",
            movie: "剧场版",
            other: "其他"
        };
        var rest = restful.newlist({url: "${proxyURL("api_content_bangumi")}"}).appendparams(requestparams);
        build_list(${'$'}("#api-panel")).info({
            title: "番剧",
            createUrl: "${proxyURL("web_data_bangumi_create")}",
            visibleControl: true,
            visibleGroup: [
                {name: "常规信息", items: ["uid", "serial", "name", "anime", "play_type", "tag"], defaultItem: true},
                {name: "制作信息", items: ["uid", "name", "anime", "company", "publish_time"]},
                {name: "放送信息", items: ["uid", "name", "play_type", "play_quantity", "play_length", "publish_time"]},
                {name: "观影信息", items: ["uid", "name", "finished_time", "watching", "multiple_time", "seen_the_origin"]},
                {name: "主观评价", items: ["uid", "name", "score_like", "score_patient", "tag"]},
                {name: "制作评价", items: ["uid", "name", "make_make", "make_drama", "make_music", "make_person", "make_background"]},
                {name: "限制级评级", items: ["uid", "name", "level_r18", "level_r18g"]},
                {name: "更新时间", items: ["uid", "name", "create_time", "update_time"]}
            ],
            content: [
                {header: "ID", field: "uid", sortable: true, link: function (i) {return "${proxyURL("web_data_bangumi_detail")}" + i.id}},
                {header: "序列号", field: "serial", sortable: true},
                {header: "名称", field: "name", sortable: true, link: function (i) {return "${proxyURL("web_data_bangumi_detail")}" + i.id}},

                {header: "番组", field: "anime", type: "source", sortable: true, typeInfo: {subField: ["name"]}, link: function(i) {return "${proxyURL("web_data_anime_detail")}" + i.id}},

                {header: "制作公司", field: "company", type: "source", sortable: true, typeInfo: {many: true, subField: ["name"]}, link: function(i) {return "${proxyURL("web_data_company_detail")}" + i.id}},

                {header: "放送类型", field: "play_type", sortable: true, type: "mapping", typeInfo: {map: typeMapping}},
                {header: "放送话数", field: "play_quantity", sortable: true},
                {header: "单话时长", field: "play_length", sortable: true},
                {header: "发布时间", field: "publish_time", sortable: true, type: "datetime"},

                {header: "看完时间", field: "finished_time", sortable: true, type: "datetime"},
                {header: "观看中", field: "watching", sortable: true, type: "text"},
                {header: "二刷", field: "multiple_time", sortable: true, type: "text"},
                {header: "看过原作", field: "seen_the_origin", sortable: true, type: "text"},

                {header: "评价:喜爱", field: "score_like", sortable: true},
                {header: "评价:耐看", field: "score_patient", sortable: true},
                {header: "R18评级", field: "level_r18", sortable: true},
                {header: "R18G评级", field: "level_r18g", sortable: true},
                {header: "制作", field: "make_make", sortable: true},
                {header: "剧本", field: "make_drama", sortable: true},
                {header: "音乐", field: "make_music", sortable: true},
                {header: "人物", field: "make_person", sortable: true},
                {header: "背景", field: "make_background", sortable: true},
                {header: "标签", field: "tag", type: "source", sortable: true, typeInfo: {many: true, subField: ["name"]}, link: function(i) {return "${proxyURL("web_data_tag_detail")}" + i.id}},

                {header: "条目创建时间", field: "create_time", sortable: true, type: "datetime"},
                {header: "最后更新时间", field: "update_time", sortable: true, type: "datetime"}
            ]
        }).rest(rest.request).build();
    """.trimIndent()))
})