package io.github.sionrgb.auralis;
import android.app.Activity;
import android.net.Uri;
import android.widget.FrameLayout;
import androidx.media3.exoplayer.source.MediaSource;
public interface EditionSupport {
    MediaSource resolve(Activity activity, String source) throws Exception;
    boolean playCompatibility(Activity activity, FrameLayout host, Uri uri);
    void pause(); void release(); void equalizer(int preset);
    default long compatibilityPosition() { return 0; }
}
