# Verification — 1.1.0 / 2026-10-15

- Final signed release x86_64 APKs were installed on the Android 15 / API 35 emulator. The instrumentation helper APK was signed with the same existing release key so these checks exercised release applications, not substitute debug applications.
- Full: 6 instrumentation tests passed (48.214 seconds). Lite: 5 passed, the Full-only VLC test skipped (runner reports 6 tests, 54.484 seconds). Both include 3 focused YouTube tests: safe Music/short-link routing, actual YouTube playback, and actual YouTube Music playback.
- Official YouTube IFrame API reported the expected video IDs `aqz-KE-bpKQ` and `K4DyBUG242c`, state 1 with time advancing past 1 second. Pause reaches state 2; after seeking to 10 seconds, playback resumes in state 1 past 10.3 seconds, unmuted. This verifies actual API playback state; the no-audio emulator does not verify acoustic output from a physical phone.
- Both editions again verified C++ JNI / Rust policy, actual Media3 HLS rendered buffers, seek and playback speed. Full also verified picture processing and actual VLC ProRes playback clock.
- Release lint: 0 errors, Lite 28 warnings / Full 30 warnings. Both release builds succeeded for ARM64, ARMv7, x86_64 and universal. The 8 APKs are zipaligned for 16 KB and `apksigner verify` passes v2 and v3 signatures. Existing public certificate SHA-256: `d1e0fd3998590409e44412cd34306ce54f7120c1f530df90987d2328f063e0e2`.
- No physical-phone test or other Android OS version test has been performed. YouTube Music support here means compatible single-track links in the official video player; it does not promise subscription features, all regional/age-restricted content, or background playback. Bilibili extraction was not revalidated in this update. Android has no DLSS implementation.
- Screenshots `youtube-music-full-1.1.png` and `youtube-music-lite-1.1.png` were captured after the final signed release apps resumed playback after seeking.

## Previous 1.0.0 checks — 2026-10-13

- Rust shared policy: 3 tests passed on Windows. Android instrumentation also checks the C++ JNI / Rust results.
- Android 15 / API 35 x86_64 emulator: Lite 2 tests passed, 1 Full-only test skipped; Full 3 tests passed.
- HLS / H.264 playback: decoder `renderedOutputBufferCount` > 5 and playback position > 300 ms, then pause, exact seek and 1.25× playback checked. Full also exercises GPU brightness, contrast and saturation processing. A further focused Full run verifies that an actual decoded frame timestamp reaches at least 1.5 seconds after seek/effects; 1 test passed. The screenshot is from that run.
- GitHub Linux release build and lint passed on commit `4ecf6b1`; [run](https://github.com/sion-rgb/auralis-android/actions/runs/38416931396). This checks source builds, not physical-device playback.
- Full VLC: a ProRes MOV stream advances the actual VLC clock beyond 300 ms. The test HTTP server supports byte ranges, as required by that stream.
- `lintLiteRelease` / `lintFullRelease`: 0 errors; 22 / 24 warnings, including deprecated APIs and newer-version notices. Warnings are retained in local reports, not represented as clean lint.
- Both release builds succeeded for ARM64, ARMv7, x86_64 and universal.
- All 8 published APKs are aligned and signed with one local release key; Android `apksigner verify` passes with v2 and v3 signatures. Public certificate SHA-256: `d1e0fd3998590409e44412cd34306ce54f7120c1f530df90987d2328f063e0e2`.
- No physical phone test, Android 8–14 / 16 device test or Google Play review has been completed. No claim of universal codec support, DLSS on Android or native authenticated YouTube/Bilibili playback is made.

Windows is a separate deliverable; its DLSS community runtime and Microsoft Store/account data are not published here.
