package com.example.inkpaperdiary.ui.reader

/**
 * 轻量 Markdown 块解析器 — 把日记正文解析为"书页元素"。
 *
 * 不追求完整的 Markdown 规范，只支持日记写作中真正会用到的结构：
 * 标题、段落、引用、列表、待办、分割线、插图。
 * 行内格式（粗体/斜体/代码）在渲染层处理。
 */
sealed class MdBlock {
    data class Heading(val level: Int, val text: String) : MdBlock()
    data class Paragraph(val text: String) : MdBlock()
    data class Quote(val lines: List<String>) : MdBlock()
    data class ListItem(val marker: String, val text: String, val done: Boolean? = null) : MdBlock()
    data object Divider : MdBlock()
    data class Image(val src: String) : MdBlock()
}

object MdParser {

    private val imagePattern = Regex("""^!\[[^\]]*\]\(([^)]+)\)\s*$""")
    private val todoPattern = Regex("""^[-*]\s\[( |x|X)\]\s+(.*)$""")
    private val bulletPattern = Regex("""^[-*]\s+(.*)$""")
    private val numberedPattern = Regex("""^(\d+)[.、]\s*(.*)$""")

    /** 逐行解析为块序列；连续普通行合并为段落。 */
    fun parse(markdown: String): List<MdBlock> {
        val blocks = mutableListOf<MdBlock>()
        val paragraphBuffer = mutableListOf<String>()

        fun flushParagraph() {
            if (paragraphBuffer.isNotEmpty()) {
                blocks += MdBlock.Paragraph(paragraphBuffer.joinToString("\n"))
                paragraphBuffer.clear()
            }
        }

        val lines = markdown.replace("\r\n", "\n").split("\n")
        var i = 0
        while (i < lines.size) {
            val raw = lines[i]
            val line = raw.trimEnd()
            val trimmed = line.trim()

            when {
                // 空行 → 段落结束
                trimmed.isEmpty() -> {
                    flushParagraph()
                }

                // 分割线
                trimmed == "---" || trimmed == "***" || trimmed == "___" -> {
                    flushParagraph()
                    blocks += MdBlock.Divider
                }

                // 插图（独占一行）
                imagePattern.matches(trimmed) -> {
                    flushParagraph()
                    blocks += MdBlock.Image(imagePattern.find(trimmed)!!.groupValues[1])
                }

                // 标题
                trimmed.startsWith("#") -> {
                    flushParagraph()
                    val level = trimmed.takeWhile { it == '#' }.length.coerceIn(1, 3)
                    val text = trimmed.dropWhile { it == '#' }.trim()
                    if (text.isNotEmpty()) blocks += MdBlock.Heading(level, text)
                }

                // 引用（连续行合并）
                trimmed.startsWith(">") -> {
                    flushParagraph()
                    val quoteLines = mutableListOf<String>()
                    while (i < lines.size && lines[i].trim().startsWith(">")) {
                        quoteLines += lines[i].trim().removePrefix(">").trim()
                        i++
                    }
                    i--
                    blocks += MdBlock.Quote(quoteLines)
                }

                // 待办
                todoPattern.matches(trimmed) -> {
                    flushParagraph()
                    val m = todoPattern.find(trimmed)!!
                    blocks += MdBlock.ListItem(
                        marker = "",
                        text = m.groupValues[2],
                        done = m.groupValues[1].equals("x", ignoreCase = true)
                    )
                }

                // 无序列表
                bulletPattern.matches(trimmed) -> {
                    flushParagraph()
                    blocks += MdBlock.ListItem(marker = "·", text = bulletPattern.find(trimmed)!!.groupValues[1])
                }

                // 有序列表
                numberedPattern.matches(trimmed) -> {
                    flushParagraph()
                    val m = numberedPattern.find(trimmed)!!
                    blocks += MdBlock.ListItem(marker = "${m.groupValues[1]}.", text = m.groupValues[2])
                }

                // 普通行 → 缓冲
                else -> {
                    paragraphBuffer += line
                }
            }
            i++
        }
        flushParagraph()
        return blocks
    }
}
