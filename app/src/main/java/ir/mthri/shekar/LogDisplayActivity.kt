package ir.mthri.shekar


import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class LogDisplayActivity : AppCompatActivity() {

    private lateinit var logTextView: TextView
    private lateinit var clearLogsButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log_display)

        logTextView = findViewById(R.id.logTextView)
        clearLogsButton = findViewById(R.id.clearLogsButton)

        displayLogs()

        clearLogsButton.setOnClickListener {
            AppLogCollector.clearLogs()
            displayLogs()
        }
    }

    private fun displayLogs() {
        val allLogs = AppLogCollector.getAllLogs()
        if (allLogs.isEmpty()) {
            logTextView.text = "No logs collected yet."
        } else {
            logTextView.text = allLogs
        }
    }
}