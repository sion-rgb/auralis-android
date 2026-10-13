param([string]$SdkPath = $env:ANDROID_SDK_ROOT)
$ErrorActionPreference = 'Stop'
if (!$SdkPath) { $SdkPath = $env:ANDROID_HOME }
if (!$SdkPath) { $SdkPath = Join-Path $env:LOCALAPPDATA 'Android\Sdk' }
$taskRoot = Split-Path $PSScriptRoot -Parent
$taskBin = Join-Path $SdkPath 'ndk\28.2.13676358\toolchains\llvm\prebuilt\windows-x86_64\bin'
if (!(Test-Path -LiteralPath $taskBin)) { throw 'Install Android NDK 28.2.13676358 first.' }
$taskTargets = @(@('aarch64-linux-android','aarch64-linux-android26-clang.cmd','arm64-v8a'),@('armv7-linux-androideabi','armv7a-linux-androideabi26-clang.cmd','armeabi-v7a'),@('x86_64-linux-android','x86_64-linux-android26-clang.cmd','x86_64'))
foreach ($taskTarget in $taskTargets) {
    rustup target add $taskTarget[0]
    $taskName = 'CARGO_TARGET_'+$taskTarget[0].Replace('-','_').ToUpperInvariant()+'_LINKER'
    [Environment]::SetEnvironmentVariable($taskName,(Join-Path $taskBin $taskTarget[1]),'Process')
    cargo build --manifest-path (Join-Path $taskRoot 'core\Cargo.toml') --release --target $taskTarget[0]
    if ($LASTEXITCODE -ne 0) { throw 'Rust native build failed.' }
    $taskDestination = Join-Path $taskRoot ('app\native\'+$taskTarget[2])
    New-Item -ItemType Directory -Force -Path $taskDestination | Out-Null
    Copy-Item -LiteralPath (Join-Path $taskRoot ('core\target\'+$taskTarget[0]+'\release\libauralis_core.a')) -Destination $taskDestination
}
