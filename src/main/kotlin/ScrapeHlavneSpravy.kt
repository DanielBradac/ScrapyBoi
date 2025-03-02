package org.example

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class ScrapeHlavneSpravy : ScrapeSite() {
    override val baseUrl = "https://www.hlavnespravy.sk/category/z_domova/page"
    override val pageRange = 873 downTo 663
    override val articleTargetFolder = File("articlesHS")


    override fun getRelevantURLs(url: String): List<String> {
        val urls = mutableListOf<String>()
        val doc: Document = Jsoup.connect(url).get()
        val blockDivs: List<Element> = doc.select("div.block")

        blockDivs.forEach { blockDiv ->
            val articlesDiv =
                blockDiv.select("div.articles").firstOrNull() ?: throw MalformedPageException("No articles on url $url")
            val articleDivs =
                articlesDiv.select("div.top-item-wrapper-text")
                    ?: throw MalformedPageException("No articleDivs on url $url")
            if (articleDivs.isEmpty()) {
                return urls
            }

            for (div in articleDivs) {
                val headline = div.select("h2.custom-blue").firstOrNull()
                    ?: throw MalformedPageException("Malformed headline on url $url")
                val href = headline.parent()
                urls.add(
                    href?.attribute("href")?.value
                        ?: throw MalformedPageException("Cannot get href for headline ${headline.text()} on url $url")
                )
            }
        }

        return urls
    }

    override fun getArticleDate(doc: Document): String {
        val dateText = doc.select("div.article-info").firstOrNull()?.text()?.split("|")?.get(0)?.trim()
            ?: throw MalformedPageException("No article info")

        val inputFormat = SimpleDateFormat("dd. MM. yyyy", Locale.getDefault())
        val outputFormat = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())

        val date = inputFormat.parse(dateText)
        return outputFormat.format(date)
    }

    override fun getArticleTitle(doc: Document): String {
        return doc.select("h1.main-title").text()
    }

    override fun getArticleText(doc: Document): String {
        val textTop = getTextContent(doc.select("div.article-top").first())
        val textMain = getTextContent(doc.select("div.article-content").first())

        return textTop + textMain
    }

    private fun getTextContent(content: Element?): String {
        var text = ""
        content?.children()?.forEach { element ->
            if (element.tagName() in listOf("p", "h1", "h2", "h3", "h4", "h5", "h6")) {
                text += "${element.text()}\n\n"
            }
        }
        return text
    }

}

suspend fun main() {
    ScrapeHlavneSpravy().srape()
}