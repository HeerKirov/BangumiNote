package com.heerkirov.bangumi.controller.api

import com.heerkirov.bangumi.controller.base.ApiController
import com.heerkirov.bangumi.controller.converter.ModelConverter
import com.heerkirov.bangumi.model.Message
import com.heerkirov.bangumi.service.MessageService
import com.heerkirov.bangumi.service.Security
import com.heerkirov.converter.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import javax.servlet.http.HttpServletRequest

@Controller
@RequestMapping("/api/message")
class MessageApi(@Autowired private val httpServletRequest: HttpServletRequest,
                 @Autowired private val security: Security,
                 @Autowired private val messageService: MessageService): ApiController() {
    override fun request() = httpServletRequest
    override fun security() = security

    @RequestMapping("/exist", method = [RequestMethod.GET])
    fun existsMessage() = view(auth(true)) {
        val user = security.currentUser()!!
        val ret = messageService.existAnyMessage(user)
        mapOf("exist" to ret)
    }
    @RequestMapping("/unread_messages", method = [RequestMethod.GET])
    fun unreadMessage() = view(auth(true)) {
        val user = security.currentUser()!!
        val ret = messageService.unreadAndSetMessages(user)
        mapOf("content" to listConverter.parse(ret))
    }
    private val converter: ModelConverter<Message> = ModelConverter(Message::class, arrayOf<ModelConverter.Field<*, *>>(
            ModelConverter.Field("id", allowToObject = false, converter = IntConverter()),
            ModelConverter.Field("uid", allowToObject = false, converter = IntConverter()),

            ModelConverter.Field("type", allowToObject = false, converter = StringConverter()),
            ModelConverter.Field("content", allowToObject = false, converter = Converter(HashMap::class)),
            ModelConverter.Field("createTime", jsonName = "create_time", allowToObject = false, converter = DateTimeConverter()),
            ModelConverter.Field("updateTime", jsonName = "update_time", allowToObject = false, converter = DateTimeConverter())
    ))
    private val listConverter = ListConverter(Message::class, converter = converter)
}