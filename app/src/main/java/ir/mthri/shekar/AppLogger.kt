package ir.mthri.shekar

import android.util.Log


fun logD(tag: String, message: String) {
    Log.d(tag, message)
    AppLogCollector.addLog("D", tag, message)
}


fun logE(tag: String, message: String) {
    Log.e(tag, message)
    AppLogCollector.addLog("E", tag, message)
}


fun logI(tag: String, message: String) {
    Log.i(tag, message)
    AppLogCollector.addLog("I", tag, message)
}


fun logW(tag: String, message: String) {
    Log.w(tag, message)
    AppLogCollector.addLog("W", tag, message)
}
