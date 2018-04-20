package com.heerkirov.ktml.builder

import com.heerkirov.ktml.element.*
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance
/**TODO 新的重构内容
    现在内容的生成模式为：
        获取上一级页面的内容 & 块引用
        调用initialize - 调用init构造 - 填充上一级的块 - DFS搜索block块引用 - 连同内容传递给下一级。
    这是自上至下的先序递归，有很多缺点。每一级的内容和块都要传递给下一级并被下一级改变，这对于上一级来说是不安全的。
    同时，每次调用都要生成一遍，这很不利于缓存操作。

    也许可以考虑吧递归顺序换过来。
        先构造本级内容同时搜索块，然后传递给上一级。上一级在构造块的同时填充块。
        这样第一免去二次搜索块位置的问题，二是对缓存有利。上级是不会修改下级传递的资源内容的，这对缓存安全。
 */
/**这定义了一个次级页面。次级页面的实现必须依赖顶级。
 */
open class HtmlView(val parent: KClass<*>?, val init: HtmlView.()->Unit) {
    constructor(): this(null, {})
    constructor(parent: KClass<*>, proxy: ConstProxy?, init: HtmlView.()->Unit): this(parent, init) {
        setProxy(proxy)
    }
    var blockImpl: HashMap<String, TagList>? = null //在initialize时用于中间转移的map结构。
    protected var notImplBlocksCache: HashMap<String, Block>? = null //本层级内所有未被实现的block的列表的缓存。

    private var parentObject: HtmlView? = null

    //这个函数将结合attrs执行init，获得Ktml结构。
    protected open fun initialize(): Map<String, TagList> {
        blockImpl = HashMap()//构建接收器

        this.init() //执行初始化，使接收器接收内容。
        //if(notImplBlocksCache == null) {//这个缓存不会产生变化，因此只需要第一次执行时跑一次。
            notImplBlocksCache = buildImplBlockCache()
        //}

        setAttributes(null) //清空参数列表，防止下一次用了
        val ret = blockImpl //接收器转移
        blockImpl = null
        return ret!!
    }
    protected open fun buildImplBlockCache(): HashMap<String, Block> {
        val ret = HashMap<String, Block>()
        blockImpl!!.forEach { _, tags -> tags.forEach { it.getBlocks().forEach { ret.put(it.blockName, it) } } }
        return ret
    }

    /**综合函数。将attr和toHtml和toString的功能都加在一起。
     */
    fun build(attrs: Map<String, Any?>? = null): String {
        return this.setAttributes(attrs).toHtml().toMinString()
    }
    /**这将返回一个去掉所有块的html结构。
     * 所有的块都会被自动搜索解决掉。
     */
    fun toHtml(): TagList {
        val (tags, p) = toHtmlFunction()
        if(p.count { !it.value.allowBlank } > 0)throw NotImplementedException(p.keys)
        return tags
    }
    /**在toHtml内递归执行。将获得经过检查的Ktml结构。
     */
    protected open fun toHtmlFunction(): HtmlReturn {
        if(parentObject==null){
            if(parent!=null){
                parentObject = parent.createInstance() as HtmlView
                parentObject!!.setProxy(constProxy)
            }else throw NeedParentException()
        }
        val (tags, p) = parentObject!!.setAttributes(usingAttrs).toHtmlFunction() //上级页面的实现。
        val blockImplements = initialize()  //执行本级页面的初始化，获得Ktml。
        //本级要做的就是用自己的impl去填充上级页面的实现内的块。
        blockImplements.forEach { name, blocks ->
            //这会遍历本级页面内拥有的实现。
            if(p.containsKey(name)) { //这意味着本级页面内包含一个对parent.block的实现
                p.remove(name)!!.children.addAll(blocks) //这会在为块添加实现的同时从列表内移除这个块的占位符。
            }
        }
        //执行完替换之后，将自己未实现的block也加入。
        val notImpl = notImplBlocksCache //线程安全。虽然好像并没有
        if(notImpl!=null)p.putAll(notImpl)
        return HtmlReturn(tags, p)
    }

    //处理proxy的部分。proxy是一次设置终生使用的。
    fun setProxy(proxy: ConstProxy?): HtmlView {
        if(proxy!=null)constProxy = proxy
        return this
    }
    private var constProxy: ConstProxy? = null
    fun proxyDim(key: String): Any? {
        return constProxy?.getItem(key, null)
    }
    fun proxyStr(key: String): String {
        return constProxy?.getItem(key, null).toString()
    }
    fun proxyURL(key: String): String {
        return constProxy?.getItem(key, null)?.toString()?:""
    }
    fun proxyURL(key: String, params: Map<String, Any?>): String {
        return constProxy?.getItem(key, params)?.toString()?:""
    }
    fun proxyURL(key: String, vararg params: Pair<String, Any?>): String {
        if(params.isNotEmpty())return constProxy?.getItem(key, params.associate { it })?.toString()?:""
        else return constProxy?.getItem(key, null)?.toString()?:""
    }
    //处理attr的部分。
    fun setAttributes(newAttr: Map<String, Any?>?): HtmlView {
        usingAttrs = newAttr
        return this
    }
    private var usingAttrs: Map<String, Any?>? = null //在处理之前，需要获得参数列表。
    fun hasAttr(name: String): Boolean {
        return usingAttrs?.containsKey(name)?:false
    }
    fun<T> attrAs(name: String): T where T: Any {
        return usingAttrs!![name]!! as T
    }
    fun attr(name: String): Any {
        return usingAttrs!![name]!!
    }
    fun<T> attrSafeAs(name: String): T? where T: Any{
        return usingAttrs?.get(name)?.let { it as T }
    }
    fun attrSafe(name: String): Any?{
        return usingAttrs?.get(name)
    }
}
/**这也是一个页面，但是实现了缓存功能。
 * 基于该类派生的页面，它们产生的Ktml结构只会被构造一次且会被缓存，因此你不能在它们的页面结构内加入任何动态内容。
 */
