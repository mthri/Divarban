package ir.mthri.shekar
import okhttp3.OkHttpClient
import okhttp3.Request
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import android.util.Log

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
                        Log.e("DivarScraper", "Error fetching Divar posts: ${response.code} ${response.message}")
                        return@withContext emptyList()
                    }

                    val responseBody = response.body?.string() ?: ""
                    if (responseBody.isEmpty()) {
                        Log.e("DivarScraper", "Empty response body.")
                        return@withContext emptyList()
                    }

                    val scriptElement = responseBody.split("<script type=\"application/ld+json\">").last()
                    val jsonData = scriptElement.split("</script>").first()

                    if (jsonData.isEmpty()) {
                        Log.e("DivarScraper", "JSON data not found or empty in script tag.")
                        return@withContext emptyList()
                    }

                    val postListType = object : TypeToken<List<DivarPost>>() {}.type
                    postsData = gson.fromJson(jsonData, postListType)

                }
            } catch (e: IOException) {
                Log.e("DivarScraper", "Network Error fetching Divar posts: ${e.message}", e)
            } catch (e: Exception) {
                Log.e("DivarScraper", "Error fetching Divar posts: ${e.message}", e)
            }
            return@withContext postsData
        }
    }
}