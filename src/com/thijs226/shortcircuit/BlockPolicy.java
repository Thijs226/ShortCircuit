package com.thijs226.shortcircuit;

/** Pure feed-session rules. Time and Android persistence are supplied by the adapter. */
public final class BlockPolicy {
 public static final int ALLOW=0, START_HOURLY=1, BLOCK=2;
 private int mode; // 0 outside, 1 break session, 2 blocked
 public int observe(boolean player,boolean advance,boolean paused,boolean hourlyReady){
  if(!player){mode=0;return ALLOW;}
  if(paused){mode=1;return ALLOW;}
  if(mode==2)return BLOCK;
  if(mode==0){
   if(advance||!hourlyReady){mode=2;return BLOCK;}
   mode=1;return START_HOURLY;
  }
  // Expiry alone leaves the current Short playing. A new grant requires reentry.
  if(advance){mode=2;return BLOCK;}
  return ALLOW;
 }
 public static boolean mayRedeem(long puzzle,long issued,long redeemed){return puzzle>0&&puzzle<=issued&&puzzle>redeemed;}
 public static long remaining(long until,long now){return until>now?until-now:0;}
 public static int minutes(int level){return new int[]{2,5,7,10}[Math.max(0,Math.min(3,level))];}
}
