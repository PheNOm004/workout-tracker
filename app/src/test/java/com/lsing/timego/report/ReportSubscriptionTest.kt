package com.lsing.timego.report

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReportSubscriptionTest {
    @Test fun reportsRequireVerifiedEmail() {
        assertEquals("Verify your email before enabling reports.", reportSubscriptionBlockReason(false, false))
    }

    @Test fun reportsRequireCloudBackupAfterVerification() {
        assertEquals("Enable cloud backup before reports so the server can build the same summary.", reportSubscriptionBlockReason(true, false))
    }

    @Test fun prerequisitesDoNotCoupleWeeklyAndMonthlyChoices() {
        assertNull(reportSubscriptionBlockReason(true, true))
        val defaults = ReportSubscription()
        assertEquals(false, defaults.weeklyEnabled)
        assertEquals(false, defaults.monthlyEnabled)
        assertEquals(0, defaults.consentVersion)
    }
}
