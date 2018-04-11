package com.heerkirov.bangumi.service

open class ServiceRuntimeException(message: String = ""): RuntimeException(message)

//拥有此主键的model没有找到。
class ModelWithPrimaryKeyNotFound(modelName: String, pkey: String): ServiceRuntimeException("Primary key '$pkey' in not found in model table '$modelName'.")
//此资源不提供给当前用户。
class UserForbidden(modelName: String, pkey: String): ServiceRuntimeException("Model '$modelName' with primary key '$pkey' is forbidden for current user.")
//值不能为null。
class NullValueError(location: String): ServiceRuntimeException("Field '$location' cannot be NULL.")