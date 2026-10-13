package io.github.sionrgb.auralis;
import android.net.Uri;
import android.os.SystemClock;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class PlayerSmokeTest {
    @Test public void vlcProresCompatibilityDecode(){
        org.junit.Assume.assumeTrue(BuildConfig.ENHANCED);
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
            scenario.onActivity(a->a.testCompatibility(Uri.parse("http://10.0.2.2:8766/prores.mov")));
            AtomicLong at=new AtomicLong();for(int i=0;i<75;i++){SystemClock.sleep(200);scenario.onActivity(a->at.set(a.testCompatibilityPosition()));if(at.get()>300)break;}
            assertTrue("VLC playback clock did not advance",at.get()>300);
        }
    }
    @Test public void sharedNativePolicy() {
        assertEquals(2, NativeCore.sourceKind("https://www.youtube.com/watch?v=test"));
        assertEquals(-1, NativeCore.sourceKind("https://u:p@example.com/a"));
        assertEquals(-1, NativeCore.sourceKind("javascript:alert(1)"));
        assertEquals(3f, NativeCore.presetGain(1,3),.001f);
    }
    @Test public void actualHlsDecodeAndEffects() {
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(a->a.openSource(Uri.parse("http://10.0.2.2:8765/hls/stream.m3u8")));
            AtomicInteger frames=new AtomicInteger();AtomicLong position=new AtomicLong();
            for(int i=0;i<60;i++){
                SystemClock.sleep(200);
                scenario.onActivity(a->{position.set(a.testPlayer().getCurrentPosition());if(a.testPlayer().getVideoDecoderCounters()!=null)frames.set(a.testPlayer().getVideoDecoderCounters().renderedOutputBufferCount);});
                if(frames.get()>5&&position.get()>300)break;
            }
            assertTrue("No video buffers rendered",frames.get()>5);
            assertTrue("Clock did not advance",position.get()>300);
            scenario.onActivity(a->{a.testPlayer().pause();a.testPlayer().seekTo(1500);a.testPlayer().setPlaybackSpeed(1.25f);if(BuildConfig.ENHANCED)a.testEffects();a.testPlayer().play();});
            SystemClock.sleep(1000);
            scenario.onActivity(a->{assertEquals(1.25f,a.testPlayer().getPlaybackParameters().speed,.01f);assertTrue(a.testPlayer().getCurrentPosition()>=1500);});
            try {
                android.graphics.Bitmap capture=androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
                android.content.Context context=androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().getTargetContext();
                android.content.ContentValues values=new android.content.ContentValues();values.put(android.provider.MediaStore.Images.Media.DISPLAY_NAME,"auralis-full.png");values.put(android.provider.MediaStore.Images.Media.MIME_TYPE,"image/png");values.put(android.provider.MediaStore.Images.Media.RELATIVE_PATH,"Pictures/Auralis-QA");
                android.net.Uri output=context.getContentResolver().insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values);
                try(java.io.OutputStream stream=context.getContentResolver().openOutputStream(output)){capture.compress(android.graphics.Bitmap.CompressFormat.PNG,100,stream);}capture.recycle();
            } catch(java.io.IOException e){throw new AssertionError("Evidence capture failed",e);}
        }
    }
}
