#!/usr/bin/env bash
set -euo pipefail
task_root="$(cd "$(dirname "$0")/.." && pwd)"
task_sdk="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}"
task_bin="$task_sdk/ndk/28.2.13676358/toolchains/llvm/prebuilt/linux-x86_64/bin"
test -d "$task_bin"
for task_spec in 'aarch64-linux-android:aarch64-linux-android26:arm64-v8a' 'armv7-linux-androideabi:armv7a-linux-androideabi26:armeabi-v7a' 'x86_64-linux-android:x86_64-linux-android26:x86_64'; do
  IFS=: read -r task_target task_clang task_abi <<< "$task_spec"
  rustup target add "$task_target"
  task_name="CARGO_TARGET_${task_target^^}_LINKER"; task_name="${task_name//-/_}"
  export "$task_name=$task_bin/$task_clang-clang"
  cargo build --manifest-path "$task_root/core/Cargo.toml" --release --target "$task_target"
  mkdir -p "$task_root/app/native/$task_abi"
  cp "$task_root/core/target/$task_target/release/libauralis_core.a" "$task_root/app/native/$task_abi/"
done
