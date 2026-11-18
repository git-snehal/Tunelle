# Tunelle

A free, open-source music player for Android (with iOS on the way), built on
YouTube Music. Tunelle talks to YouTube Music directly from your phone: no API
key, no account required and no server of our own. Sign in only if you want
your likes and playlists.

Built with Kotlin, Jetpack Compose and Media3. Android 8.0 and newer.

> **Status:** early. Tunelle is being rebuilt from OpenTune; expect rough edges.

## Built on OpenTune

Tunelle is a fork of [OpenTune](https://github.com/justshivv/Opentune) by
justshivv and aerluc, licensed under GPL-3.0. Most of the app's foundation —
the YouTube Music client, the stream engines, playback and much of the design —
comes from OpenTune. Thank you to its authors and contributors.

Compared with OpenTune, Tunelle:

- has no Discord integration;
- sends no usage counts;
- depends on no servers run by the project (no remote config, add-on catalogue
  or hosted lossless add-on). Everything runs on the phone.

OpenTune's original README is kept in [docs/OPENTUNE_README.md](docs/OPENTUNE_README.md)
for reference; its feature list still describes most of what Tunelle can do.

## Building

Open the folder in Android Studio, or build from the command line:

```
./gradlew assembleDebug        # app/build/outputs/apk/debug/app-<abi>-debug.apk
./gradlew assembleRelease      # app/build/outputs/apk/release/app-<abi>-release.apk
./gradlew testDebugUnitTest    # unit tests
```

You need JDK 17 or newer and an Android SDK with platforms 36 and 37,
NDK `27.2.12479018` and CMake `3.22.1`.

### Releases

GitHub Actions builds and tests every push (`.github/workflows/ci.yml`).
Pushing a tag such as `v0.1.0` builds signed APKs and publishes a GitHub
release (`.github/workflows/release.yml`). Release signing uses the repository
secrets `TUNELLE_KEYSTORE_BASE64`, `TUNELLE_KEYSTORE_PASSWORD`,
`TUNELLE_KEY_ALIAS` and `TUNELLE_KEY_PASSWORD`. Set them before the first
release: the in-app updater can only install an update signed with the same key.

## License

Tunelle is licensed under the GNU General Public License v3.0. See
[LICENSE](LICENSE) and [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).

Tunelle isn't affiliated with or endorsed by YouTube, Google, Spotify or
OpenTune's authors.
