package com.tunelle.data.together

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RoomInviteTest {
    private val code = RoomCode.parse("K7QX-M2PA-9DTE")!!
    @Test fun newWebAndOldAppLinksBothOpenTheSameRoom() {
        assertEquals(code, RoomCode.parse(code.link))
        assertEquals(code, RoomCode.find("Listen with me\n${code.link}"))
        assertEquals(code, RoomCode.parse(code.appLink))
        assertEquals(code, RoomCode.find("Listen with me\n${code.appLink}"))
        assertEquals(code, RoomCode.parse("k7qx m2pa 9dte"))
    }
    @Test fun refusesUnrelatedHostsPathsAndPartialCodes() {
        assertNull(RoomCode.parse("https://evil.test/together/${code.raw}"))
        assertNull(RoomCode.parse("https://git-snehal.github.io.evil.test/Tunelle/together/#code=${code.raw}"))
        assertNull(RoomCode.parse("https://git-snehal.github.io/Other/together/#code=${code.raw}"))
        assertNull(RoomCode.parse("${code.appLink}AAAA"))
        assertNull(RoomCode.parse("https://git-snehal.github.io/Tunelle/together/"))
    }
}
