package uz.kozboyin.mashq

import android.Manifest
import android.annotation.SuppressLint
import android.app.AppOpsManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.usage.UsageEvents
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.util.Calendar

/** Sozlamalar va "oxirgi mashqdan beri" hisobi. */
object Prefs {
    private fun sp(c: Context) = c.getSharedPreferences("lfk", Context.MODE_PRIVATE)

    fun intervalMin(c: Context): Int = sp(c).getInt("interval", 60)

    fun setIntervalMin(c: Context, v: Int) {
        sp(c).edit().putInt("interval", v).apply()
    }

    private fun dayKey(): Int {
        val cal = Calendar.getInstance()
        return cal.get(Calendar.YEAR) * 1000 + cal.get(Calendar.DAY_OF_YEAR)
    }

    /** Oxirgi mashq/eslatma paytidagi jami ekran vaqti (ms). Yangi kunda 0 dan boshlanadi. */
    fun baseMs(c: Context): Long {
        val s = sp(c)
        if (s.getInt("day_v2", 0) != dayKey()) {
            s.edit().putInt("day_v2", dayKey()).putLong("base_v2", 0L).apply()
            return 0L
        }
        return s.getLong("base_v2", 0L)
    }

    fun setBaseMs(c: Context, v: Long) {
        sp(c).edit().putInt("day_v2", dayKey()).putLong("base_v2", v).apply()
    }

    /** Oxirgi mashqdan beri o'tgan ekran vaqti (ms). Asos qiymat noto'g'ri bo'lsa, tuzatiladi. */
    fun sinceMs(c: Context, usedMs: Long): Long {
        val b = baseMs(c)
        if (b > usedMs) {
            setBaseMs(c, usedMs)
            return 0L
        }
        return usedMs - b
    }

    fun isDue(c: Context, usedMs: Long): Boolean =
        sinceMs(c, usedMs) >= intervalMin(c) * 60_000L
}

object ScreenTime {
    @Suppress("DEPRECATION")
    fun hasUsageAccess(c: Context): Boolean {
        val ops = c.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), c.packageName)
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun homePackage(c: Context): String? {
        val i = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        return c.packageManager.resolveActivity(i, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName
    }

    private fun midnightMs(): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun overlap(a: Long, b: Long, lo: Long, hi: Long): Long =
        (minOf(b, hi) - maxOf(a, lo)).coerceAtLeast(0L)

    private const val EV_FG = 1          // MOVE_TO_FOREGROUND / ACTIVITY_RESUMED
    private const val EV_BG = 2          // MOVE_TO_BACKGROUND / ACTIVITY_PAUSED
    private const val EV_SCREEN_OFF = 16 // SCREEN_NON_INTERACTIVE
    private const val EV_SHUTDOWN = 26   // DEVICE_SHUTDOWN

    /**
     * Bugun (soat 00:00 dan hozirgacha) ilovalar ekranda ochiq bo'lgan jami vaqt, ms.
     * Hodisalar (ochildi/yopildi/ekran o'chdi) bo'yicha aniq hisoblanadi.
     */
    fun todayMs(c: Context): Long {
        val usm = c.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = cal.timeInMillis
        val now = System.currentTimeMillis()
        val skip = setOfNotNull(c.packageName, homePackage(c), "com.android.systemui")

        val events = usm.queryEvents(start, now)
        val e = UsageEvents.Event()
        val open = HashMap<String, Long>()
        var total = 0L
        while (events.hasNextEvent()) {
            events.getNextEvent(e)
            val pkg = e.packageName ?: continue
            when (e.eventType) {
                EV_FG -> if (pkg !in skip && !open.containsKey(pkg)) open[pkg] = e.timeStamp
                EV_BG -> {
                    val s = open.remove(pkg)
                    if (s != null) total += (e.timeStamp - s).coerceAtLeast(0L)
                }
                EV_SCREEN_OFF, EV_SHUTDOWN -> {
                    for (s in open.values) total += (e.timeStamp - s).coerceAtLeast(0L)
                    open.clear()
                }
            }
        }
        for (s in open.values) total += (now - s).coerceAtLeast(0L)
        return minOf(total, now - start)
    }

    /** Mashqlar tugagach: eslatma hisobini nolga tushirish. */
    fun markDone(c: Context) {
        if (hasUsageAccess(c)) Prefs.setBaseMs(c, todayMs(c))
    }
}

object Notifier {
    private const val CHANNEL = "exercise"

    fun hasPermission(c: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(c, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    fun send(c: Context): Boolean {
        if (!hasPermission(c)) return false
        val nm = NotificationManagerCompat.from(c)
        if (!nm.areNotificationsEnabled()) return false

        val sys = c.getSystemService(NotificationManager::class.java)
        sys.createNotificationChannel(
            NotificationChannel(CHANNEL, "Mashq eslatmalari", NotificationManager.IMPORTANCE_HIGH)
        )

        val open = Intent(c, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pi = PendingIntent.getActivity(
            c, 0, open, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val n = NotificationCompat.Builder(c, CHANNEL)
            .setSmallIcon(R.drawable.ic_notif)
            .setContentTitle("Mashq vaqti")
            .setContentText("Ekranga uzoq qaradingiz. Ko'z va bo'yin uchun qisqa mashq qiling.")
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        return try {
            nm.notify(1001, n)
            true
        } catch (e: SecurityException) {
            false
        }
    }
}

/** Har ~15 daqiqada ekran vaqtini tekshiradi va kerak bo'lsa eslatma yuboradi. */
class ScreenTimeWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val c = applicationContext
        if (!ScreenTime.hasUsageAccess(c)) return Result.success()
        val used = ScreenTime.todayMs(c)
        if (Prefs.isDue(c, used) && Notifier.send(c)) {
            Prefs.setBaseMs(c, used)
        }
        return Result.success()
    }
}
