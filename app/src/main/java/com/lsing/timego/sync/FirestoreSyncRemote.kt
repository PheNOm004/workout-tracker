package com.lsing.timego.sync

import com.google.firebase.firestore.FirebaseFirestore
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

class FirestoreSyncRemote(private val firestore: FirebaseFirestore) {
    suspend fun upload(uid: String, documents: List<SyncDocument>): Result<Unit> = runCatching {
        val batch = firestore.batch()
        documents.forEach { document ->
            val reference = firestore.collection("users").document(uid).collection("data")
                .document("${document.metadata.domainType}_${document.metadata.stableUuid}")
            if (document.metadata.operation == SyncOperation.DELETE.name) batch.delete(reference)
            else {
                val payload = requireNotNull(document.payload) { "Missing local payload for pending ${document.metadata.domainType}" }
                batch.set(reference, payload + mapOf("stableUuid" to document.metadata.stableUuid, "domainType" to document.metadata.domainType, "revision" to document.metadata.revision, "updatedAtEpochMillis" to document.metadata.updatedAtEpochMillis))
            }
        }
        suspendCancellableCoroutine { continuation -> batch.commit().addOnCompleteListener { continuation.resume(it.exception) } }
            ?.let { throw it }
    }
}
