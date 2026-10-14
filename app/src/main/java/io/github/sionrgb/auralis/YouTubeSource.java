package io.github.sionrgb.auralis;

import android.net.Uri;

/** Routes Music/watch/short/playlist links through the same official player. */
public final class YouTubeSource {
    public final String videoId, playlistId;
    private YouTubeSource(String video, String list) {videoId=video;playlistId=list;}
    public static boolean isYouTube(Uri uri) {
        String host=uri.getHost();
        return host!=null && (host.equals("youtu.be")||host.equals("youtube.com")||host.endsWith(".youtube.com"));
    }
    public static YouTubeSource parse(Uri uri) {
        if(!isYouTube(uri))return null;
        String video=uri.getQueryParameter("v"),list=uri.getQueryParameter("list");
        java.util.List<String> path=uri.getPathSegments();
        if(uri.getHost().equals("youtu.be")&&!path.isEmpty())video=path.get(0);
        else if(path.size()>1&&(path.get(0).equals("embed")||path.get(0).equals("shorts")||path.get(0).equals("live")))video=path.get(1);
        if(video==null||!video.matches("[A-Za-z0-9_-]{11}"))video="";
        if(list==null||!list.matches("[A-Za-z0-9_-]{10,200}"))list="";
        return video.isEmpty()&&list.isEmpty()?null:new YouTubeSource(video,list);
    }
    public String query() {return "?v="+Uri.encode(videoId)+"&list="+Uri.encode(playlistId);}
}
