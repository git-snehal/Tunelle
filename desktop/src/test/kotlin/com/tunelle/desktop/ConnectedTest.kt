package com.tunelle.desktop

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Last.fm's request signing and listen rule. */
class ConnectedTest {
    @Test
    fun lastFmSignsTheSortedParametersWithTheSecret() {
        // The same values the web player's test checks, from Python's hashlib.
        assertEquals(
            "d5e55eb18d690865ef3a94b29d378d0a",
            LastFmDesktop.signature(
                mapOf("method" to "track.scrobble", "api_key" to "abc", "sk" to "s3ss", "artist[0]" to "Daft Punk", "track[0]" to "Digital Love", "timestamp[0]" to "1700000000"),
                "sekret",
            ),
        )
        assertEquals("b5497fe66732297478ddae8099fa4b07", LastFmDesktop.signature(mapOf("method" to "track.updateNowPlaying", "api_key" to "k", "track" to "Café del Mar", "artist" to "Energy 52"), "s"))
    }

    @Test
    fun aListenIsHalfTheSongOrFourMinutes() {
        assertFalse(LastFmDesktop.isListen(89.0, 180.0))
        assertTrue(LastFmDesktop.isListen(90.0, 180.0))
        assertTrue(LastFmDesktop.isListen(240.0, 900.0))
        assertFalse(LastFmDesktop.isListen(25.0, 25.0))
        assertFalse(LastFmDesktop.isListen(120.0, 0.0))
        assertEquals("Daft Punk", LastFmDesktop.primaryArtist("Daft Punk, Pharrell Williams & Nile Rodgers"))
    }
}
