package ir.mthri.shekar

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*


class ScrapingService : Service() {
    private var scrapingJob: Job? = null
    private val DIVAR_STORAGE = "DS"
    private val CHANNEL_POST_ID = "post_channel"

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val url = intent?.getStringExtra("url") ?: return START_NOT_STICKY
        val interval = intent.getLongExtra("interval", 30L)

        logI("ScrapingService", "Background service started.")

        startForegroundNotification()
        scrapingJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                if (isInternetAvailable()) {
                    runScraping(url)
                    logD("ScrapingService", "Scraping done")
                } else {
                    logD("ScrapingService", "No internet")
                }
                delay(interval * 1000)
            }
        }
        return START_STICKY
    }

    private fun startForegroundNotification() {
        val channelId = "scraping_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Scraping", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("در حال بررسی آگهی‌ها")
            .setContentText("ربات دیواربان فعال است")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()

        startForeground(1, notification)
    }

    private fun isInternetAvailable(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private suspend fun runScraping(targetUrl: String) {
        logI("runScraping", "Executing scraping logic for URL: $targetUrl")
        val sharedPref = getSharedPreferences(DIVAR_STORAGE, Context.MODE_PRIVATE)

        val existingTokens = sharedPref.getStringSet("tokens", emptySet())?.toMutableSet() ?: mutableSetOf()
        var firstRun = false

        if (existingTokens.isEmpty()){
            firstRun = true
            logI("runScraping", "First run detected. Initializing tokens.")
        } else {
            logI("runScraping", "Loaded ${existingTokens.size} existing tokens.")
        }

        val newPosts = DivarScraper().fetchDivarPosts(targetUrl)

        if (newPosts.isNotEmpty()) {
            var changed = false

            for (post in newPosts) {
                val token = post.url.split("/").last()
                if (existingTokens.add(token)) {
                    if(!firstRun){
                        logD("runScraping", "New post token found: $token. Sending notification.")
                        sendNewPostNotification(post.name, post.url)
                    } else {
                        logI("runScraping", "First run, adding token $token without notifying.")
                    }
                    changed = true
                } else {
                   // logI("runScraping", "Post token $token already exists.")
                }
            }

            if (changed) {
                logI("runScraping", "Changes detected. Saving updated tokens (${existingTokens.size})...")
                with(sharedPref.edit()) {
                    putStringSet("tokens", HashSet(existingTokens))
                    apply()
                }
                logD("runScraping", "Tokens saved.")
            } else {
                logI("runScraping", "No new posts found or tokens already exist.")
            }
        } else {
            logI("runScraping", "fetchDivarPosts returned an empty list.")
        }
        logI("runScraping", "Scraping logic execution finished.")
    }

    private fun sendNewPostNotification(text: String, url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_POST_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("آگهی جدید")
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = "اعلان‌های برنامه"
            val channelDescription = "اعلان‌های مربوط به آگهی‌های جدید"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = android.app.NotificationChannel(CHANNEL_POST_ID, channelName, importance)
            channel.description = channelDescription
            notificationManager.createNotificationChannel(channel)
        }

        val id = System.currentTimeMillis().toInt()
        notificationManager.notify(id, builder.build())
    }

    override fun onDestroy() {
        scrapingJob?.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?) = null
}
