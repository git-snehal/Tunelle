package com.tunelle.data

import com.tunelle.data.settings.AutomixPerformance
import com.tunelle.data.settings.InterfaceSettings
import com.tunelle.data.settings.PerformanceProfile
import com.tunelle.data.settings.PlaybackSettings
import com.tunelle.data.settings.performanceApplied
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Each performance profile sets what it says, and leaves everything else alone. */
class PerformanceProfileTest {
    private val ui = InterfaceSettings().copy(wavySeekbar = true)
    private val pb = PlaybackSettings().copy(automix = true)

    @Test fun batterySaverTurnsTheCostlyThingsOff() {
        val (u, p) = performanceApplied(ui, pb, PerformanceProfile.BATTERY_SAVER)
        assertEquals(PerformanceProfile.BATTERY_SAVER, u.performanceProfile)
        assertFalse(u.highRefreshRate)
        assertTrue(u.stillBackdrops)
        assertFalse(u.animatedCovers); assertFalse(u.spotifyCanvas); assertFalse(u.movingCover); assertFalse(u.homeMovingCovers); assertFalse(u.blurLyrics)
        assertEquals(AutomixPerformance.EFFICIENT, p.automixPerformance)
        // Untouched.
        assertTrue(u.wavySeekbar)
        assertTrue(p.automix)
    }

    @Test fun highPerformanceTurnsThemBackOn() {
        val saver = performanceApplied(ui, pb, PerformanceProfile.BATTERY_SAVER)
        val (u, p) = performanceApplied(saver.first, saver.second, PerformanceProfile.PERFORMANCE)
        assertTrue(u.highRefreshRate)
        assertFalse(u.stillBackdrops)
        assertTrue(u.animatedCovers); assertTrue(u.movingCover); assertTrue(u.blurLyrics)
        assertEquals(AutomixPerformance.PERFORMANCE, p.automixPerformance)
    }

    @Test fun balancedSitsBetween() {
        val (u, _) = performanceApplied(ui, pb, PerformanceProfile.BALANCED)
        assertFalse(u.highRefreshRate)
        assertFalse(u.stillBackdrops)
        assertTrue(u.animatedCoversWifiOnly)
    }
}
