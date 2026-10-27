package com.thijs226.shortcircuit;
/** Monotonic break deadlines and rolling hourly allowance. */
public final class BreakTimers {
 public static final long MINUTE=60000L,HOUR=3600000L;
 private long pauseUntil,nextHourlyAt,hourlyWallAt;
 public BreakTimers(long pauseUntil,long nextHourlyAt,long hourlyWallAt){this.pauseUntil=pauseUntil;this.nextHourlyAt=nextHourlyAt;this.hourlyWallAt=hourlyWallAt;}
 public long pauseUntil(){return pauseUntil;}
 public long nextHourlyAt(){return nextHourlyAt;}
 public long hourlyWallAt(){return hourlyWallAt;}
 public boolean activeAt(long at){return pauseUntil>at;}
 public boolean hourlyReady(long now){return nextHourlyAt<=now;}
 public boolean startHourly(long now,long wallNow){
  if(activeAt(now)||!hourlyReady(now))return false;
  pauseUntil=now+MINUTE;nextHourlyAt=now+HOUR;hourlyWallAt=wallNow+HOUR;return true;
 }
 public boolean startPuzzle(long now,int minutes){
  if(activeAt(now)||minutes<=0)return false;
  pauseUntil=now+minutes*MINUTE;return true;
 }
 public void endPause(){pauseUntil=0;}
 public void afterReboot(long now,long wallNow){
  pauseUntil=0;nextHourlyAt=now+Math.max(0,Math.min(HOUR,hourlyWallAt-wallNow));
 }
}
