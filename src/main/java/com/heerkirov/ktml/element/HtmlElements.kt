package com.heerkirov.ktml.element

interface Clonable<out T> where T: Any {
    fun clone(): T
}
class TagList(): ArrayList<Tag>(), Clonable<TagList> {
    constructor(vararg tag: Tag): this() {
        for(i in tag) { this.add(i) }
    }
    override fun toString(): String {
        return this.joinToString("\n")
    }
    fun toMinString(): String {
        return this.joinToString("") {it.toMinString()}
    }
    override fun clone(): TagList {
        val ret = TagList()
        for(i in this)ret.add(i.clone())
        return ret
    }
}

class Attribute(var name: String, var value: String): Clonable<Attribute> {
    override fun toString(): String = """$name="$value""""
    override fun clone(): Attribute {
        return Attribute(name, value)
    }
}
open class Tag(var name: String, vararg attr: Pair<String, String>): Clonable<Tag> {
    val children: TagList = TagList()
    val attributes: MutableList<Attribute> by lazy { attr.map { Attribute(it.first, it.second) }.toMutableList() }
    override fun toString(): String = "<$name${if(attributes.isNotEmpty())attributes.joinToString(" ", " ")else ""}>" +
            "${if(children.isNotEmpty())"\n$children\n" else ""}</$name>"
    open fun toMinString(): String = "<$name${if(attributes.isNotEmpty())attributes.joinToString(" ", " ")else ""}>" +
            "${if(children.isNotEmpty())children.toMinString() else ""}</$name>"

    override fun clone(): Tag {
        val ret = Tag(name)
        ret.children.addAll(children)
        ret.attributes.addAll(attributes)
        return ret
    }
}

class Text(val text: String) : Tag("") {
    override fun toString(): String = text
    override fun toMinString(): String = text

    override fun clone(): Text {
        val ret = Text(name)
        ret.children.addAll(children)
        ret.attributes.addAll(attributes)
        return ret
    }
}

class Html5DocType : Tag("") {
    override fun toString(): String = "<!DOCTYPE html>"
    override fun toMinString(): String = toString()
}

class Html : Tag("html")
class Head : Tag("head")
class Meta : Tag("meta")
class Title : Tag("title")
class Link : Tag("link")

open class BodyElement(name: String) : Tag(name)
class Body : BodyElement("body")

class H1 : BodyElement("h1")
class H2 : BodyElement("h2")
class H3 : BodyElement("h3")
class H4 : BodyElement("h4")
class H5 : BodyElement("h5")
class H6 : BodyElement("h6")
class P : BodyElement("p")
class A : BodyElement("a")
class B : BodyElement("b")
class Label : BodyElement("label")
class I : BodyElement("i")

class Button : BodyElement("button")
class Input : BodyElement("input")

class Ul : BodyElement("ul")
class Li : BodyElement("li")

open class TableElement(name: String) : BodyElement(name)
class Table : BodyElement("table")
class Thead : TableElement("thead")
class Tbody : TableElement("tbody")
class Th : BodyElement("th")
class Tr : BodyElement("tr")
class Td : BodyElement("td")
class Nav : BodyElement("nav")
class Span : BodyElement("span")
class Div : BodyElement("div")

class Script : Tag("script")

class Strong : Tag("strong")

fun <T : Tag> Tag.doInit(tag: T, init: (T.() -> Unit)?): T {
    init?.invoke(tag)
    children.add(tag)
    return tag
}
fun <T : Tag> Tag.doInit2(tag: T, init: (T.() -> String)?): T {
    if(init!=null)tag.text(init(tag))
    children.add(tag)
    return tag
}
fun <T : Tag> T.set(name: String, value: String?): T {
    if (value != null) {
        attributes.add(Attribute(name, value))
    }
    return this
}

fun <T : Tag> T.set(vararg p: Pair<String, String?>): T {
    p.forEach { this.set(it.first, it.second) }
    return this
}

