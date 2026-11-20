package com.ssdsolucoes.gestor

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

class VencimentoWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences("ssd_native_prefs", Context.MODE_PRIVATE)
        val rawJson = prefs.getString("clients_json", "[]") ?: "[]"

        try {
            val array = JSONArray(rawJson)
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)

            val todayCal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val todayMillis = todayCal.timeInMillis

            var urgentCount = 0
            var soonCount = 0

            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val dueStr = obj.optString("dueDate", "")
                if (dueStr.isNotEmpty()) {
                    val dueDate = sdf.parse(dueStr) ?: continue
                    val diffDays = TimeUnit.MILLISECONDS.toDays(dueDate.time - todayMillis)
                    if (diffDays <= 0) urgentCount++
                    else if (diffDays <= 3) soonCount++
                }
            }

            if (urgentCount > 0 || soonCount > 0) {
                val msg = "Você tem $urgentCount cliente(s) vencido(s)/hoje e $soonCount vencendo nos próximos 3 dias."
                dispararNotificacao("SSD Soluções - Alerta de Vencimentos", msg)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return Result.success()
    }

    private fun dispararNotificacao(titulo: String, mensagem: String) {
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(applicationContext, MainActivity.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(titulo)
            .setContentText(mensagem)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(2026, notification)
    }
}
