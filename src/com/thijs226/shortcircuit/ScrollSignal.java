package com.thijs226.shortcircuit;
import java.util.Locale;

/** Accessibility scroll data without Android objects, labels or video content. */
public final class ScrollSignal {
 public final long at;
 public final int position;
 private final String eventClass;
 private final int dx,dy;
 private boolean hasSource,feedAncestor,excluded;
 private String sourceId="",sourceClass="";
 private int width,height;
 public ScrollSignal(long at,String eventClass,int position,int dx,int dy){this.at=at;this.eventClass=lower(eventClass);this.position=position;this.dx=dx;this.dy=dy;}
 private static String lower(String value){return value==null?"":value.toLowerCase(Locale.ROOT);}
 public static boolean pagerClass(String clazz){String c=lower(clazz);return c.contains("recyclerview")||c.contains("viewpager");}
 public static boolean feedId(String id){
  String s=lower(id);
  return s.contains("reel_recycler")||s.contains("shorts_pager")||s.contains("reel_pager")||s.contains("reel_player_page")||s.contains("reel_watch_player")||s.contains("reel_watch_fragment")||s.contains("shorts_player");
 }
 public static boolean excludedId(String id){String s=lower(id);return s.contains("comment")||s.contains("chat")||s.contains("description")||s.contains("bottom_sheet")||s.contains("engagement_panel")||s.contains("search_results");}
 public static boolean overlayId(String id){
  String s=lower(id);
  return s.contains("bottom_sheet")||s.contains("engagement_panel")||s.contains("comments_recycler")||s.contains("comment_thread")||s.contains("live_chat")||s.contains("description_panel")||s.contains("description_list");
 }
 public void source(String id,String clazz,int width,int height){
  id=lower(id);clazz=lower(clazz);
  if(!hasSource){hasSource=true;sourceId=id;sourceClass=clazz;this.width=Math.max(0,width);this.height=Math.max(0,height);}
  else feedAncestor|=feedId(id);
  excluded|=excludedId(id);
 }
 public boolean isFeed(boolean player,boolean overlay,int screenWidth,int screenHeight){
  if(!player||overlay||excluded||Math.abs((long)dx)>Math.abs((long)dy))return false;
  // A source-less RecyclerView could be a comment list dismissed before handling.
  // ViewPager is narrower; anonymous RecyclerViews need source bounds/ancestry.
  if(!hasSource)return eventClass.contains("viewpager");
  if(feedId(sourceId))return true;
  boolean large=screenWidth>0&&screenHeight>0&&width>=screenWidth*.7&&height>=screenHeight*.6;
  // Test the source's own bounds, never a full-screen parent of a small carousel.
  return large&&(pagerClass(sourceClass)||pagerClass(eventClass)||feedAncestor);
 }
 public String describe(){return (hasSource?sourceId+" | "+sourceClass+" | "+width+"x"+height:"source unavailable")+" | event "+eventClass;}
}
