package ir.mthri.shekar

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.ImageView
import android.widget.Spinner
import kotlinx.coroutines.Job


class MainActivity : AppCompatActivity() {

    private val CHANNEL_POST_ID = "post_channel"
    private val DIVAR_STORAGE = "DS"
    private val divarScraper = DivarScraper()
    private var scrapingJob: Job? = null

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                Toast.makeText(this, "مجوز نوتیفیکیشن داده شد.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "مجوز نوتیفیکیشن داده نشد.", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        setupInsets()
        requestNotificationPermission()

        val sharedPref = getSharedPreferences(DIVAR_STORAGE, Context.MODE_PRIVATE)

        val urlTextEdit = findViewById<EditText>(R.id.txtUrl)
        val startButton = findViewById<Button>(R.id.btnStart)
        val stopButton = findViewById<Button>(R.id.btnStop)
        val periodSpinner = findViewById<Spinner>(R.id.spnrPeriod)
        val imageView = findViewById<ImageView>(R.id.imgTelegramChannel)

        // show log activity
        val logoImageView = findViewById<ImageView>(R.id.myImageView)
        logoImageView.setOnClickListener {
            val intent = Intent(this, LogDisplayActivity::class.java)
            startActivity(intent)
        }


        restorePreferences(sharedPref, urlTextEdit, periodSpinner)

        if (sharedPref.getBoolean("enable", false)) {
            disableControls(urlTextEdit, periodSpinner, startButton)
        }

        startButton.setOnClickListener {
            val url = urlTextEdit.text.toString()
            val selectedIndex = periodSpinner.selectedItemPosition
            val interval = resources.getStringArray(R.array.period)[selectedIndex].toLongOrNull() ?: 30L

            when {
                url.isBlank() -> urlTextEdit.error = "لطفاً آدرس را وارد کنید"
                !url.startsWith("https://divar.ir/") -> urlTextEdit.error = "آدرس باید با https://divar.ir/ شروع شود"
                else -> {
                    savePreferences(sharedPref, url, selectedIndex)
                    Toast.makeText(this, "درحال بررسی دیوار", Toast.LENGTH_SHORT).show()
                    disableControls(urlTextEdit, periodSpinner, startButton)
                    startPeriodicScraping(url, interval)
                }
            }
        }

        stopButton.setOnClickListener {
            stopScraping(sharedPref, urlTextEdit, periodSpinner, startButton)
        }

        imageView.setOnClickListener {
            val url = "https://t.me/mthri_tips"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    //Toast.makeText(this, "مجوز نوتیفیکیشن قبلاً موجود است.", Toast.LENGTH_SHORT).show()
                }

                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    AlertDialog.Builder(this)
                        .setTitle("نیاز به مجوز نوتیفیکیشن")
                        .setMessage("برای دریافت به‌روزرسانی‌های آگهی‌های جدید، برنامه نیاز به ارسال نوتیفیکیشن دارد. لطفاً مجوز را تأیید کنید.")
                        .setPositiveButton("باشه") { dialog, which ->
                            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        .setNegativeButton("نه ممنون") { dialog, which ->
                            Toast.makeText(this, "دریافت نوتیفیکیشن‌های جدید امکان‌پذیر نیست.", Toast.LENGTH_SHORT).show()
                        }
                        .show()
                }
                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            Toast.makeText(this, "مجوز نوتیفیکیشن به صورت خودکار داده شده است.", Toast.LENGTH_SHORT).show()
        }
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

    private fun startPeriodicScraping(targetUrl: String, interval: Long) {
        val intent = Intent(this, ScrapingService::class.java).apply {
            putExtra("url", targetUrl)
            putExtra("interval", interval)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }
    
    private fun setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun restorePreferences(
        sharedPref: android.content.SharedPreferences,
        urlTextEdit: EditText,
        periodSpinner: Spinner
    ) {
        urlTextEdit.setText(sharedPref.getString("url", ""))
        periodSpinner.setSelection(sharedPref.getInt("interval_position", 0))
    }

    private fun savePreferences(
        sharedPref: android.content.SharedPreferences,
        url: String,
        intervalPosition: Int
    ) = with(sharedPref.edit()) {
        putString("url", url)
        putBoolean("enable", true)
        putInt("interval_position", intervalPosition)
        putStringSet("tokens", null)
        apply()
    }

    private fun disableControls(urlEdit: EditText, spinner: Spinner, startBtn: Button) {
        urlEdit.isEnabled = false
        spinner.isEnabled = false
        startBtn.isEnabled = false
        startBtn.alpha = 0.5f
    }

    private fun enableControls(urlEdit: EditText, spinner: Spinner, startBtn: Button) {
        urlEdit.isEnabled = true
        spinner.isEnabled = true
        startBtn.isEnabled = true
        startBtn.alpha = 1f
    }

    private fun stopScraping(
        sharedPref: android.content.SharedPreferences,
        urlEdit: EditText,
        spinner: Spinner,
        startBtn: Button
    ) {
        with(sharedPref.edit()) {
            putBoolean("enable", false)
            apply()
        }
        enableControls(urlEdit, spinner, startBtn)
        scrapingJob?.cancel()
        scrapingJob = null
        val stopIntent = Intent(this, ScrapingService::class.java)
        stopService(stopIntent)
        Toast.makeText(this, "ربات متوقف شد", Toast.LENGTH_SHORT).show()
    }

}