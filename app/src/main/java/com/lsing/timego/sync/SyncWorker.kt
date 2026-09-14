package com.lsing.timego.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.lsing.timego.data.TimeGoDatabase
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first

class SyncWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        if (FirebaseApp.getApps(applicationContext).isEmpty()) return Result.success()
        val user = FirebaseAuth.getInstance().currentUser ?: return Result.success()
        if (!user.isEmailVerified) return Result.success()
        val consent = CloudBackupRepository(applicationContext).consent.first()
        if (!consent.enabled) return Result.success()
        val consentUpload = CloudBackupRemote(applicationContext).publish(consent)
        if (consentUpload.isFailure) return Result.retry()
        val database = TimeGoDatabase.getInstance(applicationContext)
        val repository = SyncRepository(database)
        val batch = repository.pendingBatch(100)
        if (batch.isEmpty()) return Result.success()
        val factory = SyncPayloadFactory(database)
        val documents = batch.map { factory.build(it) }
        val upload = FirestoreSyncRemote(FirebaseFirestore.getInstance()).upload(user.uid, documents)
        if (upload.isFailure) {
            batch.forEach { repository.markRetry(it, "remote") }
            return Result.retry()
        }
        batch.forEach { repository.acknowledge(it) }
        if (repository.pendingBatch(1).isNotEmpty()) enqueue(applicationContext)
        return Result.success()
    }

    companion object {
        private const val UNIQUE_WORK = "timego-cloud-sync"
        private const val PERIODIC_WORK = "timego-cloud-sync-periodic"
        fun enqueue(context: Context) {
            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(UNIQUE_WORK, ExistingWorkPolicy.KEEP, request)
        }

        fun setPeriodicEnabled(context: Context, enabled: Boolean) {
            val manager = WorkManager.getInstance(context)
            if (!enabled) {
                manager.cancelUniqueWork(PERIODIC_WORK)
                return
            }
            val request = PeriodicWorkRequestBuilder<SyncWorker>(12, TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
            manager.enqueueUniquePeriodicWork(PERIODIC_WORK, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
