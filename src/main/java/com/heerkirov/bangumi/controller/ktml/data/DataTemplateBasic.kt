package com.heerkirov.bangumi.controller.ktml.data

import com.heerkirov.bangumi.controller.ktml.TemplateBasic
import com.heerkirov.ktml.builder.HtmlView
import com.heerkirov.ktml.builder.impl
import com.heerkirov.ktml.element.text

class DataTemplateBasic : HtmlView(TemplateBasic::class, {
    impl("NAV_LIST", text("""
        {title: "系列", link: "${proxyURL("web_data_series_list")}"},
        {title: "番组", link: "${proxyURL("web_data_anime_list")}"},
        {title: "番剧", link: "${proxyURL("web_data_bangumi_list")}"},
        {title: "标签", link: "${proxyURL("web_data_tag_list")}"},
        {title: "作者", link: "${proxyURL("web_data_author_list")}"},
        {title: "制作公司", link: "${proxyURL("web_data_company_list")}"}
    """.trimIndent()))
})