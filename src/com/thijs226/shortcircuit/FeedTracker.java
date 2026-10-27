package com.thijs226.shortcircuit;

/** Coalesces scroll and delayed title callbacks into a single video transition. */
public final class FeedTracker {
 private String stable="",candidate="";
 private long entered,candidateAt,lastScroll,lastCallback;
 private int lastPosition=-1;
 private boolean awaitingTitle,positionSettled;
 public void reset(String key,long now){stable=key==null?"":key;candidate="";entered=now;lastScroll=lastCallback=Long.MIN_VALUE/2;lastPosition=-1;awaitingTitle=false;positionSettled=false;}
 public long observe(String key,boolean scroll,long now){return observe(key,scroll,-1,now,now);}
 /** eventAt is captured before queueing work. now is the current tree observation time. */
 public long observe(String key,boolean scroll,int position,long eventAt,long now){
  key=key==null?"":key;
  if(scroll&&eventAt>=entered&&eventAt>=lastCallback){
   boolean changed=position>=0&&lastPosition>=0&&position!=lastPosition;
   if(position>=0)lastPosition=position;
   // Bound a callback burst from its start as well as its last callback. Otherwise
   // anonymous rapid swipes can refresh a trailing debounce forever after expiry.
   boolean burst=eventAt-lastCallback<450&&(!key.isEmpty()||eventAt-lastScroll<900);
   boolean duplicate=burst&&!changed;
   lastCallback=eventAt;
   if(burst&&changed&&!positionSettled){
    // RecyclerView may report the old first-visible index at gesture start, then
    // its new index later. That is still the transition already reported.
    positionSettled=true;duplicate=true;
    if(!key.isEmpty()&&!key.equals(stable)){stable=key;candidate="";awaitingTitle=false;}
   }
   if(!duplicate&&(changed||eventAt-entered>450)){
    lastScroll=eventAt;candidate="";
    // The tree title is inspected later than the event's index. A new title is
    // not evidence that this earlier index has settled yet.
    positionSettled=changed;
    awaitingTitle=key.isEmpty()||key.equals(stable);
    if(!key.isEmpty())stable=key;
    return eventAt;
   }
  }
  if(key.isEmpty()||key.equals(stable)){candidate="";return -1;}
  if(stable.isEmpty()||now-entered<=700){stable=key;candidate="";return -1;}
  if(awaitingTitle&&now-lastScroll<=2000){stable=key;candidate="";awaitingTitle=false;return -1;}
  if(!key.equals(candidate)){candidate=key;candidateAt=now;return -1;}
  if(now-candidateAt<250)return -1;
  stable=key;candidate="";awaitingTitle=false;return candidateAt;
 }
}
