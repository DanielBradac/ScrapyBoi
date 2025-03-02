package org.example

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.io.File

class ScrapeHlavnyDennik : ScrapeSite() {
    override val baseUrl = "https://www.hlavnydennik.sk/kategoria/slovensko"
    override val pageRange = 1590 downTo 1197
    override val articleTargetFolder = File("articlesHD")

    override fun getRelevantURLs(url: String): List<String> {
        val doc: Document = Jsoup.connect(url).get()
        val links: List<Element> = doc.select("a.post.with-image.vue.blue")
        return links.map { it.attr("href") }
    }

    override fun getArticleDate(doc: Document): String {
        val url = doc.baseUri()

        val regex = Regex("""/(\d{4})/(\d{2})/(\d{2})/""")
        val matchResult = regex.find(url)

        return if (matchResult != null) {
            val (year, month, day) = matchResult.destructured
            "$year.$month.$day"
        } else {
            "unknown_date"
        }
    }

    override fun getArticleTitle(doc: Document): String {
        return doc.title().removeSuffix(" | Hlavný Denník")
    }

    override fun getArticleText(doc: Document): String {
        var text = ""
        val contents = doc.select("div.contents").first()
        contents?.children()?.forEach { element ->
            if (element.tagName() in listOf("p", "h1", "h2", "h3", "h4", "h5", "h6")) {
                text += "${element.text()}\n\n"
            }
        }

        return text
    }
}

suspend fun main() {
    ScrapeHlavnyDennik().srape()
}

