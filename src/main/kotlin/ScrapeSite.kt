package org.example

import kotlinx.coroutines.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.File

abstract class ScrapeSite {
    abstract val baseUrl: String
    abstract val pageRange: IntProgression
    abstract val articleTargetFolder: File
    abstract fun getRelevantURLs(url: String): List<String>
    abstract fun getArticleDate(doc: Document): String
    abstract fun getArticleTitle(doc: Document): String
    abstract fun getArticleText(doc: Document): String

    private val threadCount: Int = 12

    suspend fun srape() = coroutineScope {
        if (!articleTargetFolder.isDirectory) {
            throw InvalidDirectoryException("articles directory does not exist")
        }

        println("Preparing directory...")
        deleteAllFilesInDirectory(articleTargetFolder)

        println("Preparing batches...")
        val batches = getBatches()

        println("Allocating ${batches.size} workers..")

        // Allocate threads, give them hell
        val workers = List(batches.size) { index ->
            async(Dispatchers.IO) {
                workerJob(batches[index], index)
                println("Worker $index finished work!")
            }
        }
        workers.awaitAll()
        println("All URLs processed!")
    }

    private suspend fun workerJob(batch: List<Int>, workerIndex: Int) {
        batch.forEachIndexed { index, pageIndex ->
            val url = "$baseUrl/$pageIndex"

            val relevantURLs: List<String> = retryOperation(
                operationName = "Fetching Relevant URLs from $url"
            ) { getRelevantURLs(url) }

            relevantURLs.forEach {
                printArticle(it)
            }
            println("URL $url processed on thread ${Thread.currentThread().threadId()}!")
            println("Worker $workerIndex progress: ${index + 1}/${batch.size}")
        }
    }

    private suspend fun printArticle(url: String) {
        val doc: Document = retryOperation(
            operationName = "Fetching URL $url"
        ) { Jsoup.connect(url).get() }

        val title = getArticleTitle(doc)
        val date = getArticleDate(doc)

        val outputFile = File(articleTargetFolder, "$date - ${sanitizeFilename(title)}.txt")

        outputFile.printWriter().use { writer ->
            writer.println("URL: $url")
            writer.println("Datum: $date")
            writer.println("Titulek: $title")
            writer.println("Text:\n")
            writer.println(getArticleText(doc))
        }

        // Delay, so that the antispam doesn't get too mad (we are just casual users, nothing to see here haha, who is webscraping, do I know him? don't ban me again pls)
        delay(200)
    }

    private fun getBatches(): List<List<Int>> {
        val chunkSize = (pageRange.count() + 11) / threadCount
        return pageRange.chunked(chunkSize)
    }

    private suspend fun <T> retryOperation(
        maxTries: Int = 20,
        delayMillis: Long = 10000,
        operationName: String,
        operation: suspend () -> T
    ): T {
        var tries = maxTries
        while (tries > 0) {
            try {
                return operation()
            } catch (e: Exception) {
                tries--
                val timeout = delayMillis * (maxTries - tries)
                println("Failed to execute $operationName, exception: ${e.message}, retrying after ${timeout / 1000} s, $tries retries left.")
                // The antispam throwing a hissy fit, let's wait a bit till it chills out
                delay(timeout)
            }
        }
        // The antispam busted our ass, probably
        throw FetchException("No tries left for $operationName")
    }
}