open class HtmlCacheView(parent: KClass<*>?, init: HtmlView.()->Unit) : HtmlView(parent, init) {
    constructor(): this(null, {})
    constructor(parent: KClass<*>, proxy: ConstProxy?, init: HtmlView.()->Unit): this(parent, init) {
        setProxy(proxy)
    }
    private var cache: Map<String, TagList>? = null
    override fun initialize(): Map<String, TagList> {
        if(cache==null)cache = super.initialize()
        return cache!!.clone()
    }
}
/**这定义了一个顶级页面。
 */
open class HtmlTopView(val root: HtmlTopView.()->Unit): HtmlView(null, {}) {
    //顶层元素的所有元素的顺序表。
    var tagList: TagList? = null

    override fun initialize(): Map<String, TagList> {
        tagList = TagList(Html5DocType())
        this.root()
        //if(notImplBlocksCache == null) {//这个缓存不会产生变化，因此只需要第一次执行时跑一次。
            notImplBlocksCache = buildImplBlockCache()
        //}
        setAttributes(null) //清空参数列表，防止下一次用了
        val ret = mapOf("default" to tagList!!) //接收器转移
        tagList = null
        return ret
    }
    override fun buildImplBlockCache(): HashMap<String, Block> {
        val ret = HashMap<String, Block>()
        tagList!!.forEach { it.getBlocks().forEach { ret.put(it.blockName, it) } }
        return ret
    }
    override fun toHtmlFunction(): HtmlReturn {
        val tags = initialize()["default"]!!
        return HtmlReturn(tags, notImplBlocksCache!!)
    }
}
/**这也是一个顶级页面，但是实现了缓存功能。
 */
open class HtmlCacheTopView(root: HtmlTopView.()->Unit): HtmlTopView(root) {
    private var cache: Map<String, TagList>? = null
    override fun initialize(): Map<String, TagList> {
        if(cache==null)cache = super.initialize()
        return cache!!.clone()
    }
}
/**在Html页面内部交换信息用的。
 */
class HtmlReturn(val tag: TagList, private val blocks: HashMap<String, Block>) {
    operator fun component1(): TagList = tag
    operator fun component2(): HashMap<String, Block> = blocks
}

/** block块的定义并使用标签。这个块的内容会被替换为实现。
 */
class Block(val blockName: String, val allowBlank: Boolean = false) : Tag("") {
    override fun toString(): String {
        if(children.isNotEmpty())return children.toString()
        else if(allowBlank) return ""
        else throw NotImplementedException(blockName)
    }
    override fun toMinString(): String {
        if(children.isNotEmpty())return children.toMinString()
        else if(allowBlank) return ""
        else throw NotImplementedException(blockName)
    }

    override fun clone(): Block {
        val ret = Block(blockName, allowBlank)
        ret.children.addAll(children)
        ret.attributes.addAll(attributes)
        return ret
    }
}

//这个函数用于在HtmlView的初始化构建中方便地添加新的块。
fun HtmlView.impl(blockName: String, vararg tags: Tag) {
    this.blockImpl!!.put(blockName, TagList().also { it.addAll(tags) })
}
//这个函数用于在HtmlTopView的初始化构建中添加顶级元素HTML。
fun HtmlTopView.doc(lang: String = "zh", init: Html.() -> Unit) = this.tagList!!.add(Html().apply(init).set("lang", lang))

//这个函数像其他标签一样在html结构内添加了一个抽象块。
fun Tag.block(blockName: String, allowBlank: Boolean = false) = doInit(Block(blockName, allowBlank), {})
//这个函数将搜索Tag内的children，并获得所有树中Block的引用。
fun Tag.getBlocks(): Set<Block> {
    val ret = HashSet<Block>()
    if(this is Block) ret.add(this)
    else this.children.forEach {
        if(it is Block)ret.add(it)
        else if(it.children.isNotEmpty())ret.addAll(it.getBlocks())
    }
    return ret
}

fun<V> Map<String, V>.clone(): Map<String, V> where V: Clonable<V> {
    val ret = HashMap<String, V>()
    for((k, v) in this)ret.put(k, v.clone())
    return ret
}