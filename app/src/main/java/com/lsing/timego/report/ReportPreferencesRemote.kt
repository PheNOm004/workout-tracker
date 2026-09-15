package com.lsing.timego.report

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

class ReportPreferencesRemote(private val context: Context) {
    suspend fun publish(subscription: ReportSubscription): Result<Unit> = runCatching {
        if (FirebaseApp.getApps(context).isEmpty()) return@runCatching
        val user = FirebaseAuth.getInstance().currentUser ?: return@runCatching
        if (!user.isEmailVerified) return@runCatching
        val task = FirebaseFirestore.getInstance().collection("users").document(user.uid).set(
            mapOf(
                "email" to user.email,
                "emailVerified" to true,
                "weeklyEnabled" to subscription.weeklyEnabled,
                "monthlyEnabled" to subscription.monthlyEnabled,
                "timezoneId" to subscription.timezoneId,
                "reportConsentVersion" to subscription.consentVersion,
                "reportConsentedAtEpochMillis" to subscription.consentedAtEpochMillis,
            ),
            com.google.firebase.firestore.SetOptions.merge(),
        )
        suspendCancellableCoroutine<Exception?> { continuation -> task.addOnCompleteListener { continuation.resume(it.exception) } }?.let { throw it }
    }
}
