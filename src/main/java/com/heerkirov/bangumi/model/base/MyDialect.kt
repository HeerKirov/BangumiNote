package com.heerkirov.bangumi.model.base

import org.hibernate.dialect.PostgreSQL95Dialect

class MyDialect : PostgreSQL95Dialect() {
    init {
        registerColumnType(2000, "json")
    }
}