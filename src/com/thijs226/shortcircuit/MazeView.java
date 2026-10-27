package com.thijs226.shortcircuit;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.*;
import android.view.*;
import android.view.animation.DecelerateInterpolator;

/** Textured sliding tiles, explicit empty-space selection and a rolling glass ball. */
public final class MazeView extends View {
 private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
 private final MazeArt art;
 private final MazeChannel channel=new MazeChannel();
 private final MazeModel model;
 private final Runnable completed,changed;
 private float cell,ox,oy,progress,downX,downY,slideProgress;
 private boolean animating,cancelled,delivered,sliding,slideCancelled;
 private int selected=-1,slideFrom,slideTo,slideMask;
 private int[] route=new int[0];
 private ValueAnimator animator,slideAnimator;
 public MazeView(Context c,MazeModel model,Runnable completed,Runnable changed){
  super(c);this.model=model;this.completed=completed;this.changed=changed;art=new MazeArt(c);
  setFocusable(true);setContentDescription("Sliding ball maze. Swipe a wooden tile toward an empty space, or tap a tile and then a highlighted empty space. Connect the blue ball to the golden goal.");
 }
 public MazeModel model(){return model;}
 public void reset(){stop();model.reset();selected=-1;invalidate();changed.run();}
 public void undo(){if(!animating&&!sliding&&model.undo()){selected=-1;invalidate();changed.run();}}
 public void resumeIfSolved(){if(model.path().length>0)post(this::roll);}
 public void stop(){cancelled=true;slideCancelled=true;if(animator!=null)animator.cancel();if(slideAnimator!=null)slideAnimator.cancel();animating=false;sliding=false;}
 @Override protected void onDetachedFromWindow(){stop();super.onDetachedFromWindow();}
 private float dp(float x){return x*getResources().getDisplayMetrics().density;}
 @Override protected void onDraw(Canvas canvas){
  super.onDraw(canvas);int n=model.size();int[] cells=model.cells();
  float size=Math.max(0,Math.min(getWidth()-dp(28),getHeight()-dp(28)));cell=size/n;ox=(getWidth()-size)/2;oy=(getHeight()-size)/2;
  if(cell<=0)return;
  paint.setColor(0xff050e13);canvas.drawRoundRect(ox-dp(9),oy-dp(7),ox+size+dp(9),oy+size+dp(11),dp(18),dp(18),paint);
  art.draw(canvas,MazeArt.FRAME,new RectF(ox-dp(10),oy-dp(10),ox+size+dp(10),oy+size+dp(10)));
  int[] targets=model.destinations(selected);
  for(int i=0;i<cells.length;i++){
   float x=ox+i%n*cell,y=oy+i/n*cell;RectF rect=tileRect(x,y);
   art.draw(canvas,MazeArt.GAP,rect);
   if(cells[i]==0){
    boolean target=false;for(int value:targets)if(i==value)target=true;
    if(target){paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(dp(2));paint.setColor(0xffb4f3cf);canvas.drawRoundRect(inset(rect,cell*.075f),cell*.12f,cell*.12f,paint);paint.setStyle(Paint.Style.FILL);paint.setColor(0xffb4f3cf);canvas.drawCircle(x+cell/2,y+cell/2,cell*.05f,paint);}
    else{paint.setColor(0x558cb5b3);canvas.drawCircle(x+cell/2,y+cell/2,cell*.025f,paint);}
   }else if(!sliding||i!=slideTo)drawTile(canvas,i,cells[i],x,y,selected==i);
  }
  if(sliding){float x=ox+((slideFrom%n)*(1-slideProgress)+(slideTo%n)*slideProgress)*cell;float y=oy+((slideFrom/n)*(1-slideProgress)+(slideTo/n)*slideProgress)*cell;drawTile(canvas,slideTo,slideMask,x,y,false);}
  float bx=ox+cell/2,by=oy+cell/2;
  if(animating&&route.length>1){
   Path track=new Path();track.moveTo(bx,by);
   for(int j=0;j<route.length-1;j++){
    int a=route[j],b=route[j+1];float ax=ox+(a%n+.5f)*cell,ay=oy+(a/n+.5f)*cell;
    float nx=ox+(b%n+.5f)*cell,ny=oy+(b/n+.5f)*cell;
    if(j==0)track.lineTo((ax+nx)/2,(ay+ny)/2);else track.quadTo(ax,ay,(ax+nx)/2,(ay+ny)/2);
   }
   int last=route[route.length-1];track.lineTo(ox+(last%n+.5f)*cell,oy+(last/n+.5f)*cell);
   PathMeasure measure=new PathMeasure(track,false);float[] point=new float[2];measure.getPosTan(measure.getLength()*progress,point,null);bx=point[0];by=point[1];
  }
  art.marble(canvas,bx,by,cell*.16f);
 }
 private RectF tileRect(float x,float y){float gap=Math.min(dp(3),cell*.045f);return new RectF(x+gap/2,y+gap/2,x+cell-gap/2,y+cell-gap/2);}
 private RectF inset(RectF rect,float amount){return new RectF(rect.left+amount,rect.top+amount,rect.right-amount,rect.bottom-amount);}
 private void drawTile(Canvas canvas,int index,int mask,float x,float y,boolean active){
  boolean start=index==0,goal=index==model.size()*model.size()-1;
  RectF rect=tileRect(x,y);art.draw(canvas,start?MazeArt.START:goal?MazeArt.GOAL:MazeArt.WOOD,rect);
  channel.draw(canvas,mask,x,y,cell,rect,start?MazeChannel.START:goal?MazeChannel.GOAL:MazeChannel.WOOD);
  if(goal){
   paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(cell*.035f);paint.setColor(0xffffe7a1);canvas.drawCircle(x+cell/2,y+cell/2,cell*.16f,paint);paint.setStyle(Paint.Style.FILL);
   paint.setColor(0xffffd77f);canvas.drawCircle(x+cell/2,y+cell/2,cell*.045f,paint);
  }
  if(active){paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(dp(2.5f));paint.setColor(0xffb4f3cf);canvas.drawRoundRect(inset(rect,dp(1)),cell*.12f,cell*.12f,paint);paint.setStyle(Paint.Style.FILL);}
 }
 private int cellAt(float x,float y){int n=model.size();x-=ox;y-=oy;if(cell<=0||x<0||y<0||x>=cell*n||y>=cell*n)return -1;return (int)(y/cell)*n+(int)(x/cell);}
 @Override public boolean onTouchEvent(MotionEvent event){
  if(event.getAction()==MotionEvent.ACTION_DOWN){downX=event.getX();downY=event.getY();getParent().requestDisallowInterceptTouchEvent(true);return true;}
  if(event.getAction()==MotionEvent.ACTION_CANCEL){getParent().requestDisallowInterceptTouchEvent(false);return true;}
  if(event.getAction()!=MotionEvent.ACTION_UP)return true;
  getParent().requestDisallowInterceptTouchEvent(false);performClick();if(animating||sliding||delivered||cell<=0)return true;
  int index=cellAt(downX,downY);if(index<0){selected=-1;invalidate();return true;}
  float dx=event.getX()-downX,dy=event.getY()-downY;
  if(Math.hypot(dx,dy)>Math.max(dp(10),cell*.16f)){
   int target=index+(Math.abs(dx)>Math.abs(dy)?(dx>0?1:-1):(dy>0?model.size():-model.size()));
   selected=-1;slide(index,target);invalidate();return true;
  }
  if(model.cells()[index]==0&&selected>=0){int from=selected;selected=-1;slide(from,index);}
  else{
   int[] targets=model.destinations(index);
   if(targets.length==1){selected=-1;slide(index,targets[0]);}
   else selected=targets.length>1?index:-1;
  }
  invalidate();return true;
 }
 @Override public boolean performClick(){super.performClick();return true;}
 private void slide(int from,int to){
  if(!model.move(from,to))return;
  performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);changed.run();
  slideFrom=from;slideTo=to;slideMask=model.cells()[to];slideProgress=0;sliding=true;slideCancelled=false;
  slideAnimator=ValueAnimator.ofFloat(0,1);slideAnimator.setDuration(130);slideAnimator.setInterpolator(new DecelerateInterpolator());
  slideAnimator.addUpdateListener(v->{slideProgress=(float)v.getAnimatedValue();invalidate();});
  slideAnimator.addListener(new AnimatorListenerAdapter(){@Override public void onAnimationEnd(Animator a){sliding=false;invalidate();if(!slideCancelled&&model.path().length>0)roll();}});slideAnimator.start();
 }
 private void roll(){
  if(animating||sliding||delivered||!isAttachedToWindow())return;
  route=model.path();if(route.length==0)return;animating=true;cancelled=false;
  animator=ValueAnimator.ofFloat(0,1);animator.setDuration(Math.min(2600,900+route.length*65L));
  animator.addUpdateListener(v->{progress=(float)v.getAnimatedValue();invalidate();});
  animator.addListener(new AnimatorListenerAdapter(){@Override public void onAnimationEnd(Animator a){if(!cancelled&&!delivered&&isAttachedToWindow()){delivered=true;completed.run();}}});animator.start();
 }
}
