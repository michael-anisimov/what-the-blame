package com.github.michaelanisimov.whattheblame.toolwindow

import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.data.MutableDataSet
import javax.swing.JEditorPane

class MarkdownRenderer(private val pane: JEditorPane) {

    private val parser: Parser
    private val renderer: HtmlRenderer
    private val buffer = StringBuilder()
    private var rawMarkdown: String = ""

    init {
        val options = MutableDataSet()
        parser = Parser.builder(options).build()
        renderer = HtmlRenderer.builder(options).build()
    }

    fun reset() {
        buffer.setLength(0)
        rawMarkdown = ""
        pane.text = ""
    }

    fun setText(markdown: String) {
        buffer.setLength(0)
        buffer.append(markdown)
        rerender()
    }

    fun appendDelta(text: String) {
        buffer.append(text)
        rerender()
    }

    val markdown: String get() = rawMarkdown

    private fun rerender() {
        rawMarkdown = buffer.toString()
        val html = renderer.render(parser.parse(rawMarkdown))
        pane.text = "<html><body style='font-family:sans-serif;'>$html</body></html>"
        pane.caretPosition = 0
    }
}
