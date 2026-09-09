package com.hufeng943.timetable.sync

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import com.hufeng943.timetable.TimetableDatabaseProvider
import com.hufeng943.timetable.shared.sync.SyncCoordinator
import com.hufeng943.timetable.shared.sync.SyncManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class AutoSyncJobService : JobService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onStartJob(params: JobParameters): Boolean {
        scope.launch {
            val db = TimetableDatabaseProvider.database(applicationContext)
            SyncCoordinator(
                dao = db.syncRecordDao(),
                manager = SyncManager(listOf(WearOsTransport(applicationContext)))
            ).syncPending()
            jobFinished(params, false)
        }
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean = true

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val PERIODIC_ID = 94310
        private const val NOW_ID = 94311

        fun schedulePeriodic(context: Context) {
            val scheduler = context.getSystemService(JobScheduler::class.java)
            if (scheduler.getPendingJob(PERIODIC_ID) != null) return
            scheduler.schedule(
                JobInfo.Builder(PERIODIC_ID, ComponentName(context, AutoSyncJobService::class.java))
                    .setPeriodic(15 * 60 * 1000L)
                    .setPersisted(false)
                    .build()
            )
        }

        fun scheduleNow(context: Context) {
            context.getSystemService(JobScheduler::class.java).schedule(
                JobInfo.Builder(NOW_ID, ComponentName(context, AutoSyncJobService::class.java))
                    .setMinimumLatency(1_500L)
                    .setOverrideDeadline(8_000L)
                    .build()
            )
        }
    }
}
