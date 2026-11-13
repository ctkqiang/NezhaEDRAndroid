package xin.ctkqiang.nezhaedr.util

import android.util.Log

object EDRLogger {

    private const val TAG_PREFIX = "NezhaEDR_"

    private fun isAndroidLogAvailable(): Boolean {
        return try {
            Log.isLoggable(TAG_PREFIX, Log.VERBOSE)
            true
        } catch (e: Throwable) {
            false
        }
    }

    fun v(tag: String, message: String) {
        if (isAndroidLogAvailable()) {
            Log.v(TAG_PREFIX + tag, message)
        } else {
            println("[V][$TAG_PREFIX$tag] $message")
        }
    }

    fun d(tag: String, message: String) {
        if (isAndroidLogAvailable()) {
            Log.d(TAG_PREFIX + tag, message)
        } else {
            println("[D][$TAG_PREFIX$tag] $message")
        }
    }

    fun i(tag: String, message: String) {
        if (isAndroidLogAvailable()) {
            Log.i(TAG_PREFIX + tag, message)
        } else {
            println("[I][$TAG_PREFIX$tag] $message")
        }
    }

    fun w(tag: String, message: String, throwable: Throwable? = null) {
        if (isAndroidLogAvailable()) {
            if (throwable != null) {
                Log.w(TAG_PREFIX + tag, message, throwable)
            } else {
                Log.w(TAG_PREFIX + tag, message)
            }
        } else {
            println("[W][$TAG_PREFIX$tag] $message ${throwable?.message ?: ""}")
        }
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (isAndroidLogAvailable()) {
            if (throwable != null) {
                Log.e(TAG_PREFIX + tag, message, throwable)
            } else {
                Log.e(TAG_PREFIX + tag, message)
            }
        } else {
            println("[E][$TAG_PREFIX$tag] $message ${throwable?.message ?: ""}")
        }
    }

    fun fatal(tag: String, message: String, throwable: Throwable? = null) {
        val fatalMsg = "[FATAL CRITICAL FAILURE] $message"
        if (isAndroidLogAvailable()) {
            if (throwable != null) {
                Log.e(TAG_PREFIX + tag, fatalMsg, throwable)
            } else {
                Log.e(TAG_PREFIX + tag, fatalMsg)
            }
        } else {
            println("[FATAL][$TAG_PREFIX$tag] $fatalMsg ${throwable?.message ?: ""}")
        }
    }
}