//直接性元素
fun html(lang: String = "zh", init: Html.() -> Unit): Html = Html().apply(init).set("lang", lang)
fun head(init: Head.() -> Unit) = Head().apply(init)
fun meta(name: String? = null, charset: String? = null, content: String? = null, init: Meta.() -> Unit) = Meta().apply(init).set("name" to name, "charset" to charset, "content" to content)
fun title(init: Title.() -> Unit) = Title().apply(init)
fun link(href: String? = null, rel: String = "stylesheet", init: Link.() -> Unit) = Link().apply(init).set("rel" to rel, "href" to href)
fun body(init: Body.() -> Unit) = Body().apply(init)
fun h1(init: H1.() -> Unit) = H1().apply(init)
fun h2(init: H2.() -> Unit) = H2().apply(init)
fun h3(init: H3.() -> Unit) = H3().apply(init)
fun h4(init: H4.() -> Unit) = H4().apply(init)
fun h5(init: H5.() -> Unit) = H5().apply(init)
fun h6(init: H6.() -> Unit) = H6().apply(init)
fun p(init: P.() -> Unit) = P().apply(init)
fun a(href: String="#", clazz: String?=null, dataToggle: String?=null, init: A.() -> Unit) = A().apply(init).set("href" to href, "class" to clazz, "data-toggle" to dataToggle)
fun i(clazz: String?=null, init: A.() -> Unit) = A().apply(init).set("class" to clazz)
fun b(clazz: String?=null, init: B.() -> Unit) = B().apply(init).set("class" to clazz)
fun label(clazz: String?=null, init: Label.() -> Unit) = Label().apply(init).set("class" to clazz)
fun button(id: String?=null, type: String="button", clazz: String?=null, dataToggle: String?=null, dataTarget: String?=null, init: Button.() -> Unit) = Button().apply(init).set("id" to id, "type" to type, "class" to clazz, "data-toggle" to dataToggle, "data-target" to dataTarget)
fun input(id: String?=null, name: String?=null, type: String="text", clazz: String?=null, maxlength: Int?=null, placeholder: String?=null) = Button().set("id" to id, "name" to name, "type" to type, "class" to clazz, "maxlength" to maxlength?.toString(), "placeholder" to placeholder)
fun ul(id: String?=null, clazz: String?=null, init: Ul.() -> Unit) = Ul().apply(init).set("id" to id, "class" to clazz)
fun li(id: String?=null, clazz: String?=null, init: Li.() -> Unit) = Li().apply(init).set("id" to id, "class" to clazz)
fun table(init: Table.() -> Unit) = Table().apply(init)
fun thead(init: Thead.() -> Unit) = Thead().apply(init)
fun tbody(init: Tbody.() -> Unit) = Tbody().apply(init)
fun tr(init: Tr.() -> Unit) = Tr().apply(init)
fun th(init: Th.() -> Unit) = Th().apply(init)
fun td(init: Td.() -> Unit) = Td().apply(init)
fun nav(id: String?=null, clazz: String?=null, init: Nav.() -> Unit) = Nav().apply(init).set("id" to id, "class" to clazz)
fun span(id: String?=null, clazz: String?=null, init: Span.() -> Unit) = Span().apply(init).set("id" to id, "class" to clazz)
fun div(id: String?=null, clazz: String?=null, init: Div.() -> Unit) = Div().apply(init).set("id" to id, "class" to clazz)
fun text(s: Any?) = Text(s.toString())
fun script(init: Script.() -> Unit) = Script().apply(init)
fun script_(init: Script.() -> String) = Script().also { it.children.add(text(init(it))) }
fun script(src: String) = Script().set("src", src)

//依附性元素
fun Html.head(init: (Head.() -> Unit)?=null) = doInit(Head(), init)
fun Head.meta(name: String? = null, charset: String? = null, content: String? = null, init: (Meta.() -> Unit)?=null) = doInit(Meta(), init).set("name" to name, "charset" to charset, "content" to content)
fun Head.title(init: (Title.() -> Unit)?=null) = doInit(Title(), init)
fun Head.title_(init: (Title.() -> String)?=null) = doInit2(Title(), init)
fun Head.link(href: String? = null, rel: String = "stylesheet", init: (Link.() -> Unit)?=null) = doInit(Link(), init).set("rel" to rel, "href" to href)

