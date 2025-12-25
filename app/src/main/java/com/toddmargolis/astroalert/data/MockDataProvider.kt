package com.toddmargolis.astroalert.data

import com.toddmargolis.astroalert.data.models.*

object MockDataProvider {

    fun getMockForecast(): AstrosphericForecast {
        val cloudCoverData = mutableListOf<HourData>()
        val seeingData = mutableListOf<HourData>()
        val transparencyData = mutableListOf<HourData>()

        // Simulate good conditions: starts a bit cloudy, then clears up nicely
        for (hour in 0..81) {
            // Cloud cover: starts at 40%, drops to 10% by hour 3, stays low
            val cloudValue = when {
                hour < 2 -> 35.0 - (hour * 10.0)
                hour < 10 -> 10.0 + (Math.random() * 5)
                else -> 5.0 + (Math.random() * 10)
            }

            // Seeing: mostly good (3-4 out of 5)
            val seeingValue = 3.0 + (Math.random() * 1.5)

            // Transparency: good (low numbers are better, 0-13 is good)
            val transparencyValue = 8.0 + (Math.random() * 4)

            cloudCoverData.add(HourData(
                value = ValueData("#003E7E", cloudValue),
                hourOffset = hour
            ))

            seeingData.add(HourData(
                value = ValueData("#62A2E2", seeingValue),
                hourOffset = hour
            ))

            transparencyData.add(HourData(
                value = ValueData("#79BCE1", transparencyValue),
                hourOffset = hour
            ))
        }

        return AstrosphericForecast(
            timeZone = "America/Chicago",
            utcMinuteOffset = 360,
            modelTime = "2025122312",
            latitude = 41.66,
            longitude = -87.04,
            localStartTime = "2025-12-23T18:00:00",
            utcStartTime = "2025-12-24T00:00:00Z",
            apiCreditUsedToday = 95, // Show we're almost at limit
            cloudCover = cloudCoverData,
            transparency = transparencyData,
            seeing = seeingData,
            temperature = emptyList(),
            windVelocity = emptyList()
        )
    }

    fun getMockSkyData(): SkyData {
        return SkyData(
            time = "2025-12-23T18:00:00Z",
            apiCreditUsedToday = 95,
            moon = MoonLocation(
                altitude = -15.0, // Below horizon - excellent!
                azimuth = 180.0,
                illumination = 25.0, // Thin crescent
                phase = 0.1
            ),
            sun = SunLocation(
                altitude = -20.0, // Well below horizon
                azimuth = 270.0
            )
        )
    }
}