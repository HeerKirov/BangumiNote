package com.heerkirov.bangumi.controller.base

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

enum class HttpKeyword {
    NONE,

    USER_EXISTS, //400
    NO_ENOUGH_INFORMATION, //400
    INFORMATION_FORMAT_WRONG, //400

    REGISTER_FORBIDDEN, //403
    AUTHENTICATED_FAILED, //403
    NOT_FOUND, //404
    METHOD_NOT_ALLOWED //405

}
fun HttpKeyword.toKeyword(): String {
    if(this == HttpKeyword.NONE)return ""
    else return this.toString()
}

open class HttpStatusException(message: String = "", val keyword: HttpKeyword) : RuntimeException(message)

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
class BadRequestException(message: String = "", keyword: HttpKeyword = HttpKeyword.NONE) : HttpStatusException(message, keyword)

@ResponseStatus(value = HttpStatus.FORBIDDEN)
class ForbiddenException(message: String = "Authenticated failed.", keyword: HttpKeyword = HttpKeyword.AUTHENTICATED_FAILED) : HttpStatusException(message, keyword)

@ResponseStatus(value = HttpStatus.NOT_FOUND)
class NotFoundException(message: String = "Resource is not found.", keyword: HttpKeyword = HttpKeyword.NOT_FOUND) : HttpStatusException(message, keyword)

@ResponseStatus(value = HttpStatus.METHOD_NOT_ALLOWED)
class MethodNotAllowedException(message: String = "Method is not allowed.", keyword: HttpKeyword = HttpKeyword.METHOD_NOT_ALLOWED) : HttpStatusException(message, keyword)