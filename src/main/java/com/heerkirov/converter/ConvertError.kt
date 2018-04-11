package com.heerkirov.converter

open class ConvertError(message: String = ""): RuntimeException(message)

//array转换器用的
class ConvertArrayNulError(index: Int): ConvertError("Item[$index] is null")

//model转换器用的
class ConvertFieldRequiredError(val fieldName: String): ConvertError("Field '$fieldName' is required but not exist")
class ConvertFieldNullError(val fieldName: String): ConvertError("Field '$fieldName' is null")
class ConvertFieldBlankError(val fieldName: String): ConvertError("Field '$fieldName' is blank")

//通用
class ConvertGoalTypeError(type: String = ""): ConvertError("Goal need type '$type'")
class ConvertTypeError(message: String = "") : ConvertError(message)
class ConvertActionError(message: String = "") : ConvertError(message)