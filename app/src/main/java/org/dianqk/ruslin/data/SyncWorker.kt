package org.dianqk.ruslin.data

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.*
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import java.util.concurrent.TimeUnit

private val TAG = "SyncWorker"

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val notesRepository: NotesRepository,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.i(TAG, "syncing")
        setProgress(setIsSyncing(true))
        val syncResult = notesRepository.sync()
        setProgress(setIsSyncing(false))
        return@withContext if (syncResult.isSuccess) Result.success() else Result.failure()
    }

    companion object {
        private const val IS_SYNCING = "isSyncing"
        const val WORK_NAME = "Ruslin"
        lateinit var uuid: UUID

        fun enqueueOneTimeWork(workerManager: WorkManager) {
            workerManager.enqueue(OneTimeWorkRequestBuilder<SyncWorker>().addTag(WORK_NAME).build())
        }

        fun enqueuePeriodicWork(
            workManager: WorkManager,
            syncInterval: Long,
            syncOnlyWhenCharging: Boolean,
            syncOnlyOnWiFi: Boolean,
        ) {
            workManager.enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.REPLACE,
                PeriodicWorkRequestBuilder<SyncWorker>(syncInterval, TimeUnit.MINUTES)
                    .setConstraints(Constraints.Builder()
                        .setRequiresCharging(syncOnlyWhenCharging)
                        .setRequiredNetworkType(if (syncOnlyOnWiFi) NetworkType.UNMETERED else NetworkType.CONNECTED)
                        .build()
                    )
                    .addTag(WORK_NAME)
                    .setInitialDelay(15, TimeUnit.MINUTES)
                    .build()
            )
        }

        fun setIsSyncing(boolean: Boolean) = workDataOf(IS_SYNCING to boolean)
        fun Data.getIsSyncing(): Boolean = getBoolean(IS_SYNCING, false)
    }

}
