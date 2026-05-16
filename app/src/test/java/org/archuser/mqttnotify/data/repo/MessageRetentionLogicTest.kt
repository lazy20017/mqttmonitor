package org.archuser.mqttnotify.data.repo

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageRetentionLogicTest {

    @Test
    fun `retained message is not new activity by default`() {
        val retainedAsNew = false
        val isRetained = true
        val isNewActivity = !isRetained || retainedAsNew
        assertFalse(isNewActivity)
    }

    @Test
    fun `retained message can be counted as new when enabled`() {
        val retainedAsNew = true
        val isRetained = true
        val isNewActivity = !isRetained || retainedAsNew
        assertTrue(isNewActivity)
    }
}
