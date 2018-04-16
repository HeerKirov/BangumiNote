package com.heerkirov.bangumi.controller.ktml.login

import com.heerkirov.bangumi.controller.ktml.SimpleBasic
import com.heerkirov.bangumi.controller.ktml.TemplateBasic
import com.heerkirov.ktml.builder.ConstProxy
import com.heerkirov.ktml.builder.HtmlView
import com.heerkirov.ktml.builder.impl
import com.heerkirov.ktml.element.text
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class DocumentView(@Autowired p: ConstProxy): HtmlView(TemplateBasic::class, p, {
    impl("TITLE", text("个人资料 - ${proxyStr("val_logo")}"))
    impl("NAV_LIST", text("""
        {title: "个人资料", link: "${proxyURL("web_document")}"},
        {title: "修改密码", link: "${proxyURL("web_password")}"}
    """.trimIndent()))
    impl("SCRIPT", text("""
        var rest = restful.newdetail({url: "${proxyURL("api_user_current")}"});
        build_detail(${'$'}("#api-panel")).info({
            title: "个人资料",
            allowDelete: false,
            allowPartialUpdate: false,
            content: [
                {header: "ID", field: "id", type: "text", writable: false},
                {header: "用户名", field: "name", type: "text", typeInfo: {length: 16, allowBlank: false}},
                "hr",
                {header: "管理员", field: "is_admin", type: "bool", writable: false, typeInfo: {noIcon: false}},
                "hr",
                {header: "最后登录", field: "last_login", type: "datetime", writable: false},
                {header: "用户注册时间", field: "create_time", type: "datetime", writable: false},
                {header: "最后更新时间", field: "update_time", type: "datetime", writable: false}
            ]
        }).rest(rest).load();
    """.trimIndent()))
})