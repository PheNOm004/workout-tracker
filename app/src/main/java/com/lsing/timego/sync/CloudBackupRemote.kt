package com.lsing.timego.sync

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

class CloudBackupRemote(private val context: Context) {
    suspend fun publish(consent: CloudBackupConsent): Result<Unit> = runCatching {
        if (FirebaseApp.getApps(context).isEmpty()) return@runCatching
        val user = FirebaseAuth.getInstance().currentUser ?: return@runCatching
        if (!user.isEmailVerified) return@runCatching
        val task = FirebaseFirestore.getInstance().collection("users").document(user.uid).set(
            mapOf("cloudBackupEnabled" to consent.enabled, "cloudConsentVersion" to consent.consentVersion, "cloudConsentedAtEpochMillis" to consent.consentedAtEpochMillis),
            SetOptions.merge(),
        )
        suspendCancellableCoroutine<Exception?> { continuation -> task.addOnCompleteListener { continuation.resume(it.exception) } }?.let { throw it }
    }
}
