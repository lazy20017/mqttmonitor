package org.archuser.mqttnotify.core

object TopicMatcher {
    fun matches(filter: String, topic: String): Boolean {
        if (filter == "#") return true
        val filterParts = filter.split('/')
        val topicParts = topic.split('/')

        var index = 0
        while (index < filterParts.size) {
            val filterPart = filterParts[index]
            if (filterPart == "#") {
                return index == filterParts.lastIndex
            }
            if (index >= topicParts.size) {
                return false
            }
            if (filterPart != "+" && filterPart != topicParts[index]) {
                return false
            }
            index++
        }
        return index == topicParts.size
    }
}
