package com.tunelle.data.history

import android.os.SystemClock
import com.tunelle.data.DebugLog as Log
import com.tunelle.data.account.AccountStore
import com.tunelle.data.innertube.Innertube
import com.tunelle.data.innertube.InnertubeParser
import com.tunelle.data.settings.AppSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Listens made outside Tunelle (YouTube Music, or any app on the same
 * account) come into Tunelle's history: Recents, Replay, the stats and the
 * mixes built from them. Tunelle already reports its own plays to the
 * account; this reads the account's history back when the app comes to the
 * front, at most every few minutes, and adds what isn't here yet.
 */
object AccountHistory {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null
    private var lastAt = 0L
    private const val EVERY_MS = 5 * 60_000L

    fun syncSoon(force: Boolean = false) {
        val pb = AppSettings.playback.value
        if (!pb.importAccountHistory) return
        if (!AccountStore.signedIn.value) return
        val now = SystemClock.elapsedRealtime()
        if (!force && lastAt != 0L && now - lastAt < EVERY_MS) return
        if (job?.isActive == true) return
        lastAt = now
        job = scope.launch {
            runCatching {
                val sections = InnertubeParser.parseHistory(Innertube.browse("FEmusic_history"))
                History.mergeAccount(sections)
            }.onFailure { Log.w("AccountHistory", "Couldn't read the account's history", it) }
        }
    }
}
