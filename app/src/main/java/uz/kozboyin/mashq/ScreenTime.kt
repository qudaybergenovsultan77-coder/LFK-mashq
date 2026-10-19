package uz.kozboyin.mashq

import android.Manifest
import android.annotation.SuppressLint
import android.app.AppOpsManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
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

    fun intervalMin(c: Context): Int = sp(c).getInt("interval", 30)

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
        if (s.getInt("day", 0) != dayKey()) {
            s.edit().putInt("day", dayKey()).putLong("base", 0L).apply()
            return 0L
        }
        return s.getLong("base", 0L)
    }

    fun setBaseMs(c: Context, v: Long) {
        sp(c).edit().putInt("day", dayKey()).putLong("base", v).apply()
    }

    fun isDue(c: Context, usedMs: Long): Boolean =
        usedMs - baseMs(c) >= intervalMin(c) * 60_000L
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

    /** Bugun (soat 00:00 dan hozirgacha) ilovalar oldingi planda bo'lgan jami vaqt, ms. */
    fun todayMs(c: Context): Long {
        val usm = c.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val stats = usm.queryAndAggregateUsageStats(cal.timeInMillis, System.currentTimeMillis())
        val skip = setOfNotNull(c.packageName, homePackage(c), "com.android.systemui")
        var total = 0L
        for ((pkg, s) in stats) {
            if (pkg !in skip) total += s.totalTimeInForeground
        }
        return total
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
