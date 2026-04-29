package com.exapps.anistream.data.download

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import com.exapps.anistream.R
import com.exapps.anistream.core.common.DispatcherProvider
import com.exapps.anistream.domain.model.StreamType
import com.exapps.anistream.domain.usecase.GetEpisodeStreamUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@AndroidEntryPoint
class AnimeDownloadService : LifecycleService() {

    @Inject
    lateinit var getEpisodeStreamUseCase: GetEpisodeStreamUseCase

    @Inject
    lateinit var dispatcherProvider: DispatcherProvider

    private val serviceScope by lazy {
        CoroutineScope(SupervisorJob() + dispatcherProvider.main)
    }
    private var activeTransformer: Transformer? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CANCEL -> {
                activeTransformer?.cancel()
                stopSelf()
            }
            ACTION_START_DOWNLOAD -> {
                val request = DownloadRequest.fromIntent(intent) ?: return Service.START_NOT_STICKY
                startForeground(
                    NOTIFICATION_ID,
                    notificationBuilder(request.displayTitle)
                        .setProgress(100, 0, true)
                        .build(),
                )
                serviceScope.launch {
                    runCatching { executeDownload(request) }
                        .onSuccess {
                            updateNotification(
                                title = request.displayTitle,
                                text = getString(R.string.download_complete),
                                progress = 100,
                                ongoing = false,
                            )
                            stopSelf()
                        }
                        .onFailure { error ->
                            updateNotification(
                                title = request.displayTitle,
                                text = error.message ?: getString(R.string.download_failed),
                                progress = 0,
                                ongoing = false,
                                indeterminate = false,
                            )
                            stopSelf()
                        }
                }
            }
        }

        return Service.START_NOT_STICKY
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private suspend fun executeDownload(request: DownloadRequest) {
        val stream = getEpisodeStreamUseCase(
            titleSlug = request.titleSlug,
            episodeNumber = request.episodeNumber,
            preferredServerId = request.preferredServerId,
        )
        val selectedSource = stream.availableSources.firstOrNull {
            it.type == StreamType.HLS || it.type == StreamType.MP4 || it.type == StreamType.MKV
        } ?: error(getString(R.string.download_no_source))

        val outputFile = buildOutputFile(request = request, sourceType = selectedSource.type)
        exportStreamToFile(sourceUrl = selectedSource.url, outputFile = outputFile, title = request.displayTitle)
    }

    private suspend fun exportStreamToFile(
        sourceUrl: String,
        outputFile: File,
        title: String,
    ) = withContext(dispatcherProvider.main) {
        suspendCancellableCoroutine<Unit> { continuation ->
            updateNotification(
                title = title,
                text = getString(R.string.download_in_progress),
                progress = 0,
                ongoing = true,
                indeterminate = true,
            )

            val transformer = Transformer.Builder(this@AnimeDownloadService)
                .setVideoMimeType(MimeTypes.VIDEO_H264)
                .setAudioMimeType(MimeTypes.AUDIO_AAC)
                .addListener(
                    object : Transformer.Listener {
                        override fun onCompleted(composition: Composition, result: ExportResult) {
                            activeTransformer = null
                            if (!continuation.isCompleted) {
                                continuation.resume(Unit)
                            }
                        }

                        override fun onError(
                            composition: Composition,
                            exportResult: ExportResult,
                            exportException: ExportException,
                        ) {
                            activeTransformer = null
                            if (!continuation.isCompleted) {
                                continuation.resumeWithException(exportException)
                            }
                        }
                    },
                )
                .build()

            activeTransformer = transformer

            val editedMediaItem = EditedMediaItem.Builder(MediaItem.fromUri(sourceUrl)).build()
            transformer.start(editedMediaItem, outputFile.absolutePath)

            continuation.invokeOnCancellation {
                transformer.cancel()
                activeTransformer = null
            }
        }
    }

    private fun buildOutputFile(request: DownloadRequest, sourceType: StreamType): File {
        val rootDir = getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: filesDir
        val downloadsDir = File(rootDir, "AniStream").apply { mkdirs() }
        val fileName = buildString {
            append(sanitizeFileName(request.displayTitle))
            append("-E")
            append(request.episodeNumber.toString().padStart(2, '0'))
            append(
                when (sourceType) {
                    StreamType.MKV -> ".mkv"
                    else -> ".mp4"
                },
            )
        }
        return File(downloadsDir, fileName)
    }

    private fun sanitizeFileName(value: String): String {
        return value.replace(Regex("[^A-Za-z0-9\\u0600-\\u06FF._ -]"), "_")
    }

    private fun updateNotification(
        title: String,
        text: String,
        progress: Int,
        ongoing: Boolean,
        indeterminate: Boolean = false,
    ) {
        val notification = notificationBuilder(title)
            .setContentText(text)
            .setOngoing(ongoing)
            .setOnlyAlertOnce(true)
            .setProgress(100, progress, indeterminate)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun notificationBuilder(title: String): NotificationCompat.Builder {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(getString(R.string.download_in_progress))
            .setPriority(NotificationCompat.PRIORITY_LOW)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.download_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        )
        notificationManager.createNotificationChannel(channel)
    }

    data class DownloadRequest(
        val titleSlug: String,
        val episodeNumber: Int,
        val displayTitle: String,
        val preferredServerId: String? = null,
    ) {
        fun toIntent(context: Context): Intent {
            return Intent(context, AnimeDownloadService::class.java).apply {
                action = ACTION_START_DOWNLOAD
                putExtra(EXTRA_TITLE_SLUG, titleSlug)
                putExtra(EXTRA_EPISODE_NUMBER, episodeNumber)
                putExtra(EXTRA_DISPLAY_TITLE, displayTitle)
                putExtra(EXTRA_PREFERRED_SERVER_ID, preferredServerId)
            }
        }

        companion object {
            fun fromIntent(intent: Intent): DownloadRequest? {
                val titleSlug = intent.getStringExtra(EXTRA_TITLE_SLUG) ?: return null
                val episodeNumber = intent.getIntExtra(EXTRA_EPISODE_NUMBER, -1)
                val displayTitle = intent.getStringExtra(EXTRA_DISPLAY_TITLE) ?: return null
                if (episodeNumber < 0) return null
                return DownloadRequest(
                    titleSlug = titleSlug,
                    episodeNumber = episodeNumber,
                    displayTitle = displayTitle,
                    preferredServerId = intent.getStringExtra(EXTRA_PREFERRED_SERVER_ID),
                )
            }
        }
    }

    companion object {
        private const val CHANNEL_ID = "anistream.downloads"
        private const val NOTIFICATION_ID = 3001
        private const val ACTION_START_DOWNLOAD = "com.exapps.anistream.action.START_DOWNLOAD"
        private const val ACTION_CANCEL = "com.exapps.anistream.action.CANCEL_DOWNLOAD"
        private const val EXTRA_TITLE_SLUG = "extra_title_slug"
        private const val EXTRA_EPISODE_NUMBER = "extra_episode_number"
        private const val EXTRA_DISPLAY_TITLE = "extra_display_title"
        private const val EXTRA_PREFERRED_SERVER_ID = "extra_preferred_server_id"

        fun start(context: Context, request: DownloadRequest) {
            ContextCompat.startForegroundService(context, request.toIntent(context))
        }

        fun cancel(context: Context) {
            context.startService(
                Intent(context, AnimeDownloadService::class.java).apply {
                    action = ACTION_CANCEL
                },
            )
        }
    }
}
