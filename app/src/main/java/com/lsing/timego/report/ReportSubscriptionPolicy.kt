package com.lsing.timego.report

fun reportSubscriptionBlockReason(emailVerified: Boolean, cloudBackupEnabled: Boolean): String? = when {
    !emailVerified -> "Verify your email before enabling reports."
    !cloudBackupEnabled -> "Enable cloud backup before reports so the server can build the same summary."
    else -> null
}
