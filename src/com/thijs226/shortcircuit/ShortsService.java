package com.thijs226.shortcircuit;
import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Intent;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.os.Build;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.ArrayDeque;
import java.util.Locale;

/** Local, package-filtered Shorts adapter. Never records titles or screen content. */
public final class ShortsService extends AccessibilityService {
 private static final String YOUTUBE="com.google.android.youtube";
 private HandlerThread thread;
 private Handler worker;
 private final BlockPolicy policy=new BlockPolicy();
 private boolean wasPlayer,exiting;
 private long lastPosted,missingSince;
 private final FeedTracker transitions=new FeedTracker();
 private final Runnable poll=()->inspectEvent(null);
 private int scrollEvents,feedEvents,advances;
 private static final class Snapshot {
  final ShortsSignals signals=new ShortsSignals();
  String title="";
  boolean overlay;
  AccessibilityNodeInfo home;
  boolean player(){return signals.player();}
  String key(){return title;}
  void recycle(){if(home!=null)home.recycle();}
 }
 @Override protected void onServiceConnected(){
  super.onServiceConnected();
  AccessibilityServiceInfo config=getServiceInfo();
  if(config!=null){config.flags|=AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS|AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS;setServiceInfo(config);}
  if(thread==null){thread=new HandlerThread("short-circuit-observer");thread.start();worker=new Handler(thread.getLooper());}
  Prefs.get(this).edit().putBoolean("service_connected",true).putString("guard_diagnostic","Waiting for a Shorts session.").remove("guard_event").apply();
 }
 @Override public void onAccessibilityEvent(AccessibilityEvent event){
  if(worker==null||event==null||!YOUTUBE.contentEquals(event.getPackageName()==null?"":event.getPackageName()))return;
  long now=SystemClock.elapsedRealtime();
  boolean scroll=event.getEventType()==AccessibilityEvent.TYPE_VIEW_SCROLLED||event.getEventType()==AccessibilityEvent.TYPE_VIEW_SELECTED;
  if(!scroll&&now-lastPosted<140)return;
  lastPosted=now;
  // AccessibilityEvent time uses uptime; persistent timers use elapsed realtime.
  long eventAt=Math.max(0,now-Math.max(0,SystemClock.uptimeMillis()-event.getEventTime()));
  // Capture source evidence while the event is delivered. A deferred getSource()
  // may lose a comments list after its sheet has been dismissed.
  ScrollSignal scrollSignal=readScroll(event,eventAt);
  worker.post(()->inspectEvent(scrollSignal));
 }
 private void inspectEvent(ScrollSignal scroll){
  if(exiting)return;
  worker.removeCallbacks(poll);
  AccessibilityNodeInfo root=getRootInActiveWindow();
  if(root==null){if(wasPlayer)worker.postDelayed(poll,600);return;}
  if(!YOUTUBE.contentEquals(root.getPackageName()==null?"":root.getPackageName())){
   root.recycle();leavePlayer();return;
  }
  Snapshot s=scan(root);root.recycle();
  long now=SystemClock.elapsedRealtime();boolean player=s.player();
  if(!player){
   if(s.overlay&&wasPlayer){worker.postDelayed(poll,400);s.recycle();return;}
   if(missingSince==0)missingSince=now;
   // Content trees can temporarily disappear while loading or opening comments.
   if(s.home!=null||now-missingSince>=1100)leavePlayer();
   else if(wasPlayer)worker.postDelayed(poll,400);
   s.recycle();return;
  }
  missingSince=0;
  long advancedAt=-1;
  boolean feed=scroll!=null&&scroll.isFeed(player,s.overlay,getResources().getDisplayMetrics().widthPixels,getResources().getDisplayMetrics().heightPixels);
  if(!wasPlayer){
   transitions.reset(s.key(),now);if(feed)transitions.observe(s.key(),true,scroll.position,now,now);
  }
  else advancedAt=transitions.observe(s.overlay?"":s.key(),feed,feed?scroll.position:-1,feed?scroll.at:now,now);
  wasPlayer=true;
  boolean paused=advancedAt>=0?Prefs.pauseActiveAt(this,advancedAt):Prefs.remaining(this)>0;
  int decision=policy.observe(true,advancedAt>=0,paused,Prefs.nextHourlyIn(this)==0);
  if(decision==BlockPolicy.START_HOURLY){
   if(Prefs.startHourly(this))paused=true;
   else if(Prefs.remaining(this)>0)decision=BlockPolicy.ALLOW;
   else decision=BlockPolicy.BLOCK;
  }
  if(scroll!=null){scrollEvents++;if(feed)feedEvents++;}
  if(advancedAt>=0)advances++;
  if(scroll!=null||advancedAt>=0||decision!=BlockPolicy.ALLOW){
   android.content.SharedPreferences.Editor log=Prefs.get(this).edit();
   log.putString("guard_diagnostic","Scroll events: "+scrollEvents+"\nFeed events: "+feedEvents+"\nDetected advances: "+advances+"\nLast action: "+(decision==BlockPolicy.BLOCK?"return to Home":decision==BlockPolicy.START_HOURLY?"hourly minute started":paused?"break active":"finishing current Short")+"\nBreak remaining ms: "+Prefs.remaining(this)+"\nNext hourly minute ms: "+Prefs.nextHourlyIn(this)+"\nTitle available: "+!s.title.isEmpty()+"\nSheet visible: "+s.overlay);
   if(scroll!=null)log.putString("guard_event",scroll.describe());log.apply();
  }
  if(decision==BlockPolicy.BLOCK){exiting=true;navigateHome(0);}
  else worker.postDelayed(poll,400);
  s.recycle();
 }
 private void leavePlayer(){
  wasPlayer=false;missingSince=0;policy.observe(false,false,false,false);
 }
 private ScrollSignal readScroll(AccessibilityEvent event,long eventAt){
  if(event==null)return null;
  int type=event.getEventType();
  if(type!=AccessibilityEvent.TYPE_VIEW_SCROLLED&&type!=AccessibilityEvent.TYPE_VIEW_SELECTED)return null;
  int position=event.getCurrentItemIndex()>=0?event.getCurrentItemIndex():event.getFromIndex();
  ScrollSignal result=new ScrollSignal(eventAt,safe(event.getClassName()),position,Build.VERSION.SDK_INT>=28?event.getScrollDeltaX():0,Build.VERSION.SDK_INT>=28?event.getScrollDeltaY():0);
  AccessibilityNodeInfo node=Build.VERSION.SDK_INT>=33?event.getSource(AccessibilityNodeInfo.FLAG_PREFETCH_ANCESTORS):event.getSource();
  for(int depth=0;node!=null&&depth<14;depth++){
   String id=safe(node.getViewIdResourceName());
   Rect rect=new Rect();node.getBoundsInScreen(rect);
   result.source(id,safe(node.getClassName()),rect.width(),rect.height());
   AccessibilityNodeInfo parent=node.getParent();node.recycle();node=parent;
  }
  if(node!=null)node.recycle();return result;
 }
 private Snapshot scan(AccessibilityNodeInfo root){
  Snapshot result=new Snapshot();ArrayDeque<AccessibilityNodeInfo> q=new ArrayDeque<>();q.add(AccessibilityNodeInfo.obtain(root));int seen=0;
  while(!q.isEmpty()&&seen++<800){
   AccessibilityNodeInfo node=q.remove();
   if(node.isVisibleToUser()){
    String id=safe(node.getViewIdResourceName());String label=safe(node.getText());
    if(label.isEmpty())label=safe(node.getContentDescription());
    result.signals.add(id,label,node.isSelected(),true);
    if(ScrollSignal.overlayId(id)){
     Rect bounds=new Rect();node.getBoundsInScreen(bounds);
     if(bounds.height()>=getResources().getDisplayMetrics().heightPixels*.2&&bounds.width()>=getResources().getDisplayMetrics().widthPixels*.5)result.overlay=true;
    }
    if(result.home==null&&isHome(label)){
     AccessibilityNodeInfo target=clickTarget(node);
     if(target!=null)result.home=target;
    }
    if(result.title.isEmpty()&&ShortsSignals.videoTitleId(id))result.title=label;
   }
   // Included layout wrappers may be non-visible while exposing visible descendants.
   for(int i=0;i<node.getChildCount();i++){AccessibilityNodeInfo child=node.getChild(i);if(child!=null)q.add(child);}
   node.recycle();
  }
  while(!q.isEmpty())q.remove().recycle();return result;
 }
 private static AccessibilityNodeInfo clickTarget(AccessibilityNodeInfo original){
  AccessibilityNodeInfo node=AccessibilityNodeInfo.obtain(original);
  for(int i=0;i<4&&node!=null;i++){
   if(node.isClickable())return node;
   AccessibilityNodeInfo parent=node.getParent();node.recycle();node=parent;
  }
  if(node!=null)node.recycle();return null;
 }
 private static boolean isHome(String text){
  return text.equals("home")||text.equals("startpagina")||text.equals("startseite")||text.equals("inicio")||text.equals("accueil")||text.equals("home, tab, 1 of 5")||text.startsWith("home, tab,")||text.startsWith("startpagina, tab,");
 }
 private static String safe(CharSequence text){return text==null?"":text.toString().trim().toLowerCase(Locale.ROOT);}
 private void navigateHome(int attempt){
  AccessibilityNodeInfo root=getRootInActiveWindow();
  if(root==null){finishExit();return;}
  if(!YOUTUBE.contentEquals(root.getPackageName()==null?"":root.getPackageName())){root.recycle();finishExit();return;}
  Snapshot s=scan(root);root.recycle();
  boolean clicked=false;
  if(s.home!=null)clicked=s.home.performAction(AccessibilityNodeInfo.ACTION_CLICK);
  boolean stillPlayer=s.player();s.recycle();
  if(!stillPlayer&&clicked){finishExit();return;}
  if(attempt>=3){
   // Explicit YouTube URL fallback, only while YouTube is the active app.
   try{Intent i=new Intent(Intent.ACTION_VIEW,Uri.parse("https://www.youtube.com/"));i.setPackage(YOUTUBE);i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);startActivity(i);}catch(RuntimeException ignored){}
   finishExit();return;
  }
  if(!clicked||attempt>0)performGlobalAction(GLOBAL_ACTION_BACK);
  worker.postDelayed(()->navigateHome(attempt+1),350);
 }
 private void finishExit(){leavePlayer();worker.postDelayed(()->{exiting=false;inspectEvent(null);},650);}
 @Override public void onInterrupt(){if(worker!=null)worker.removeCallbacks(poll);}
 @Override public void onDestroy(){
  Prefs.get(this).edit().putBoolean("service_connected",false).apply();
  if(worker!=null)worker.removeCallbacksAndMessages(null);if(thread!=null)thread.quitSafely();super.onDestroy();
 }
}
