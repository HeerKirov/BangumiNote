package com.heerkirov.ktml.builder

/**这个类附加在HtmlBuilder中，为页面构建提供必须的常量信息。最常见的是提供URL的代理信息。
 * 类不需要传递给Builder，只需要设置为配置参数即可。
 */
open class ConstProxy(private val init: (ConstProxy.()->Unit)?=null) {
    //添加一条常规条目。
    fun dim(key: String, value: Any?) {
        lists.put(key, ProxyValue(value))
    }
    //添加一条URL。
    fun url(key: String, url: (Map<String, Any?>)->String) {
        lists.put(key, ProxyURL(url))
    }
    //查询所需要的条目。
    fun getItem(key: String, params: Map<String, Any?>? = null): Any? {
        if(initFlag){
            initFlag = false
            init?.invoke(this)
        }
        val ret = lists[key]
        if(ret!=null)return ret.getValue(params)
        else throw ProxyNotFoundException(key)
    }
    fun getString(key: String, params: Map<String, Any?>? = null): String? {
        return getItem(key, params) as String?
    }
    //初始化标记。
    private var initFlag: Boolean = true
    //列表。
    private val lists: HashMap<String, ProxyItem> = hashMapOf()
}
interface ProxyItem {
    fun getValue(params: Map<String, Any?>? = null): Any?
}
class ProxyValue(private val value: Any?) : ProxyItem {
    override fun getValue(params: Map<String, Any?>?): Any? {
        return value
    }
}
class ProxyURL(private val build: (Map<String, Any?>)->String) : ProxyItem {
    companion object {
        private val empty = HashMap<String, Any?>()
    }
    override fun getValue(params: Map<String, Any?>?): Any? {
        return build(params?:empty)
    }
}

class ProxyNotFoundException(name: String) : Exception("Proxy item '$name' is not found.")