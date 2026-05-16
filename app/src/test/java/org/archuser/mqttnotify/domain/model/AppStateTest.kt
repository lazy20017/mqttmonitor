package org.archuser.mqttnotify.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppStateTest {

    @Test
    fun `mute logic checks expiration`() {
        val mutedState = AppState(
            activeBrokerId = null,
            connectionMode = ConnectionMode.VISIBLE_ONLY,
            globalMuteUntil = 2000L,
            lastSessionStartedAt = null,
            materialYouEnabled = true
        )
        assertTrue(mutedState.isMuted(1000L))
        assertFalse(mutedState.isMuted(3000L))
    }
}
