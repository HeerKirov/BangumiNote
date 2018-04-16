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
class PasswordView(@Autowired p: ConstProxy): HtmlView(TemplateBasic::class, p, {
    impl("TITLE", text("修改密码 - ${proxyStr("val_logo")}"))
    impl("NAV_LIST", text("""
        {title: "个人资料", link: "${proxyURL("web_document")}"},
        {title: "修改密码", link: "${proxyURL("web_password")}"}
    """.trimIndent()))
    impl("SCRIPT", text("""
        var rest = restful.newdetail({url: "${proxyURL("api_user_password")}"});
        build_detail(${'$'}("#api-panel")).info({
            title: "修改密码",
            allowDelete: false,
            allowPartialUpdate: false,
            defaultState: "edit",
            updateUrl: "${proxyURL("web_document")}",
            content: [
                {header: "旧密码", field: "old_password", type: "password", typeInfo: {length: 128, allowBlank: false}},
                "hr",
                {header: "新密码", field: "new_password", type: "password", typeInfo: {length: 128, allowBlank: false}},
                {header: "确认密码", field: "check_password", type: "password", typeInfo: {length: 128, allowBlank: false}},
            ],
            validate: function(json) {
                if(json.new_password !== json.check_password) throw [{index: 3, msg: "两次输入的密码不一致。"}];
                else return { old_password: json.old_password, new_password: json.new_password };
            }
        }).rest(rest).load();
    """.trimIndent()))
})