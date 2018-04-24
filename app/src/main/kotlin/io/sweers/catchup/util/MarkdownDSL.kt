package io.sweers.catchup.util

interface Element {
  fun render(builder: StringBuilder)
}

class TextElement(val text: String) : Element {
  override fun render(builder: StringBuilder) {
    builder.append(text)
  }
}

abstract class MarkdownElement : Element {
  val children = arrayListOf<Element>()

  protected fun <T : Element> initTag(tag: T, init: T.() -> Unit): T {
    tag.init()
    children.add(tag)
    return tag
  }

  override fun render(builder: StringBuilder) {
    for (c in children) {
      c.render(builder)
    }
  }

  override fun toString(): String {
    val builder = StringBuilder()
    render(builder)
    return builder.toString()
  }
}

abstract class MarkerElement(val marker: String) : MarkdownElement() {
  override fun render(builder: StringBuilder) {
    builder.append(marker)
    for (c in children) {
      c.render(builder)
    }
    builder.append(marker)
  }
}

class LinkElement(val url: String, val text: String) : Element {
  override fun render(builder: StringBuilder) {
    builder.append("[")
        .append(text)
        .append("](")
        .append(url)
        .append(")")
  }
}

class CodeBlockElement(val text: String, val language: String = "") : Element {
  override fun render(builder: StringBuilder) {
    builder.append("```")
        .append(language)
        .append("\n")
        .append(text)
        .append("\n```")
  }
}

abstract class MarkerElementWithText(name: String) : MarkerElement(name) {
  operator fun String.unaryPlus() {
    children.add(TextElement(this))
  }
}

class Document : MarkdownElement() {
  operator fun String.unaryPlus() = text(this)
  fun text(text: String) = children.add(TextElement(text))
  fun space(count: Int = 1) = children.add(TextElement(" ".repeat(count)))
  fun newline(count: Int = 1) = children.add(TextElement("\n".repeat(count)))
  fun bold(init: Bold.() -> Unit) = initTag(Bold(), init)
  fun italic(init: Italic.() -> Unit) = initTag(Italic(), init)
  fun strikethrough(init: StrikeThrough.() -> Unit) = initTag(StrikeThrough(), init)
  fun code(init: Code.() -> Unit) = initTag(Code(), init)
  fun link(url: String, text: String) = children.add(LinkElement(url, text))
  fun codeBlock(text: String, language: String = "") = children.add(CodeBlockElement(text, language))
  fun h1(init: H1.() -> Unit) = initTag(H1(), init)
  fun h2(init: H2.() -> Unit) = initTag(H2(), init)
  fun h3(init: H3.() -> Unit) = initTag(H3(), init)
  fun h4(init: H4.() -> Unit) = initTag(H4(), init)
  fun h5(init: H5.() -> Unit) = initTag(H5(), init)
}

class Bold : MarkerElementWithText("*")
class Italic : MarkerElementWithText("_")
class StrikeThrough : MarkerElementWithText("~~")
class Code : MarkerElementWithText("`")
class H1 : MarkerElementWithText("#")
class H2 : MarkerElementWithText("##")
class H3 : MarkerElementWithText("###")
class H4 : MarkerElementWithText("####")
class H5 : MarkerElementWithText("#####")

fun buildMarkdown(init: Document.() -> Unit): Markdown {
  val markdown = Document()
  markdown.init()
  return markdown.toString().markdown()
}
