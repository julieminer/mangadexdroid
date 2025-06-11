package com.melonhead.lib_notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.TaskStackBuilder
import com.melonhead.lib_logging.Clog
import com.melonhead.lib_navigation.Navigator
import com.melonhead.lib_navigation.keys.ActivityKey
import com.melonhead.lib_notifications.models.ChapterNotification
import kotlinx.coroutines.delay

data class NewChapterNotificationChannel(
    val navigator: Navigator,
) {
    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT)
            val manager = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun pendingIntent(context: Context, mangaId: String, chapterId: String): PendingIntent? {
        val mainActivityIntent = navigator.intentForKey(context, ActivityKey.MainActivity)

        mainActivityIntent.putExtra(MANGA_ID_EXTRA, mangaId)
        mainActivityIntent.putExtra(CHAPTER_ID_EXTRA, chapterId)

        return TaskStackBuilder.create(context).run {
            addNextIntentWithParentStack(mainActivityIntent)
            getPendingIntent(chapterId.hashCode(), PendingIntent.FLAG_IMMUTABLE)
        }
    }

    private fun buildNotification(context: Context, pendingIntent: PendingIntent, mangaTitle: String, chapterTitle: String): Notification {
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(mangaTitle)
            .setContentText(chapterTitle)
            .setSmallIcon(R.drawable.ic_notification_icon)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
    }

    suspend fun post(context: Context, series: List<ChapterNotification>, installDateSeconds: Long) {
        // set up channel
        createNotificationChannel(context)

        val notificationManager = NotificationManagerCompat.from(context)
        val activeNotifications = notificationManager.activeNotifications

        Clog.i("post: New chapters for ${series.count()} manga")
        series.forEach { chapter ->
            val id = notificationId(chapter)
            // don't re-post notifications that have already been posted
            if (activeNotifications.any { it.id == id }) return@forEach
            val pendingIntent = pendingIntent(context, chapter.mangaId, chapter.chapterId) ?: return@forEach
            val notification = buildNotification(context, pendingIntent, chapter.mangaTitle, chapter.chapterTitle)
            notificationManager.notify(id, notification)
            delay(1000) // ensures android actually posts all notifications
        }
    }

    fun dismissNotification(context: Context, mangaId: String, chapterId: String) {
        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.cancel(mangaId.hashCode() + chapterId.hashCode())
    }

    private fun notificationId(chapterNotification: ChapterNotification): Int {
        return chapterNotification.mangaId.hashCode() + chapterNotification.chapterId.hashCode()
    }

    companion object {
        const val MANGA_ID_EXTRA = "manga_id_extra"
        const val CHAPTER_ID_EXTRA = "chapter_id_extra"
        private const val CHANNEL_ID = "new_chapters"
        private const val CHANNEL_NAME = "New Chapter"
    }
}
