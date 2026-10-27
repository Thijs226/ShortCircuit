package com.thijs226.shortcircuit;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

/** Carved channels with a soft inner wall and a crisp, light-catching lower lip. */
final class MazeChannel {
 static final int WOOD=0,START=1,GOAL=2;
 // Lip, cut edge, wall shadow and channel floor for each tile material.
 private static final int[][] COLORS={
  {0xffe8b978,0xff3c2416,0xff4d2b17,0xffa16b3b},
  {0xff80c5c7,0xff0b252b,0xff0b2c33,0xff397580},
  {0xffffdfa0,0xff51351b,0xff62411f,0xffb48446}
 };
 private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);

 void draw(Canvas canvas,int mask,float x,float y,float side,RectF tile,int material){
  if(mask==0||side<=0)return;
  Path path=path(mask,x,y,side);int[] colors=COLORS[material];
  canvas.save();
  // Openings end at the tile edge, never with a round cap spilling into a gap.
  canvas.clipRect(tile);
  paint.setStyle(Paint.Style.STROKE);paint.setStrokeCap(Paint.Cap.ROUND);paint.setStrokeJoin(Paint.Join.ROUND);
  canvas.save();canvas.translate(0,side*.014f);
  stroke(canvas,path,side*.436f,colors[0]);canvas.restore();
  stroke(canvas,path,side*.424f,colors[1]);
  // Fine blended bands form a curved wall rather than a few hard painted stripes.
  for(int layer=0;layer<=16;layer++){
   float t=layer/16f;
   stroke(canvas,path,side*(.398f-.124f*t),blend(colors[2],colors[3],(float)Math.sin(t*Math.PI/2)));
  }
  // A faint lower reflection gives the floor depth without obscuring its center.
  canvas.save();canvas.translate(0,side*.012f);
  stroke(canvas,path,side*.23f,blend(colors[3],colors[0],.06f));canvas.restore();
  paint.setStyle(Paint.Style.FILL);canvas.restore();
 }

 private void stroke(Canvas canvas,Path path,float width,int color){paint.setStrokeWidth(width);paint.setColor(color);canvas.drawPath(path,paint);}
 private static int blend(int a,int b,float t){
  int r=Math.round(((a>>16)&255)*(1-t)+((b>>16)&255)*t);
  int g=Math.round(((a>>8)&255)*(1-t)+((b>>8)&255)*t);
  int blue=Math.round((a&255)*(1-t)+(b&255)*t);
  return 0xff000000|(r<<16)|(g<<8)|blue;
 }

 /** Same midpoint curve used by the rolling ball, with exact half-edge connections. */
 static Path path(int mask,float x,float y,float side){
  float cx=x+side/2,cy=y+side/2;float[][] points=new float[2][2];int at=0;
  for(int d:new int[]{1,2,4,8})if((mask&d)!=0){
   points[at][0]=d==2?x+side:d==8?x:cx;points[at][1]=d==1?y:d==4?y+side:cy;
   if(++at==2)break;
  }
  Path path=new Path();if(at==0)return path;path.moveTo(points[0][0],points[0][1]);
  if(at==1)path.lineTo(cx,cy);else path.quadTo(cx,cy,points[1][0],points[1][1]);return path;
 }
}
