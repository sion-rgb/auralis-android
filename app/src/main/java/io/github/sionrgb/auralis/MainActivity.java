package io.github.sionrgb.auralis;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.audiofx.Equalizer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.database.Cursor;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceError;
import org.json.JSONObject;
import android.widget.*;
import androidx.media3.common.Effect;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.effect.Brightness;
import androidx.media3.effect.Contrast;
import androidx.media3.effect.HslAdjustment;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.ui.PlayerView;
import androidx.activity.ComponentActivity;
import androidx.activity.OnBackPressedCallback;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@UnstableApi
public final class MainActivity extends ComponentActivity {
    private final int ink=Color.rgb(231,239,248), muted=Color.rgb(147,166,188), accent=Color.rgb(120,228,207), panel=Color.rgb(17,29,45);
    private ExoPlayer player;
    private PlayerView playerView;
    private FrameLayout stage;
    private WebView web;
    private TextView status, title, mediaTime;
    private LinearLayout root, toolbar, enhancements;
    private SeekBar position;
    private Equalizer equalizer;
    private EditionSupport edition;
    private Uri current;
    private YouTubeSource youtubeSource;
    private volatile String webSnapshot="{}";
    private double webTime, webDuration;
    private int webState=-1;
    private boolean compatibility, inWebsite, fullscreen, seeking, destroyed;
    private int preset, generation;
    private float brightness, contrast, saturation;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final ArrayList<Uri> playlist = new ArrayList<>();
    private final Runnable updatePosition = new Runnable() { public void run() {
        if(destroyed)return;
        long duration=inWebsite?(long)(webDuration*1000):player.getDuration(), at=inWebsite?(long)(webTime*1000):player.getCurrentPosition();
        if(!seeking&&duration>0)position.setProgress((int)(1000*at/duration));
        mediaTime.setText(time(at)+" / "+(duration>0?time(duration):"--:--"));
        handler.postDelayed(this,500);
    }};
    private String time(long ms){long s=Math.max(0,ms/1000);return String.format(Locale.ROOT,"%02d:%02d",s/60,s%60);}
    private int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    private GradientDrawable background(int color){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(12));return d;}
    private TextView text(String value,int size,int color){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setPadding(0,dp(4),0,dp(4));return t;}
    private Button button(String label,Runnable action){Button b=new Button(this);b.setText(label);b.setTextColor(ink);b.setTextSize(13);b.setAllCaps(false);b.setBackground(background(Color.rgb(27,41,58)));b.setPadding(dp(8),dp(4),dp(8),dp(4));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(46),1);p.setMargins(0,0,dp(6),0);b.setLayoutParams(p);b.setOnClickListener(v->action.run());return b;}
    private LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.HORIZONTAL);l.setPadding(0,dp(6),0,dp(6));return l;}
    @Override public void onCreate(Bundle saved){
        super.onCreate(saved);getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getOnBackPressedDispatcher().addCallback(this,new OnBackPressedCallback(true){@Override public void handleOnBackPressed(){if(fullscreen){toggleFullscreen();return;}if(web!=null&&web.canGoBack()){web.goBack();return;}finish();}});
        root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(Color.rgb(11,19,32));root.setPadding(dp(16),dp(12),dp(16),dp(8));
        root.setOnApplyWindowInsetsListener((view,insets)->{view.setPadding(dp(16)+insets.getSystemWindowInsetLeft(),dp(8)+insets.getSystemWindowInsetTop(),dp(16)+insets.getSystemWindowInsetRight(),dp(8)+insets.getSystemWindowInsetBottom());return insets;});
        TextView brand=text(BuildConfig.ENHANCED?"AURALIS":"AURALIS  LITE",24,ink);brand.setTypeface(null,Typeface.BOLD);root.addView(brand);
        TextView tagline=text(BuildConfig.ENHANCED?"你的隨身影音空間 · 音畫強化版":"輕巧播放 · 手機原生解碼",12,muted);root.addView(tagline);
        toolbar=row();toolbar.addView(button("＋ 檔案",()->pickFile()));toolbar.addView(button("↗ 連結",()->openLink()));toolbar.addView(button("清單",()->showPlaylist()));root.addView(toolbar);
        stage=new FrameLayout(this);stage.setBackgroundColor(Color.BLACK);root.addView(stage,new LinearLayout.LayoutParams(-1,dp(220)));
        player=new ExoPlayer.Builder(this).build();playerView=new PlayerView(this);playerView.setPlayer(player);stage.addView(playerView,new FrameLayout.LayoutParams(-1,-1));
        title=text("好聲音，好畫面。",17,ink);title.setMaxLines(2);root.addView(title);
        status=text("選擇影音檔案，或貼上網絡連結。",12,muted);root.addView(status);
        position=new SeekBar(this);position.setMax(1000);root.addView(position,new LinearLayout.LayoutParams(-1,dp(30)));
        position.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int v,boolean user){}public void onStartTrackingTouch(SeekBar s){seeking=true;}public void onStopTrackingTouch(SeekBar s){seeking=false;if(inWebsite){webControl("seek",webDuration*s.getProgress()/1000);}else if(player.getDuration()>0)player.seekTo(player.getDuration()*s.getProgress()/1000);}});
        mediaTime=text("00:00 / --:--",12,muted);root.addView(mediaTime);
        LinearLayout actions=row();actions.addView(button("−10 秒",()->seekRelative(-10)));actions.addView(button("▶ / Ⅱ",()->{if(inWebsite)webControl("toggle",0);else if(player.isPlaying())player.pause();else player.play();}));actions.addView(button("＋10 秒",()->seekRelative(10)));actions.addView(button("全螢幕",()->toggleFullscreen()));root.addView(actions);
        ScrollView scroll=new ScrollView(this);LinearLayout options=new LinearLayout(this);options.setOrientation(LinearLayout.VERTICAL);options.setPadding(0,dp(8),0,dp(10));scroll.addView(options);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout secondary=row();secondary.addView(button("字幕",()->pickSubtitle()));secondary.addView(button("音軌",()->chooseAudio()));secondary.addView(button("倍速",()->speedDialog()));if(BuildConfig.ENHANCED)secondary.addView(button("兼容播放",()->useCompatibility()));options.addView(secondary);
        LinearLayout webActions=row();webActions.addView(button("原網站",()->{if(current!=null)showWebsite(current,true);}));webActions.addView(button("原生串流",()->{if(current!=null)openSource(current,true);}));webActions.addView(button("說明",()->showHelp()));options.addView(webActions);
        if(BuildConfig.ENHANCED){
            enhancements=new LinearLayout(this);enhancements.setOrientation(LinearLayout.VERTICAL);options.addView(enhancements);
            TextView heading=text("調整你的體驗",17,accent);enhancements.addView(heading);
            LinearLayout presets=row();for(int p=0;p<4;p++){final int selected=p;presets.addView(button(new String[]{"原音","人聲","音樂","電影"}[p],()->{preset=selected;applyEqualizer();}));}enhancements.addView(presets);
            addSlider(enhancements,"亮度",value->{brightness=value;applyVideoEffects();});addSlider(enhancements,"對比",value->{contrast=value;applyVideoEffects();});addSlider(enhancements,"飽和度",value->{saturation=value*100;applyVideoEffects();});
            options.addView(text("本機及直接串流可調音畫；YouTube 官方播放由網站控制。",11,muted));
        }
        setContentView(root);edition=EditionFactory.create();
        player.addListener(new Player.Listener(){
            @Override public void onPlaybackStateChanged(int state){if(inWebsite)return;if(state==Player.STATE_READY)status.setText("已載入 · Media3 原生播放");if(state==Player.STATE_ENDED)next();}
            @Override public void onPlayerError(PlaybackException error){if(inWebsite)return;if(current!=null&&website(current)){showWebsite(current);return;}status.setText("原生播放失敗（"+error.getErrorCodeName()+"）。可嘗試兼容或網站模式。");}
            @Override public void onAudioSessionIdChanged(int id){if(equalizer!=null){equalizer.release();equalizer=null;}if(BuildConfig.ENHANCED&&id!=0){try{equalizer=new Equalizer(0,id);applyEqualizer();}catch(RuntimeException e){status.setText("此裝置不提供系統等化器。");}}}
        });
        handler.post(updatePosition);
        if(saved!=null&&saved.containsKey("source")){openSource(Uri.parse(saved.getString("source")));player.seekTo(saved.getLong("position"));}
        else handleIntent(getIntent());
    }
    private interface Adjustment{void apply(float v);}
    private void addSlider(LinearLayout parent,String label,Adjustment adjustment){parent.addView(text(label,13,ink));SeekBar slider=new SeekBar(this);slider.setMax(200);slider.setProgress(100);parent.addView(slider,new LinearLayout.LayoutParams(-1,dp(34)));slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int value,boolean user){if(user)adjustment.apply((value-100)/100f);}public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}});}
    private void applyVideoEffects(){if(compatibility||inWebsite){status.setText("畫面調整適用於原生 Media3 模式。");return;}ArrayList<Effect> effects=new ArrayList<>();if(brightness!=0)effects.add(new Brightness(brightness));if(contrast!=0)effects.add(new Contrast(contrast));if(saturation!=0)effects.add(new HslAdjustment.Builder().adjustSaturation(saturation).build());player.setVideoEffects(effects);}
    private void applyEqualizer(){if(inWebsite){status.setText("官方網站播放的音畫由網站控制，App 等化器未套用。");return;}if(compatibility){edition.equalizer(preset);return;}if(equalizer==null)return;try{short[] range=equalizer.getBandLevelRange();float[] centers={60,170,350,1000,3500,10000};for(short i=0;i<equalizer.getNumberOfBands();i++){float freq=equalizer.getCenterFreq(i)/1000f;int nearest=0;for(int j=1;j<6;j++)if(Math.abs(Math.log(freq/centers[j]))<Math.abs(Math.log(freq/centers[nearest])))nearest=j;short gain=(short)Math.max(range[0],Math.min(range[1],NativeCore.presetGain(preset,nearest)*100));equalizer.setBandLevel(i,gain);}equalizer.setEnabled(preset!=0);}catch(RuntimeException e){status.setText("此裝置無法套用等化器。");}}
    private void pickFile(){Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT);intent.addCategory(Intent.CATEGORY_OPENABLE);intent.setType("*/*");intent.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"video/*","audio/*","application/octet-stream"});intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true);startActivityForResult(intent,1);}
    private void pickSubtitle(){if(current==null)return;Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT);intent.addCategory(Intent.CATEGORY_OPENABLE);intent.setType("*/*");startActivityForResult(intent,2);}
    @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(result!=RESULT_OK||data==null)return;if(request==1){int first=playlist.size();if(data.getClipData()!=null){for(int i=0;i<data.getClipData().getItemCount();i++)playlist.add(data.getClipData().getItemAt(i).getUri());}else if(data.getData()!=null)playlist.add(data.getData());if(playlist.size()>first)openSource(playlist.get(first));}else if(request==2&&data.getData()!=null){if(compatibility||inWebsite){status.setText("字幕匯入適用於原生模式。");return;}MediaItem existing=player.getCurrentMediaItem();if(existing!=null){long at=player.getCurrentPosition();MediaItem.SubtitleConfiguration subtitle=new MediaItem.SubtitleConfiguration.Builder(data.getData()).setMimeType(androidx.media3.common.MimeTypes.APPLICATION_SUBRIP).setSelectionFlags(androidx.media3.common.C.SELECTION_FLAG_DEFAULT).build();player.setMediaItem(existing.buildUpon().setSubtitleConfigurations(java.util.Collections.singletonList(subtitle)).build());player.prepare();player.seekTo(at);player.play();}}}
    private void openLink(){EditText input=new EditText(this);input.setHint("https://…");input.setSingleLine(false);new AlertDialog.Builder(this).setTitle("網絡媒體連結").setView(input).setPositiveButton("播放",(d,w)->{String link=input.getText().toString().trim();if(NativeCore.sourceKind(link)<=0){status.setText("請使用 HTTP、HTTPS 或 RTSP 媒體連結；不要包含登入密碼。");return;}Uri uri=Uri.parse(link);playlist.add(uri);openSource(uri);}).setNegativeButton("取消",null).show();}
    private boolean website(Uri uri){String host=uri.getHost();return host!=null&&(host.equals("youtu.be")||host.equals("youtube.com")||host.endsWith(".youtube.com")||host.equals("bilibili.com")||host.endsWith(".bilibili.com")||host.equals("b23.tv"));}
    private void resetStage(){generation++;inWebsite=false;setEnhancementsEnabled(true);youtubeSource=null;webTime=0;webDuration=0;webState=-1;webSnapshot="{}";player.stop();edition.release();compatibility=false;if(web!=null){web.removeJavascriptInterface("AuralisBridge");web.stopLoading();web.destroy();web=null;}stage.removeAllViews();stage.addView(playerView,new FrameLayout.LayoutParams(-1,-1));}
    public void openSource(Uri source){
        openSource(source,false);
    }
    private void openSource(Uri source,boolean nativeRequested){
        if(source==null)return;
        String scheme=source.getScheme();if(!"content".equals(scheme)&&!"file".equals(scheme)&&NativeCore.sourceKind(source.toString())<=0){status.setText("不支援此來源。");return;}
        resetStage();current=source;title.setText(displayName(source));
        if(YouTubeSource.isYouTube(source)&&!nativeRequested){showWebsite(source);return;}
        if(website(source)){
            if(!BuildConfig.ENHANCED){showWebsite(source);return;}
            status.setText("正在解析網站媒體…");final int request=generation;
            worker.submit(()->{try{MediaSource resolved=edition.resolve(this,source.toString());handler.post(()->{if(!destroyed&&request==generation){player.setMediaSource(resolved);applyVideoEffects();player.prepare();player.play();}});}catch(Exception e){handler.post(()->{if(!destroyed&&request==generation){showWebsite(source);status.setText("原生解析受限，已切換官方網站播放。");}});}});
        }else{player.setMediaItem(MediaItem.fromUri(source));applyVideoEffects();player.prepare();player.play();status.setText("正在載入媒體…");}
    }
    private String displayName(Uri uri){if("content".equals(uri.getScheme())){try(Cursor cursor=getContentResolver().query(uri,new String[]{OpenableColumns.DISPLAY_NAME},null,null,null)){if(cursor!=null&&cursor.moveToFirst())return cursor.getString(0);}catch(Exception ignored){}}return uri.getHost()!=null?uri.getHost():uri.getLastPathSegment();}
    private void showWebsite(Uri uri){showWebsite(uri,false);}
    private void showWebsite(Uri uri,boolean originalSite){
        if(uri==null||!("https".equals(uri.getScheme())||"http".equals(uri.getScheme()))||NativeCore.sourceKind(uri.toString())<1){status.setText("網站模式只接受 HTTP／HTTPS 連結。");return;}
        resetStage();current=uri;inWebsite=true;setEnhancementsEnabled(false);youtubeSource=originalSite?null:YouTubeSource.parse(uri);
        web=new WebView(this);web.getSettings().setJavaScriptEnabled(true);web.getSettings().setDomStorageEnabled(true);web.getSettings().setMediaPlaybackRequiresUserGesture(false);
        web.getSettings().setAllowFileAccess(false);web.getSettings().setAllowContentAccess(false);web.getSettings().setSaveFormData(false);
        final int request=generation;
        web.setWebViewClient(new WebViewClient(){
            @Override public boolean shouldOverrideUrlLoading(WebView view,WebResourceRequest resource){Uri target=resource.getUrl();return !("https".equals(target.getScheme())||"http".equals(target.getScheme()));}
            @Override public void onReceivedError(WebView view,WebResourceRequest resource,WebResourceError error){if(resource.isForMainFrame()&&request==generation)status.setText("網站載入失敗："+error.getErrorCode()+"。可重試或使用原網站。");}
        });
        web.setWebChromeClient(new WebChromeClient());stage.addView(web,new FrameLayout.LayoutParams(-1,-1));
        if(youtubeSource!=null){
            web.addJavascriptInterface(new Object(){@JavascriptInterface public void report(String payload){
                if(payload==null||payload.length()>8192)return;
                handler.post(()->{if(destroyed||request!=generation||!inWebsite)return;try{
                    JSONObject value=new JSONObject(payload);webSnapshot=payload;
                    int error=value.optInt("error");if(error!=0){status.setText("YouTube 回報 "+error+"；按『原網站』可在 App 內開啟來源頁面。");return;}
                    if(!value.optBoolean("ready"))return;
                    webTime=value.optDouble("time",0);webDuration=value.optDouble("duration",0);webState=value.optInt("state",-1);
                    String name=value.optString("title");if(!name.isEmpty())title.setText(name);
                    status.setText(webState==1?"YouTube 官方播放中 · 網站音畫由來源控制":webState==3?"官方播放器緩衝中…":"按播放開始 · YouTube 官方播放器");
                }catch(org.json.JSONException ignored){}
                });
            }},"AuralisBridge");
            try(java.io.InputStream input=getAssets().open("youtube-player.html");java.io.ByteArrayOutputStream output=new java.io.ByteArrayOutputStream()){
                byte[] bytes=new byte[8192];int count;while((count=input.read(bytes))!=-1)output.write(bytes,0,count);
                String base="https://"+getPackageName().toLowerCase(Locale.ROOT)+"/youtube-player.html"+youtubeSource.query();
                web.loadDataWithBaseURL(base,output.toString("UTF-8"),"text/html","UTF-8",base);
            }catch(java.io.IOException e){status.setText("官方播放器資源缺失，改用原網站。");web.loadUrl(uri.toString());}
            status.setText("正在載入 YouTube 官方播放器…");
        }else{web.loadUrl(uri.toString());status.setText("原網站模式 · 使用網站內的播放控制");}
    }
    private void webControl(String command,Object value){if(web==null||youtubeSource==null){if(inWebsite)status.setText("請使用原網站內的播放控制。");return;}web.evaluateJavascript("auralisControl("+JSONObject.quote(command)+","+String.valueOf(JSONObject.wrap(value))+")",null);}
    private void seekRelative(double seconds){if(inWebsite)webControl("seek",Math.max(0,webTime+seconds));else player.seekTo(Math.max(0,player.getCurrentPosition()+(long)(seconds*1000)));}
    private void setEnhancementsEnabled(boolean enabled){if(enhancements!=null){enhancements.setAlpha(enabled?1f:.4f);enableChildren(enhancements,enabled);}}
    private void enableChildren(View view,boolean enabled){view.setEnabled(enabled);if(view instanceof android.view.ViewGroup){android.view.ViewGroup group=(android.view.ViewGroup)view;for(int i=0;i<group.getChildCount();i++)enableChildren(group.getChildAt(i),enabled);}}
    private void useCompatibility(){if(current==null)return;if(website(current)){status.setText("網站連結請使用原生解析或網站模式。");return;}Uri source=current;resetStage();compatibility=edition.playCompatibility(this,stage,source);if(compatibility){edition.equalizer(preset);status.setText("VLC 兼容播放 · 支援更多軟件解碼格式");}else status.setText("此版本不提供 VLC 兼容核心。");}
    private void speedDialog(){String[] labels={"0.5×","0.75×","1×","1.25×","1.5×","2×"};float[] values={.5f,.75f,1,1.25f,1.5f,2};new AlertDialog.Builder(this).setTitle("播放倍速").setItems(labels,(d,index)->{if(inWebsite)webControl("rate",values[index]);else player.setPlaybackSpeed(values[index]);}).show();}
    private void chooseAudio(){if(compatibility||inWebsite){status.setText("音軌選擇適用於原生模式。");return;}ArrayList<String> labels=new ArrayList<>();ArrayList<androidx.media3.common.TrackSelectionOverride> overrides=new ArrayList<>();for(androidx.media3.common.Tracks.Group group:player.getCurrentTracks().getGroups()){if(group.getType()!=androidx.media3.common.C.TRACK_TYPE_AUDIO)continue;for(int i=0;i<group.length;i++){androidx.media3.common.Format f=group.getTrackFormat(i);labels.add("音軌 "+(labels.size()+1)+(f.language==null?"":" · "+f.language));overrides.add(new androidx.media3.common.TrackSelectionOverride(group.getMediaTrackGroup(),i));}}if(labels.isEmpty()){status.setText("此來源沒有可選音軌。");return;}new AlertDialog.Builder(this).setTitle("音軌").setItems(labels.toArray(new String[0]),(d,index)->player.setTrackSelectionParameters(player.getTrackSelectionParameters().buildUpon().setOverrideForType(overrides.get(index)).build())).show();}
    private void showPlaylist(){if(playlist.isEmpty()){status.setText("播放清單尚未加入檔案。");return;}String[] items=new String[playlist.size()];for(int i=0;i<items.length;i++)items[i]=displayName(playlist.get(i));new AlertDialog.Builder(this).setTitle("播放清單").setItems(items,(d,index)->openSource(playlist.get(index))).setNegativeButton("返回",null).show();}
    private void next(){int at=playlist.indexOf(current);if(at>=0&&at+1<playlist.size())openSource(playlist.get(at+1));}
    private void toggleFullscreen(){fullscreen=!fullscreen;for(int i=0;i<root.getChildCount();i++)if(root.getChildAt(i)!=stage)root.getChildAt(i).setVisibility(fullscreen?View.GONE:View.VISIBLE);LinearLayout.LayoutParams p=(LinearLayout.LayoutParams)stage.getLayoutParams();p.height=fullscreen?0:dp(220);p.weight=fullscreen?1:0;stage.setLayoutParams(p);getWindow().getDecorView().setSystemUiVisibility(fullscreen?View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY:View.SYSTEM_UI_FLAG_VISIBLE);}
    private void showHelp(){new AlertDialog.Builder(this).setTitle("Auralis 手機播放器").setMessage("精簡版：本機、直接影音、HLS／DASH／RTSP，以及網站模式。\n強化版：增加 VLC 兼容播放、網站連結解析、音效風格、亮度、對比及飽和度。\n\n所有解碼能力以手機及播放核心為準；YouTube／嗶哩嗶哩可能要求登入或限制串流解析。\n網站模式不提供本程式音畫濾鏡。\n\n字幕匯入目前為 SRT。畫面調整不會修改原始檔案。\nAndroid 不提供 DLSS 5。\n\n停止到背景時暫停播放。沒有廣告、分析追蹤或自動儲存媒體歷史。").setPositiveButton("知道了",null).show();}
    private void handleIntent(Intent intent){if(Intent.ACTION_SEND.equals(intent.getAction())){String source=intent.getStringExtra(Intent.EXTRA_TEXT);if(source!=null&&NativeCore.sourceKind(source.trim())>0)openSource(Uri.parse(source.trim()));}else if(intent.getData()!=null)openSource(intent.getData());}
    @Override protected void onNewIntent(Intent intent){super.onNewIntent(intent);setIntent(intent);handleIntent(intent);}
    @Override public void onConfigurationChanged(Configuration c){super.onConfigurationChanged(c);}
    @Override protected void onSaveInstanceState(Bundle state){super.onSaveInstanceState(state);if(current!=null){state.putString("source",current.toString());state.putLong("position",player.getCurrentPosition());}}
    @Override protected void onStop(){super.onStop();player.pause();edition.pause();if(web!=null){webControl("pause",0);web.onPause();}}
    @Override protected void onResume(){super.onResume();if(web!=null)web.onResume();}
    @Override protected void onDestroy(){destroyed=true;generation++;handler.removeCallbacksAndMessages(null);worker.shutdownNow();if(web!=null)web.destroy();if(equalizer!=null)equalizer.release();edition.release();playerView.setPlayer(null);player.release();super.onDestroy();}
    // Instrumentation reads actual renderer state, not a success flag.
    public ExoPlayer testPlayer(){return player;}
    public void testEffects(){brightness=.1f;contrast=.15f;saturation=10;applyVideoEffects();preset=1;applyEqualizer();}
    public void testCompatibility(Uri source){current=source;useCompatibility();}
    public long testCompatibilityPosition(){return edition.compatibilityPosition();}
    public String testWebsiteSnapshot(){return webSnapshot;}
    public void testWebsiteControl(String command,Object value){webControl(command,value);}
}
