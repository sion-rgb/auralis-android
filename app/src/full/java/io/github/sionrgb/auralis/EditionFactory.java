package io.github.sionrgb.auralis;

import android.app.Activity;
import android.net.Uri;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Button;
import android.view.Gravity;
import androidx.media3.common.MediaItem;
import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.exoplayer.source.MergingMediaSource;
import com.yausername.youtubedl_android.YoutubeDL;
import com.yausername.youtubedl_android.YoutubeDLRequest;
import org.json.JSONArray;
import org.json.JSONObject;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;
import org.videolan.libvlc.util.VLCVideoLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

@androidx.media3.common.util.UnstableApi
public final class EditionFactory {
    public static EditionSupport create() { return new FullSupport(); }
    private static final class FullSupport implements EditionSupport {
        private LibVLC lib;
        private MediaPlayer vlc;
        public MediaSource resolve(Activity activity, String source) throws Exception {
            YoutubeDL.getInstance().init(activity);
            YoutubeDLRequest request = new YoutubeDLRequest(source);
            request.addOption("--dump-single-json"); request.addOption("--no-playlist"); request.addOption("--skip-download");
            request.addOption("--socket-timeout", 15); request.addOption("--no-cache-dir"); request.addOption("--no-plugin-dirs");
            request.addOption("-f", "bestvideo[height<=1080]+bestaudio/best[height<=1080]/best");
            JSONObject info = new JSONObject(YoutubeDL.getInstance().execute(request).getOut());
            HashMap<String,String> headers = new HashMap<>();
            JSONObject headerInfo = info.optJSONObject("http_headers");
            if (headerInfo != null) { Iterator<String> keys = headerInfo.keys(); while (keys.hasNext()) { String key = keys.next(); headers.put(key, headerInfo.getString(key)); } }
            DefaultHttpDataSource.Factory http = new DefaultHttpDataSource.Factory().setDefaultRequestProperties(headers).setConnectTimeoutMs(15000).setReadTimeoutMs(20000);
            DefaultMediaSourceFactory factory = new DefaultMediaSourceFactory(new DefaultDataSource.Factory(activity, http));
            JSONArray formats = info.optJSONArray("requested_formats");
            if (formats != null && formats.length() > 1) {
                MediaSource[] tracks = new MediaSource[formats.length()];
                for (int i=0;i<formats.length();i++) tracks[i] = factory.createMediaSource(MediaItem.fromUri(formats.getJSONObject(i).getString("url")));
                return new MergingMediaSource(tracks);
            }
            return factory.createMediaSource(MediaItem.fromUri(info.getString("url")));
        }
        public boolean playCompatibility(Activity activity, FrameLayout host, Uri uri) {
            release();
            ArrayList<String> options = new ArrayList<>(); options.add("--network-caching=1500"); options.add("--no-video-title-show");
            lib = new LibVLC(activity, options); vlc = new MediaPlayer(lib);
            VLCVideoLayout layout = new VLCVideoLayout(activity); host.removeAllViews(); host.addView(layout, new FrameLayout.LayoutParams(-1,-1));
            vlc.attachViews(layout, null, false, false);
            Media media = new Media(lib, uri); media.setHWDecoderEnabled(true, false); vlc.setMedia(media); media.release(); vlc.play();
            LinearLayout controls = new LinearLayout(activity); controls.setGravity(Gravity.CENTER);
            Button back = new Button(activity); back.setText("−10 秒"); back.setOnClickListener(v -> vlc.setTime(Math.max(0,vlc.getTime()-10000)));
            Button toggle = new Button(activity); toggle.setText("▶ / Ⅱ"); toggle.setOnClickListener(v -> {if(vlc.isPlaying())vlc.pause();else vlc.play();});
            Button ahead = new Button(activity); ahead.setText("＋10 秒"); ahead.setOnClickListener(v -> vlc.setTime(vlc.getTime()+10000));
            controls.addView(back); controls.addView(toggle); controls.addView(ahead);
            FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM); host.addView(controls,p);
            return true;
        }
        public void equalizer(int preset) {
            if(vlc==null)return;
            MediaPlayer.Equalizer equalizer = MediaPlayer.Equalizer.create();
            equalizer.setPreAmp(preset==0?0:-4);
            for(int i=0;i<MediaPlayer.Equalizer.getBandCount();i++) {
                float frequency=MediaPlayer.Equalizer.getBandFrequency(i); float[] centers={60,170,350,1000,3500,10000}; int nearest=0;
                for(int j=1;j<6;j++) if(Math.abs(Math.log(frequency/centers[j]))<Math.abs(Math.log(frequency/centers[nearest])))nearest=j;
                equalizer.setAmp(i,NativeCore.presetGain(preset,nearest));
            }
            vlc.setEqualizer(equalizer);
        }
        public void pause() {if(vlc!=null)vlc.pause();}
        public long compatibilityPosition() {return vlc==null?0:vlc.getTime();}
        public void release() {if(vlc!=null){vlc.stop();vlc.detachViews();vlc.release();vlc=null;} if(lib!=null){lib.release();lib=null;}}
    }
}
