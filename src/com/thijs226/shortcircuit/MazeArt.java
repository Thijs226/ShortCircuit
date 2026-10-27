package com.thijs226.shortcircuit;
import android.content.Context;
import android.graphics.*;

/** Production artwork generated for this app; source atlas retained in art/. */
final class MazeArt {
 static final int WOOD=0,START=1,GOAL=2,GAP=3,MARBLE=4,FRAME=5;
 private static Bitmap shared;
 private final Bitmap atlas;
 private final Paint bitmapPaint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
 // Tight sprite bounds in the original 1536 by 1024 atlas. Alpha is preserved.
 private final Rect[] sprites={new Rect(16,12,504,488),new Rect(526,12,1016,488),new Rect(1025,12,1525,488),new Rect(16,500,504,992),new Rect(585,552,948,920),new Rect(1025,492,1528,995)};
 MazeArt(Context context){
  if(shared==null){BitmapFactory.Options options=new BitmapFactory.Options();options.inScaled=false;shared=BitmapFactory.decodeResource(context.getResources(),R.drawable.maze_atlas,options);}
  atlas=shared;
 }
 void draw(Canvas canvas,int sprite,RectF target){canvas.drawBitmap(atlas,sprites[sprite],target,bitmapPaint);}
 void marble(Canvas canvas,float cx,float cy,float radius){draw(canvas,MARBLE,new RectF(cx-radius,cy-radius,cx+radius,cy+radius));}
}
