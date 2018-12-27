package th.co.digio.chatapp.demo.utils

import android.os.AsyncTask
import android.util.Patterns
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.IOException
import java.util.*

/**
 * A class with static util methods.
 */

object WebUtils {

    /**
     * Extract urls from string.
     * @param input
     * @return
     */
    fun extractUrls(input: String): List<String> {
        val result = ArrayList<String>()
        val words = input.split("\\s+".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        var word: String
        val pattern = Patterns.WEB_URL

        for (w in words) {
            word = w
            if (pattern.matcher(word).find()) {
                if (!word.toLowerCase().contains("http://") && !word.toLowerCase().contains("https://")) {
                    word = "http://$word"
                }
                result.add(word)
            }
        }

        return result
    }

    /**
     * Scrap page information of given URL.
     *
     * ScrapInfo will contain below information.
     *
     * site_name
     * title
     * description
     * image
     * url
     */
    abstract class UrlPreviewAsyncTask : AsyncTask<String, Void, UrlPreviewInfo>() {
        private val TIMEOUT_MILLIS = 10 * 1000

        abstract override fun onPostExecute(info: UrlPreviewInfo)

        override fun doInBackground(vararg params: String): UrlPreviewInfo? {
            val result = Hashtable<String, String>()
            val url = params[0]
            var doc: Document? = null
            try {
                doc = Jsoup.connect(url).followRedirects(true).timeout(TIMEOUT_MILLIS).get()

                var ogTags = doc!!.select("meta[property^=og:]")
                for (i in ogTags.indices) {
                    val tag = ogTags[i]

                    val text = tag.attr("property")
                    when (text) {
                        "og:image" -> result["image"] = tag.attr("content")
                        "og:description" -> result["description"] = tag.attr("content")
                        "og:title" -> result["title"] = tag.attr("content")
                        "og:site_name" -> result["site_name"] = tag.attr("content")
                        "og:url" -> result["url"] = tag.attr("content")
                    }
                }

                ogTags = doc.select("meta[property^=twitter:]")
                for (i in ogTags.indices) {
                    val tag = ogTags[i]

                    val text = tag.attr("property")
                    if ("twitter:image" == text) {
                        if (!result.containsKey("image")) {
                            result["image"] = tag.attr("content")
                        }
                    } else if ("twitter:description" == text) {
                        if (!result.containsKey("description")) {
                            result["description"] = tag.attr("content")
                        }
                    } else if ("twitter:title" == text) {
                        if (!result.containsKey("title")) {
                            result["title"] = tag.attr("content")
                        }
                    } else if ("twitter:site" == text) {
                        if (!result.containsKey("site_name")) {
                            result["site_name"] = tag.attr("content")
                        }
                    } else if ("twitter:url" == text) {
                        if (!result.containsKey("url")) {
                            result["url"] = tag.attr("content")
                        }
                    }
                }

                if (!result.containsKey("site_name")) {
                    result["site_name"] = result["title"]
                }

                if (!result.containsKey("url")) {
                    result["url"] = url
                }

                if (result["image"] != null && result["image"]!!.startsWith("//")) {
                    result["image"] = "http:" + result["image"]!!
                }

                if (result["url"] != null && result["url"]!!.startsWith("//")) {
                    result["url"] = "http:" + result["url"]!!
                }

                /**
                 * site_name, title, image, description, url
                 */
                if (result.keys.size == 5) {
                    return UrlPreviewInfo(
                            result["url"]!!,
                            result["site_name"]!!,
                            result["title"]!!,
                            result["description"]!!,
                            result["image"]!!
                    )
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }

            return null
        }
    }

}// This class should not be initialized
