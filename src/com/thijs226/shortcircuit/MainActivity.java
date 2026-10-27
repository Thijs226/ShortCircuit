package com.thijs226.shortcircuit;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.util.Locale;
import java.util.Random;

public final class MainActivity extends Activity {
 private static final int BG=0xff0b171c,CARD=0xff162930,EDGE=0xff2e454a,MINT=0xffa4edcb,WHITE=0xfff1f5eb,MUTED=0xffa7bcb9,GOLD=0xffefc47e;
 private static final String[] NAMES={"Easy","Normal","Hard","Demon"};
 private final Handler handler=new Handler(Looper.getMainLooper());
 private LinearLayout root;
 private TextView status,countdown,hourly,moves;
 private View endButton;
 private MazeView maze;
 private int screen,level=1;
 private long puzzleId;
 private final Runnable ticker=new Runnable(){public void run(){refresh();handler.postDelayed(this,1000);}};
 @Override public void onCreate(Bundle state){
  super.onCreate(state);getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);
  if(state!=null&&state.getInt("screen")==1){level=state.getInt("level",1);puzzle(level,state);}
  else if(state!=null&&state.getInt("screen")==2&&Prefs.remaining(this)>0)unlocked();else home();
 }
 @Override protected void onResume(){super.onResume();handler.removeCallbacks(ticker);handler.post(ticker);if(screen==1&&maze!=null)maze.resumeIfSolved();}
 @Override protected void onPause(){handler.removeCallbacks(ticker);if(maze!=null)maze.stop();super.onPause();}
 @Override protected void onDestroy(){handler.removeCallbacksAndMessages(null);if(maze!=null)maze.stop();super.onDestroy();}
 @Override protected void onSaveInstanceState(Bundle state){
  super.onSaveInstanceState(state);state.putInt("screen",screen);state.putInt("level",level);state.putLong("puzzle_id",puzzleId);
  if(screen==1&&maze!=null){state.putIntArray("cells",maze.model().cells());state.putIntArray("initial",maze.model().initial());state.putInt("moves",maze.model().moves());}
 }
 private int dp(float value){return Math.round(value*getResources().getDisplayMetrics().density);}
 private String clock(long ms){long s=Math.max(0,(ms+999)/1000);return String.format(Locale.US,"%d:%02d",s/60,s%60);}
 private GradientDrawable background(int color,int stroke){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(20));d.setStroke(dp(1),stroke);return d;}
 private LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
 private TextView text(String value,int size,int color,boolean bold){TextView v=new TextView(this);v.setText(value);v.setTextSize(size);v.setTextColor(color);v.setLineSpacing(dp(2),1);v.setFontFeatureSettings("kern");if(bold)v.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));return v;}
 private void add(LinearLayout parent,View view,int bottom){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(bottom);parent.addView(view,p);}
 private LinearLayout card(){LinearLayout l=column();l.setPadding(dp(19),dp(18),dp(19),dp(18));l.setBackground(background(CARD,EDGE));return l;}
 private TextView button(String label,boolean primary,Runnable click){TextView v=text(label,15,primary?BG:WHITE,true);v.setGravity(Gravity.CENTER);v.setPadding(dp(12),dp(14),dp(12),dp(14));v.setMinHeight(dp(50));v.setBackground(background(primary?MINT:0xff233b42,primary?MINT:EDGE));v.setOnClickListener(w->click.run());v.setFocusable(true);return v;}
 private void shell(boolean scroll){
  if(maze!=null){maze.stop();maze=null;}
  status=null;countdown=null;hourly=null;moves=null;endButton=null;
  root=column();root.setPadding(dp(22),dp(24),dp(22),dp(22));root.setBackgroundColor(BG);
  if(scroll){ScrollView view=new ScrollView(this);view.setFillViewport(true);view.setVerticalScrollBarEnabled(false);view.addView(root);setContentView(view);}else setContentView(root);
 }
 private boolean serviceOn(){String s=Settings.Secure.getString(getContentResolver(),Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);return s!=null&&(s.contains("com.thijs226.shortcircuit/.ShortsService")||s.contains("com.thijs226.shortcircuit/com.thijs226.shortcircuit.ShortsService"));}
 private void home(){
  screen=0;shell(true);
  add(root,text("SHORT CIRCUIT     /     01",12,MINT,true),13);
  add(root,text("A little less\nendless.",38,WHITE,true),8);
  add(root,text("Keep the good videos. Take back the scroll.",14,MUTED,false),22);
  LinearLayout guard=card();status=text("",13,MINT,true);add(guard,status,8);
  add(guard,text("Enjoy a free minute of Shorts once per hour. It starts automatically when you open Shorts, and you can scroll freely during that minute.",15,WHITE,false),14);
  add(guard,button("Set up blocking",false,this::setup),0);add(root,guard,12);
  LinearLayout timer=card();add(timer,text("YOUR SCROLLING BREAK",11,MUTED,true),5);
  countdown=text("",32,MINT,true);add(timer,countdown,7);
  hourly=text("",13,GOLD,true);add(timer,hourly,9);
  add(timer,text("When a break ends, finish the current Short. Your next swipe returns to YouTube Home.",13,MUTED,false),10);
  endButton=button("End break now",false,()->{Prefs.endPause(this);refresh();});add(timer,endButton,0);add(root,timer,25);
  add(root,text("MAKE A CONNECTION",12,MINT,true),6);
  add(root,text("Solve a maze. Earn a break.",23,WHITE,true),7);
  add(root,text("Connect the blue ball to the gold goal. Bigger mazes have more empty spaces to move through.",14,MUTED,false),16);
  LinearLayout first=new LinearLayout(this);first.addView(levelCard(0),half(8));first.addView(levelCard(1),half(0));add(root,first,8);
  LinearLayout second=new LinearLayout(this);second.addView(levelCard(2),half(8));second.addView(levelCard(3),half(0));add(root,second,18);
  add(root,button("Open YouTube",true,this::openYouTube),12);
  TextView help=text("Setup help & privacy",13,MUTED,false);help.setGravity(Gravity.CENTER);help.setPadding(0,dp(8),0,dp(8));help.setOnClickListener(v->help());add(root,help,0);
  refresh();
 }
 private LinearLayout.LayoutParams half(int right){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1);p.rightMargin=dp(right);return p;}
 private View levelCard(int index){
  LinearLayout c=column();c.setPadding(dp(16),dp(16),dp(12),dp(16));c.setBackground(background(index==1?0xff25443f:CARD,index==1?0xff73b69a:EDGE));
  add(c,text(new String[]{"● ○ ○ ○","● ● ○ ○","● ● ● ○","● ● ● ●"}[index],11,index==3?GOLD:MINT,true),10);
  add(c,text(NAMES[index],19,WHITE,true),3);add(c,text(BlockPolicy.minutes(index)+" min break",13,index==1?MINT:MUTED,false),5);
  add(c,text((index+3)+" × "+(index+3)+"  ·  "+MazeModel.gapsForSize(index+3)+" empty",11,MUTED,false),0);
  c.setContentDescription(NAMES[index]+", "+BlockPolicy.minutes(index)+" minute break");c.setFocusable(true);
  c.setOnClickListener(v->{if(Prefs.remaining(this)>0){Toast.makeText(this,"Finish your current break first.",Toast.LENGTH_SHORT).show();return;}puzzle(index,null);});return c;
 }
 private void puzzle(int difficulty,Bundle saved){
  level=Math.max(0,Math.min(3,difficulty));
  puzzleId=saved==null?Prefs.newPuzzle(this):saved.getLong("puzzle_id",0);
  if(saved!=null&&Prefs.redeemed(this,puzzleId)){if(Prefs.remaining(this)>0)unlocked();else home();return;}
  screen=1;shell(false);int n=level+3;
  add(root,text("CONNECT TO DISCONNECT",11,MINT,true),11);
  add(root,text(NAMES[level]+" maze",30,WHITE,true),5);
  add(root,text("A connected path earns "+BlockPolicy.minutes(level)+" minutes of Shorts.",14,MUTED,false),12);
  moves=text("",13,GOLD,true);add(root,moves,4);
  MazeModel model=null;
  if(saved!=null&&saved.getIntArray("cells")!=null){try{model=new MazeModel(n,saved.getIntArray("cells"));model.restoreInitial(saved.getIntArray("initial"),saved.getInt("moves"));if(model.gapCount()!=MazeModel.gapsForSize(n)){model=null;puzzleId=Prefs.newPuzzle(this);}}catch(IllegalArgumentException ignored){}}
  if(model==null)model=MazeModel.generate(n,MazeModel.scrambleForSize(n),new Random());
  maze=new MazeView(this,model,()->{if(Prefs.earnPause(this,level,puzzleId)||Prefs.remaining(this)>0)unlocked();else home();},this::updateMoves);
  root.addView(maze,new LinearLayout.LayoutParams(-1,0,1));
  add(root,text("Swipe a tile toward a gap. Or tap a tile, then choose a highlighted empty space.",13,MUTED,false),14);
  LinearLayout controls=new LinearLayout(this);controls.addView(button("Undo",false,()->maze.undo()),half(8));controls.addView(button("Reset",false,()->maze.reset()),half(0));add(root,controls,8);
  add(root,button("New maze",false,()->puzzle(level,null)),8);
  TextView back=button("Back without a break",false,this::home);add(root,back,0);
  updateMoves();maze.resumeIfSolved();
 }
 private void updateMoves(){if(moves!=null&&maze!=null)moves.setText(maze.model().moves()+" MOVES     ·     "+maze.model().gapCount()+" EMPTY SPACES");}
 private void unlocked(){
  screen=2;shell(true);
  add(root,text("CIRCUIT COMPLETE",12,MINT,true),32);
  add(root,text("Connected.\nNow take a break.",35,WHITE,true),14);
  add(root,text("Your timer starts now. Enjoy your Shorts, then let the guard take over on the next swipe.",16,MUTED,false),26);
  LinearLayout timer=card();countdown=text("",60,MINT,true);countdown.setGravity(Gravity.CENTER);countdown.setPadding(0,dp(24),0,dp(24));add(timer,countdown,3);TextView label=text("TIME TO SCROLL",11,MUTED,true);label.setGravity(Gravity.CENTER);add(timer,label,10);add(root,timer,24);
  add(root,button("Open YouTube",true,this::openYouTube),10);add(root,button("Back to dashboard",false,this::home),0);refresh();
 }
 private void refresh(){
  long remaining=Prefs.remaining(this),nextHourly=Prefs.nextHourlyIn(this);
  if(status!=null){status.setText(serviceOn()?"●  GUARD ENABLED":"○  BLOCKING NEEDS SETUP");status.setTextColor(serviceOn()?MINT:GOLD);}
  if(countdown!=null)countdown.setText(remaining>0?clock(remaining):screen==2?"0:00":nextHourly==0?"Free minute ready":"Scrolling blocked");
  if(hourly!=null)hourly.setText(nextHourly==0?"Free minute ready for your next Shorts visit":"Next free minute in "+clock(nextHourly));
  if(endButton!=null)endButton.setVisibility(remaining>0?View.VISIBLE:View.GONE);
 }
 private void setup(){
  new AlertDialog.Builder(this).setTitle("Enable your Shorts guard")
   .setMessage("Short Circuit needs Accessibility access to detect the YouTube Shorts player and tap Home when your allowance ends.\n\nIt only processes YouTube's interface locally. It does not save video titles, send data, or access your account.\n\nIn the next screen, select Short Circuit blocker and turn it on.")
   .setPositiveButton("Open settings",(d,w)->startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))).setNegativeButton("Cancel",null).show();
 }
 private void help(){
  new AlertDialog.Builder(this).setTitle("Setup & privacy")
   .setMessage("1. Install this APK.\n2. Open Set up blocking and enable Short Circuit blocker under Accessibility.\n3. If Android says Restricted setting, open App info, tap the three-dot menu and choose Allow restricted settings. Then enable Accessibility again.\n\nAll processing stays on your phone. No network permission, ads, analytics or login. You may disable the service or uninstall at any time.\n\nWorks with the official YouTube Android app. YouTube interface changes may affect detection. Normal videos stay available. If Android stops the service, check its battery settings.\n\nFree break: one minute, starting automatically when you open Shorts. The next free minute is available 60 minutes after its start. Leaving YouTube does not stop the timer, and reopening does not reset it. You can watch any Shorts during the minute. When time ends, finish the current Short; the next swipe returns to Home. No video identity or fingerprint is stored.\n\nBreaks: Easy 2 min, Normal 5 min, Hard 7 min, Demon 10 min. They start on completion and do not stack. Restarting the phone ends an active break.")
   .setPositiveButton("App info",(d,w)->startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+getPackageName())))).setNeutralButton("Copy diagnostics",(d,w)->copyDiagnostics()).setNegativeButton("Close",null).show();
 }
 private void copyDiagnostics(){
  String youtube="not installed";
  try{youtube=getPackageManager().getPackageInfo("com.google.android.youtube",0).versionName;}catch(android.content.pm.PackageManager.NameNotFoundException ignored){}
  String report="Short Circuit 1.5\nAndroid API: "+android.os.Build.VERSION.SDK_INT+"\nYouTube: "+youtube+"\nGuard enabled: "+serviceOn()+"\n"+Prefs.get(this).getString("guard_diagnostic","No observations yet.")+"\nLast scroll source: "+Prefs.get(this).getString("guard_event","none");
  android.content.ClipboardManager clipboard=(android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
  clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Short Circuit diagnostics",report));
  Toast.makeText(this,"Copied diagnostics. No video titles or account details included.",Toast.LENGTH_LONG).show();
 }
 private void openYouTube(){
  if(!serviceOn()){new AlertDialog.Builder(this).setTitle("Blocking is off").setMessage("Enable Accessibility first so the Shorts guard can work.").setPositiveButton("Set up",(d,w)->setup()).setNegativeButton("Cancel",null).show();return;}
  Intent intent=getPackageManager().getLaunchIntentForPackage("com.google.android.youtube");if(intent==null)Toast.makeText(this,"Install the official YouTube app first.",Toast.LENGTH_LONG).show();else startActivity(intent);
 }
 @Override public void onBackPressed(){if(screen!=0)home();else super.onBackPressed();}
}