fun Html.body(init: (Body.() -> Unit)?=null) = doInit(Body(), init)
fun BodyElement.h1(clazz: String?=null, init: (H1.() -> Unit)?=null) = doInit(H1(), init).set("class" to clazz)
fun BodyElement.h2(clazz: String?=null, init: (H2.() -> Unit)?=null) = doInit(H2(), init).set("class" to clazz)
fun BodyElement.h3(clazz: String?=null, init: (H3.() -> Unit)?=null) = doInit(H3(), init).set("class" to clazz)
fun BodyElement.h4(clazz: String?=null, init: (H4.() -> Unit)?=null) = doInit(H4(), init).set("class" to clazz)
fun BodyElement.h5(clazz: String?=null, init: (H5.() -> Unit)?=null) = doInit(H5(), init).set("class" to clazz)
fun BodyElement.h6(clazz: String?=null, init: (H6.() -> Unit)?=null) = doInit(H6(), init).set("class" to clazz)
fun BodyElement.p(init: (P.() -> Unit)?) = doInit(P(), init)
fun BodyElement.a(id: String?=null, href: String="#", onclick: String?=null, clazz: String?=null, dataToggle: String?=null, dataTarget: String?=null, init: (A.() -> Unit)?=null) = doInit(A(), init).set("id" to id, "href" to href, "onclick" to onclick, "class" to clazz, "data-toggle" to dataToggle, "data-target" to dataTarget)
fun BodyElement.a_(id: String?=null, href: String="#", onclick: String?=null, clazz: String?=null, dataToggle: String?=null, dataTarget: String?=null, init: (A.() -> String)?=null) = doInit2(A(), init).set("id" to id, "href" to href, "onclick" to onclick, "class" to clazz, "data-toggle" to dataToggle, "data-target" to dataTarget)
fun BodyElement.b(clazz: String?=null, init: (B.() -> Unit)?=null) = doInit(B(), init).set("class" to clazz)
fun BodyElement.label(clazz: String?=null, forz: String?=null, init: (Label.() -> Unit)?=null) = doInit(Label(), init).set("class" to clazz, "for" to forz)
fun BodyElement.label_(clazz: String?=null, forz: String?=null, init: (Label.() -> String)?=null) = doInit2(Label(), init).set("class" to clazz, "for" to forz)
fun BodyElement.i(clazz: String?=null, init: (I.() -> Unit)?=null) = doInit(I(), init).set("class" to clazz)

fun BodyElement.input(id: String?=null, name: String?=null, type: String="text", clazz: String?=null, maxlength: Int?=null, placeholder: String?=null)
        = doInit(Input(), {}).set("id" to id, "name" to name, "type" to type, "class" to clazz, "maxlength" to maxlength?.toString(), "placeholder" to placeholder)
fun BodyElement.button(id: String?=null, type: String="button", clazz: String?=null, dataToggle: String?=null, dataTarget: String?=null, dataDismiss: String?=null, onclick: String?=null, init: (Button.() -> Unit)?=null)
        = doInit(Button(), init).set("id" to id, "type" to type, "class" to clazz, "data-toggle" to dataToggle, "data-target" to dataTarget, "data-dismiss" to dataDismiss, "onclick" to onclick)
fun BodyElement.button_(id: String?=null, type: String="button", clazz: String?=null, dataToggle: String?=null, dataTarget: String?=null, dataDismiss: String?=null, onclick: String?=null, init: (Button.() -> String)?=null)
        = doInit2(Button(), init).set("id" to id, "type" to type, "class" to clazz, "data-toggle" to dataToggle, "data-target" to dataTarget, "data-dismiss" to dataDismiss, "onclick" to onclick)

fun BodyElement.ul(id: String?=null, clazz: String?=null, init: (Ul.() -> Unit)?=null) = doInit(Ul(), init).set("id" to id, "class" to clazz)
fun Ul.li(id: String?=null, clazz: String?=null, init: (Li.() -> Unit)?=null) = doInit(Li(), init).set("id" to id, "class" to clazz)

fun BodyElement.table(init: (Table.() -> Unit)?=null) = doInit(Table(), init)
fun Table.thead(init: (Thead.() -> Unit)?=null) = doInit(Thead(), init)
fun Table.tbody(init: (Tbody.() -> Unit)?=null) = doInit(Tbody(), init)
fun TableElement.tr(init: (Tr.() -> Unit)?=null) = doInit(Tr(), init)
fun Tr.th(init: (Th.() -> Unit)?=null) = doInit(Th(), init)
fun Tr.td(init: (Td.() -> Unit)?=null) = doInit(Td(), init)


fun BodyElement.nav(id: String?=null, clazz: String?=null, style: String?=null, init: (Nav.() -> Unit)?=null)
        = doInit(Nav(), init).set("id" to id, "class" to clazz, "style" to style)
fun BodyElement.span(id: String?=null, clazz: String?=null, style: String?=null, init: (Span.() -> Unit)?=null)
        = doInit(Span(), init).set("id" to id, "class" to clazz, "style" to style)
fun BodyElement.div(id: String?=null, clazz: String?=null, style: String?=null, init: (Div.() -> Unit)?=null)
        = doInit(Div(), init).set("id" to id, "class" to clazz, "style" to style)



fun Tag.text(s: Any?) = doInit(Text(s.toString()), {})

fun BodyElement.strong(init: (Strong.() -> Unit)?=null) = doInit(Strong(), init)
fun BodyElement.strong_(init: Strong.() -> String) = doInit2(Strong(), init)

fun Tag.script(init: Script.() -> Unit) = doInit(Script(), init)
fun Tag.script_(init: Script.() -> String) = doInit2(Script(), init)
fun Tag.script(src: String) = doInit(Script(), {this.set("src", src)})