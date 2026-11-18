package com.tunelle.data

import android.app.Application
import com.tunelle.data.account.AccountStore
import com.tunelle.data.autoeq.AutoEq
import com.tunelle.data.covers.AlbumCovers
import com.tunelle.data.download.Downloads
import com.tunelle.data.history.History
import com.tunelle.data.innertube.InnerTubeXResolver
import com.tunelle.data.innertube.UpgradedTracks
import com.tunelle.data.lastfm.LastFm
import com.tunelle.data.library.LibraryStore
import com.tunelle.data.listenbrainz.ListenBrainz
import com.tunelle.data.settings.AppSettings
import com.tunelle.data.subsonic.Subsonic
import com.tunelle.playback.QueueStore

/** Starts the shared services, in the order they depend on each other. */
object Core {
    /**
     * [phone] is false on the watch, which leaves out what only the phone
     * and TV app does: new-release alerts, update checks and usage counts.
     */
    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    fun init(app: Application, phone: Boolean = true) {
        // Media3's warnings and errors (a failed load, a decoder that won't
        // start) into the app's own log as well as logcat, for "Copy log".
        androidx.media3.common.util.Log.setLogger(object : androidx.media3.common.util.Log.Logger {
            private val system = androidx.media3.common.util.Log.Logger.DEFAULT
            override fun d(tag: String, message: String, throwable: Throwable?) = system.d(tag, message, throwable)
            override fun i(tag: String, message: String, throwable: Throwable?) = system.i(tag, message, throwable)
            override fun w(tag: String, message: String, throwable: Throwable?) {
                AppLog.add('W', tag, message, throwable)
                system.w(tag, message, throwable)
            }
            override fun e(tag: String, message: String, throwable: Throwable?) {
                AppLog.add('E', tag, message, throwable)
                system.e(tag, message, throwable)
            }
        })
        AppSettings.init(app)
        com.tunelle.data.config.RemoteConfig.init(app)
        com.tunelle.data.addons.AddonStore.init(app)
        com.tunelle.data.addons.AddonOutput.init(app)
        com.tunelle.data.addons.AddonStore.onChanged = com.tunelle.data.lossless.ExternalStreams::retryLookups
        History.init(app)
        com.tunelle.data.history.ListenedCollections.init(app)
        com.tunelle.data.covers.ArtistPhotos.init(app)
        com.tunelle.data.metadata.ArtistGenres.init(app)
        com.tunelle.data.history.MixSnapshots.init(app)
        com.tunelle.data.reco.HomeEngagement.init(app)
        LoudnessStore.init(app)
        UpgradedTracks.init(app)
        QueueStore.init(app)
        com.tunelle.data.together.Together.init(app)
        LastFm.init(app)
        Subsonic.init(app)
        ListenBrainz.init(app)
        com.tunelle.data.config.HomeDismissals.init(app)
        com.tunelle.data.reco.TasteModel.init(app)
        com.tunelle.data.audiobooks.Audiobooks.init(app)
        com.tunelle.data.radio.Radio.init(app)
        if (phone) {
            com.tunelle.data.addons.AddonRefreshWorker.schedule(app)
            com.tunelle.data.releases.NewReleases.init(app)
            com.tunelle.data.UpdateCheck.init(app)
            com.tunelle.data.UpdateLock.init(app)
            com.tunelle.data.UpdateCheck.schedule(app)
        }
        // Spotify sign-in is gone; drop any tokens an earlier version kept.
        app.deleteSharedPreferences("spotify")
        // The waveform seek bar is gone; drop the waveforms an earlier version saved.
        java.io.File(app.filesDir, "waveforms").takeIf { it.exists() }?.let { dir -> Thread { dir.deleteRecursively() }.start() }
        com.tunelle.data.podcasts.Podcasts.init(app)
        com.tunelle.data.library.ResumePoints.init(app)
        AlbumCovers.init(app)
        com.tunelle.data.spotify.SpotifyWebPlayer.init(app)
        com.tunelle.data.spotify.SpotifySync.init(app)
        AutoEq.init(app)
        com.tunelle.data.library.LibraryArtwork.init(app)
        LibraryStore.init(app)
        Downloads.init(app)
        if (phone) com.tunelle.data.download.SmartDownloads.init(app)
        com.tunelle.data.download.DownloadedLists.init(app)
        AccountStore.init(app)
        com.tunelle.data.library.LibraryCache.init(app)
        // After library and account: it sends playlist changes still waiting, when signed in.
        com.tunelle.data.library.PlaylistSync.init(app)
        com.tunelle.data.library.LikedSync.init()
        InnerTubeXResolver.init(app)
    }

    /**
     * Saves at once what's still waiting for its save (each store waits a moment
     * after a change, so a run of changes is written once): the app is leaving
     * the screen or the player is closing, and the process may go any moment.
     */
    fun flush() {
        LibraryStore.flush()
        History.flush()
        com.tunelle.data.history.ListenedCollections.flush()
        Downloads.flush()
    }
}
