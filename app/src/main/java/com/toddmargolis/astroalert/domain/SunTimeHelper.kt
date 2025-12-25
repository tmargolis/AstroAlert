package com.toddmargolis.astroalert.domain

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object SunTimeHelper {

    fun parseToLocalHour(isoTime: String, zoneId: ZoneId = ZoneId.systemDefault()): Int {
        val instant = Instant.parse(isoTime)
        val zonedDateTime = instant.atZone(zoneId)
        return zonedDateTime.hour
    }

    fun parseToLocalHourAndMinute(isoTime: String, zoneId: ZoneId = ZoneId.systemDefault()): Pair<Int, Int> {
        val instant = Instant.parse(isoTime)
        val zonedDateTime = instant.atZone(zoneId)
        return Pair(zonedDateTime.hour, zonedDateTime.minute)
    }
}