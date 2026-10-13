# Verification — 2026-10-13

- Rust shared policy: 3 tests passed on Windows. Android instrumentation also checks the C++ JNI / Rust results.
- Android 15 / API 35 x86_64 emulator: Lite 2 tests passed, 1 Full-only test skipped; Full 3 tests passed.
- HLS / H.264 playback: decoder `renderedOutputBufferCount` > 5 and playback position > 300 ms, then pause, exact seek and 1.25× playback checked. Full also exercises GPU brightness, contrast and saturation processing.
- Full VLC: a ProRes MOV stream advances the actual VLC clock beyond 300 ms. The test HTTP server supports byte ranges, as required by that stream.
- `lintLiteRelease` / `lintFullRelease`: 0 errors; 22 / 24 warnings, including deprecated APIs and newer-version notices. Warnings are retained in local reports, not represented as clean lint.
- Both release builds succeeded for ARM64, ARMv7, x86_64 and universal.
- All 8 published APKs are aligned and signed with one local release key; Android `apksigner verify` passes with v2 and v3 signatures. Public certificate SHA-256: `d1e0fd3998590409e44412cd34306ce54f7120c1f530df90987d2328f063e0e2`.
- No physical phone test, Android 8–14 / 16 device test or Google Play review has been completed. No claim of universal codec support, DLSS on Android or native authenticated YouTube/Bilibili playback is made.

Windows is a separate deliverable; its DLSS community runtime and Microsoft Store/account data are not published here.
