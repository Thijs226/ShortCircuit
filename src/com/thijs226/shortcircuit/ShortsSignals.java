package com.thijs226.shortcircuit;
import java.util.Locale;

/** Deliberately avoids treating the ordinary Home Shorts shelf as a full player. */
public final class ShortsSignals {
 private boolean surface,selectedShorts;
 private int actionMask;
 private static String lower(String s){return s==null?"":s.toLowerCase(Locale.ROOT);}
 public void add(String id,String label,boolean selected,boolean visible){
  if(!visible)return;id=lower(id);label=lower(label).trim();
  if(id.contains("reel_recycler")||id.contains("reel_watch_player")||id.contains("reel_player_page")||id.contains("shorts_player")||id.contains("reel_watch_fragment_root")||id.contains("reel_watch_fragment_container"))surface=true;
  if(id.contains("reel_like")||id.contains("reel_dislike"))actionMask|=1;
  if(id.contains("reel_comment"))actionMask|=2;
  if(id.contains("reel_share"))actionMask|=4;
  if(selected&&(label.equals("shorts")||label.startsWith("shorts,")))selectedShorts=true;
  if(label.startsWith("dislike")||label.startsWith("niet leuk"))actionMask|=8;
  if(label.startsWith("remix"))actionMask|=16;
 }
 public boolean player(){return surface||Integer.bitCount(actionMask&7)>=2||(selectedShorts&&(actionMask&24)==24);}
 public static boolean videoTitleId(String id){
  id=lower(id);
  return (id.contains("reel")||id.contains("shorts"))&&(id.endsWith("_title")||id.endsWith("_title_text"))
   &&!ScrollSignal.excludedId(id)&&!id.contains("channel")&&!id.contains("audio")&&!id.contains("sound");
 }
 public static boolean feedScroll(String id,String clazz,int height,int screenHeight){
  id=lower(id);clazz=lower(clazz);
  if(id.contains("comment")||id.contains("chat")||id.contains("description")||id.contains("bottom_sheet"))return false;
  if(height<screenHeight*.4)return false;
  return id.contains("reel_recycler")||id.contains("shorts_pager")||id.contains("reel_pager")||id.contains("reel_player_page")||(id.contains("reel")&&clazz.contains("viewpager"));
 }
}
