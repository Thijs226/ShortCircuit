package com.thijs226.shortcircuit;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.provider.Settings;

/** Persistent monotonic timers. Wall time only bounds hourly cooldown after reboot. */
public final class Prefs {
 private static BreakTimers timers(SharedPreferences p){return new BreakTimers(p.getLong("pause_elapsed_until",0),p.getLong("hourly_elapsed_until",0),p.getLong("hourly_wall_until",0));}
 private static SharedPreferences.Editor saveTimers(Context c,SharedPreferences.Editor edit,BreakTimers t,long now){
  return edit.putLong("pause_elapsed_until",t.pauseUntil()).putLong("hourly_elapsed_until",t.nextHourlyAt()).putLong("hourly_wall_until",t.hourlyWallAt()).putInt("timer_boot",Settings.Global.getInt(c.getContentResolver(),Settings.Global.BOOT_COUNT,-1)).putLong("saved_elapsed",now);
 }
 private static SharedPreferences checked(Context c){
  SharedPreferences p=c.getSharedPreferences("short_circuit",Context.MODE_PRIVATE);
  // Retire the v1.4 identity and cooldown. An existing puzzle break is retained.
  if(p.contains("single_signature")||p.contains("single_elapsed_until")||p.contains("single_wall_until")){
   p.edit().remove("single_signature").remove("single_elapsed_until").remove("single_wall_until").apply();
  }
  int boot=Settings.Global.getInt(c.getContentResolver(),Settings.Global.BOOT_COUNT,-1);
  long now=SystemClock.elapsedRealtime();
  if(p.getInt("timer_boot",boot)!=boot||now<p.getLong("saved_elapsed",0)){
   BreakTimers t=timers(p);t.afterReboot(now,System.currentTimeMillis());saveTimers(c,p.edit(),t,now).commit();
  }
  return p;
 }
 public static synchronized long remaining(Context c){return BlockPolicy.remaining(timers(checked(c)).pauseUntil(),SystemClock.elapsedRealtime());}
 public static synchronized long nextHourlyIn(Context c){return BlockPolicy.remaining(timers(checked(c)).nextHourlyAt(),SystemClock.elapsedRealtime());}
 public static synchronized boolean pauseActiveAt(Context c,long elapsed){return timers(checked(c)).activeAt(elapsed);}
 public static synchronized boolean startHourly(Context c){
  SharedPreferences p=checked(c);long now=SystemClock.elapsedRealtime();BreakTimers t=timers(p);
  if(!t.startHourly(now,System.currentTimeMillis()))return false;
  // The one-minute break and one-hour cooldown reach disk together.
  return saveTimers(c,p.edit(),t,now).commit();
 }
 public static synchronized long newPuzzle(Context c){
  SharedPreferences p=checked(c);long id=p.getLong("issued_puzzle",0)+1;
  p.edit().putLong("issued_puzzle",id).commit();return id;
 }
 public static synchronized boolean redeemed(Context c,long puzzle){return !BlockPolicy.mayRedeem(puzzle,checked(c).getLong("issued_puzzle",0),checked(c).getLong("redeemed_puzzle",0));}
 public static synchronized boolean earnPause(Context c,int level,long puzzle){
  SharedPreferences p=checked(c);
  if(!BlockPolicy.mayRedeem(puzzle,p.getLong("issued_puzzle",0),p.getLong("redeemed_puzzle",0)))return false;
  long now=SystemClock.elapsedRealtime();BreakTimers t=timers(p);
  SharedPreferences.Editor edit=p.edit().putLong("redeemed_puzzle",puzzle);
  if(!t.startPuzzle(now,BlockPolicy.minutes(level))){edit.commit();return false;}
  return saveTimers(c,edit,t,now).commit();
 }
 public static synchronized void endPause(Context c){
  SharedPreferences p=checked(c);BreakTimers t=timers(p);t.endPause();
  p.edit().putLong("pause_elapsed_until",t.pauseUntil()).apply();
 }
 public static SharedPreferences get(Context c){return c.getSharedPreferences("short_circuit",Context.MODE_PRIVATE);}
 private Prefs(){}
}
