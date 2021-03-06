package com.heerkirov.bangumi.controller.ktml

import com.heerkirov.ktml.builder.HtmlCacheTopView
import com.heerkirov.ktml.builder.HtmlTopView
import com.heerkirov.ktml.builder.block
import com.heerkirov.ktml.builder.doc
import com.heerkirov.ktml.element.*

/** 根页面。它只提供了必须导入的文件，内容基本都是空的。
 * block: TITLE TOP_BAR* BODY BOTTOM_BAR* SCRIPT*
 */
class Meta : HtmlTopView({doc(lang = "zh-cn") {
    head {
        meta(charset = "UTF-8")
        link(href = "/BangumiNote/static/bootstrap/css/bootstrap.min.css", rel = "stylesheet")
        link(href = "/BangumiNote/static/font-awesome/css/font-awesome.min.css", rel = "stylesheet")
        link(href = "/BangumiNote/static/datetimepicker/css/bootstrap-datetimepicker.min.css", rel = "stylesheet")
        script("/BangumiNote/static/jQuery/jquery-3.3.1.min.js")
        script("/BangumiNote/static/bootstrap/js/umd/popper.min.js")
        script("/BangumiNote/static/bootstrap/js/umd/tooltip.min.js")
        script("/BangumiNote/static/bootstrap/js/bootstrap.min.js")
        script("/BangumiNote/static/rest-framework/tools.js")
        script("/BangumiNote/static/rest-framework/core.js")
        script("/BangumiNote/static/rest-framework/list.js")
        script("/BangumiNote/static/rest-framework/create.js")
        script("/BangumiNote/static/rest-framework/detail.js")
        script("/BangumiNote/static/rest-framework/diary.js")
        script("/BangumiNote/static/rest-framework/notice.js")
        script("/BangumiNote/static/datetimepicker/js/bootstrap-datetimepicker.min.js")
        meta(name = "viewport", content = "width=device-width, initial-scale=1, shrink-to-fit=no")

        title { block("TITLE") }
    }
    body {
        block("TOP_BAR", true)
        block("BODY")
        block("BOTTOM_BAR", true)
    }
    block("SCRIPT", true)
}})