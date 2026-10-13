package io.github.sionrgb.auralis;
import android.app.Activity; import android.net.Uri; import android.widget.FrameLayout;
import androidx.media3.exoplayer.source.MediaSource;
public final class EditionFactory {
    public static EditionSupport create() { return new EditionSupport() {
        public MediaSource resolve(Activity a, String s) { throw new UnsupportedOperationException("精簡版以網站模式播放網站連結。"); }
        public boolean playCompatibility(Activity a, FrameLayout h, Uri u) { return false; }
        public void pause() {} public void release() {} public void equalizer(int preset) {}
    }; }
}
