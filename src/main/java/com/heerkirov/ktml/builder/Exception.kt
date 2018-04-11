package com.heerkirov.ktml.builder

class NeedParentException : RuntimeException("Html page need parent page.")
class NotImplementedException(blockName: Set<String>) : RuntimeException("Block ${blockName.joinToString()} is not implemented.") {
    constructor(name: String): this(setOf(name))
}
class BlockNotFoundException(blockName: Set<String>) : RuntimeException("Block define ${blockName.joinToString()} is not found in parents.") {
    constructor(name: String): this(setOf(name))
}