//! Shared source validation and DSP preset policy. No networking or credential storage.
pub fn source_kind(source: &str) -> i32 {
    let source = source.trim();
    if source.is_empty() || source.len() > 16384 || source.chars().any(|c| c.is_control()) { return -1; }
    if source.starts_with('-') { return -1; }
    if source.len() > 2 && source.as_bytes()[1] == b':' && matches!(source.as_bytes()[2], b'\\' | b'/') { return 0; }
    let Some((scheme, rest)) = source.split_once("://") else { return if source.contains(':') { -1 } else { 0 }; };
    let authority = rest.split(['/', '?', '#']).next().unwrap_or("");
    if authority.is_empty() || authority.contains('@') || authority.chars().any(char::is_whitespace) { return -1; }
    match scheme.to_ascii_lowercase().as_str() { "http" => 1, "https" => 2, "rtsp" => 3, "rtmp" => 4, "rtmps" => 5, _ => -1 }
}
pub fn preset_gain(preset: i32, band: i32) -> f32 {
    const GAINS: [[f32; 6]; 4] = [[0.; 6], [-3., -2., 0., 3., 3., 0.], [3., 1., -1., 0., 2., 2.], [4., 2., -1., 2., 3., 1.]];
    if !(0..4).contains(&preset) || !(0..6).contains(&band) { return 0.; }
    GAINS[preset as usize][band as usize]
}
#[no_mangle]
/// # Safety
/// Caller must provide a readable buffer of `len` bytes. Input is borrowed only during this call.
pub unsafe extern "C" fn auralis_source_kind(data: *const u8, len: usize) -> i32 {
    if data.is_null() || len > 16384 { return -1; }
    match std::str::from_utf8(std::slice::from_raw_parts(data, len)) { Ok(s) => source_kind(s), Err(_) => -1 }
}
#[no_mangle]
pub extern "C" fn auralis_preset_gain(preset: i32, band: i32) -> f32 { preset_gain(preset, band) }
#[no_mangle]
pub extern "C" fn auralis_clamp_gain(gain: f32) -> f32 { if gain.is_finite() { gain.clamp(-12., 12.) } else { 0. } }
#[cfg(test)]
mod tests {
    use super::*;
    #[test] fn rejects_unsafe_sources() { for s in ["", "javascript:alert(1)", "file://C:/a", "https://u:p@host/a", "--script=x", "https://", "https://bad host/a", "https://host/\nscript"] { assert_eq!(source_kind(s), -1, "{s}"); } }
    #[test] fn accepts_media_protocols_and_local_paths() { for (s, k) in [("https://youtube.com/watch?v=x", 2), ("rtsp://host:8554/live", 3), ("C:\\影片\\a.mp4", 0), ("/storage/emulated/0/a.mp4", 0)] { assert_eq!(source_kind(s), k); } }
    #[test] fn dsp_is_bounded() { assert_eq!(auralis_clamp_gain(f32::NAN), 0.); assert_eq!(auralis_clamp_gain(100.), 12.); for p in -1..5 { for b in -1..7 { assert!((-12.0..=12.0).contains(&preset_gain(p,b))); } } }
}
