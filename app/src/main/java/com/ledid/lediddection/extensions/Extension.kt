package com.ledid.lediddection.extensions

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

fun Boolean?.isTrue(): Boolean = this == true
fun Boolean?.isFalse(): Boolean = this == false
fun Boolean?.isFalseOrNull(): Boolean = this == false || this == null
fun LiveData<Boolean>?.runIfTrue(block: () -> Unit): LiveData<Boolean> =
    MutableLiveData(this?.value)

fun runDelay(long: Long, handle: () -> Unit) {
    Handler(Looper.getMainLooper()).postDelayed({
        handle.invoke()
    }, long)
}

fun View.setSingleClickListener(action: () -> Unit) {
    setOnClickListener(object : View.OnClickListener {
        private var lastClickTime: Long = 0
        override fun onClick(v: View) {
            if (SystemClock.elapsedRealtime() - lastClickTime < 1000) {
                return
            }
            action()
            lastClickTime = SystemClock.elapsedRealtime()
        }
    })
}