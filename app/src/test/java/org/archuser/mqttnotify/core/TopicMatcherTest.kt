package org.archuser.mqttnotify.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TopicMatcherTest {

    @Test
    fun `matches exact and wildcard filters`() {
        assertTrue(TopicMatcher.matches("sensors/+/temp", "sensors/node1/temp"))
        assertTrue(TopicMatcher.matches("sensors/#", "sensors/node1/temp"))
        assertTrue(TopicMatcher.matches("#", "anything/here"))
        assertFalse(TopicMatcher.matches("sensors/+/temp", "sensors/node1/humidity"))
    }
}
