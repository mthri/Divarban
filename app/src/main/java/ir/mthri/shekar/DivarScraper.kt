package ir.mthri.shekar
import okhttp3.OkHttpClient
import okhttp3.Request
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

data class DivarPost(
    val name: String,
    val url: String
)

class DivarScraper {

    private val client = OkHttpClient()
    private val gson = Gson()

    suspend fun fetchDivarPosts(targetUrl: String): List<DivarPost> {
        return withContext(Dispatchers.IO) {
            var postsData: List<DivarPost> = emptyList()

            try {
                val request = Request.Builder()
                    .url(targetUrl)
                    .header("Accept", "application/json, text/plain, */*")
                    .header("Accept-Language", "en-GB,en;q=0.9,fa-IR;q=0.8,fa;q=0.7,en-US;q=0.6")
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/136.0.0.0 Safari/537.36")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        logE("DivarScraper", "Error fetching Divar posts: ${response.code} ${response.message}")
                        return@withContext emptyList()
                    }

                    val responseBody = response.body?.string() ?: ""
                    if (responseBody.isEmpty()) {
                        logE("DivarScraper", "Empty response body.")
                        return@withContext emptyList()
                    }

                    val scriptElements = responseBody.split("<script type=\"application/ld+json\">")
                    if (scriptElements.size == 1) {
                        logI("DivarScraper", "Returning empty list")
                        return@withContext emptyList()
                    }

                    val scriptContent = scriptElements.last()
                    val jsonData = scriptContent.split("</script>").first()

                    if (jsonData.isEmpty()) {
                        logE("DivarScraper", "JSON data not found or empty in script tag.")
                        return@withContext emptyList()
                    }

                    val postListType = object : TypeToken<List<DivarPost>>() {}.type
                    postsData = gson.fromJson(jsonData, postListType)

                }
            } catch (e: IOException) {
                logE("DivarScraper", "Network Error fetching Divar posts: ${e.message}")
            } catch (e: Exception) {
                logE("DivarScraper", "Error fetching Divar posts: ${e.message}")
            }
            return@withContext postsData
        }
    }
}