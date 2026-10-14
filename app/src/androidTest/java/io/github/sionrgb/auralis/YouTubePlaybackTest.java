package io.github.sionrgb.auralis;

import android.net.Uri;
import android.os.SystemClock;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class YouTubePlaybackTest {
    @Test public void musicAndShortLinkRouting(){
        assertEquals("K4DyBUG242c",YouTubeSource.parse(Uri.parse("https://music.youtube.com/watch?v=K4DyBUG242c&list=RDAMVMK4DyBUG242c")).videoId);
        assertEquals("aqz-KE-bpKQ",YouTubeSource.parse(Uri.parse("https://youtu.be/aqz-KE-bpKQ?si=example")).videoId);
        assertNull(YouTubeSource.parse(Uri.parse("https://youtube.com.evil.example/watch?v=aqz-KE-bpKQ")));
        assertNull(YouTubeSource.parse(Uri.parse("https://youtube.com/watch?v=%22%3E%3Cscript%3E")));
    }
    @Test public void youtubeActualPlayback() throws Exception {play("https://www.youtube.com/watch?v=aqz-KE-bpKQ","aqz-KE-bpKQ");}
    @Test public void musicActualPlayback() throws Exception {play("https://music.youtube.com/watch?v=K4DyBUG242c","K4DyBUG242c");}
    private void play(String source,String expectedId) throws Exception {
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
            scenario.onActivity(a->a.openSource(Uri.parse(source)));
            AtomicReference<String> latest=new AtomicReference<>("{}");boolean commanded=false,playing=false;
            for(int i=0;i<90;i++){
                SystemClock.sleep(500);scenario.onActivity(a->latest.set(a.testWebsiteSnapshot()));JSONObject value=new JSONObject(latest.get());
                if(!value.optBoolean("ready"))continue;
                if(!commanded){commanded=true;scenario.onActivity(a->a.testWebsiteControl("play",0));}
                if(expectedId.equals(value.optString("videoId"))&&value.optInt("state")==1&&value.optDouble("time")>1){playing=true;break;}
            }
            assertTrue("Official player did not advance: "+latest.get(),playing);
            scenario.onActivity(a->a.testWebsiteControl("pause",0));SystemClock.sleep(700);scenario.onActivity(a->latest.set(a.testWebsiteSnapshot()));
            assertEquals(2,new JSONObject(latest.get()).getInt("state"));
            scenario.onActivity(a->{a.testWebsiteControl("seek",10);a.testWebsiteControl("play",0);});
            JSONObject after=new JSONObject();
            for(int i=0;i<40;i++){SystemClock.sleep(400);scenario.onActivity(a->latest.set(a.testWebsiteSnapshot()));after=new JSONObject(latest.get());if(after.optInt("state")==1&&after.optDouble("time")>10.3)break;}
            assertTrue("Video did not resume after seek: "+latest.get(),after.getDouble("time")>10.3);assertEquals(1,after.getInt("state"));assertFalse(after.getBoolean("muted"));
            android.graphics.Bitmap capture=androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
            android.content.Context context=androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().getTargetContext();
            android.content.ContentValues values=new android.content.ContentValues();values.put(android.provider.MediaStore.Images.Media.DISPLAY_NAME,expectedId+"-"+BuildConfig.FLAVOR+".png");values.put(android.provider.MediaStore.Images.Media.MIME_TYPE,"image/png");values.put(android.provider.MediaStore.Images.Media.RELATIVE_PATH,"Pictures/Auralis-YouTube-QA");
            Uri output=context.getContentResolver().insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values);
            try(java.io.OutputStream stream=context.getContentResolver().openOutputStream(output)){capture.compress(android.graphics.Bitmap.CompressFormat.PNG,100,stream);}capture.recycle();
        }
    }
}
