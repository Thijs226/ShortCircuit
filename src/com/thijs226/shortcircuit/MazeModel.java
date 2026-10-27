package com.thijs226.shortcircuit;
import java.util.*;

/** Fixed endpoints and one or more gaps. Every generated board starts from a connected route. */
public final class MazeModel {
 public static final int U=1,R=2,D=4,L=8;
 private final int n,gaps;
 private int[] board,initial;
 private int count;
 private final ArrayDeque<int[]> undo=new ArrayDeque<>();
 public MazeModel(int n,int[] cells){
  if(n<3||n>6||cells==null||cells.length!=n*n)throw new IllegalArgumentException("Invalid board size");
  this.n=n;board=cells.clone();int zeros=0;
  for(int mask:board){
   if(mask==0)zeros++;
   else if(mask<0||mask>15||Integer.bitCount(mask)>2)throw new IllegalArgumentException("Invalid pipe");
  }
  if(zeros<1||zeros>n*n-(2*n-1)||Integer.bitCount(board[0])!=1||Integer.bitCount(board[n*n-1])!=1)
   throw new IllegalArgumentException("Empty spaces and two fixed endpoints required");
  gaps=zeros;initial=board.clone();
 }
 public static int gapsForSize(int n){if(n<3||n>6)throw new IllegalArgumentException("Board size");return new int[]{1,2,4,6}[n-3];}
 public static int scrambleForSize(int n){if(n<3||n>6)throw new IllegalArgumentException("Board size");return new int[]{14,32,60,90}[n-3];}
 public static MazeModel generate(int n,int steps,Random rng){return generate(n,gapsForSize(n),steps,rng);}
 public static MazeModel generate(int n,int gaps,int steps,Random rng){
  if(n<3||n>6||gaps<1||gaps>n*n-(2*n-1)||rng==null)throw new IllegalArgumentException("Invalid generation parameters");
  for(int attempt=0;attempt<500;attempt++){
   ArrayList<Integer> candidates=new ArrayList<>();for(int i=1;i<n*n-1;i++)candidates.add(i);Collections.shuffle(candidates,rng);
   boolean[] blocked=new boolean[n*n];for(int i=0;i<gaps;i++)blocked[candidates.get(i)]=true;
   ArrayList<Integer> route=new ArrayList<>();
   if(!route(0,n,blocked.clone(),route,rng))continue;
   int[] cells=new int[n*n];int[] masks={U|R,R|D,D|L,L|U,U|D,L|R};
   for(int i=0;i<cells.length;i++)cells[i]=blocked[i]?0:masks[rng.nextInt(masks.length)];
   for(int i=0;i<route.size();i++){
    int at=route.get(i),mask=0;
    if(i>0)mask|=direction(at,route.get(i-1),n);
    if(i+1<route.size())mask|=direction(at,route.get(i+1),n);
    cells[at]=mask;
   }
   MazeModel model=new MazeModel(n,cells);int[] previous=null;
   for(int k=0;k<Math.max(4,steps);k++){
    ArrayList<int[]> choices=new ArrayList<>();
    for(int tile=1;tile<n*n-1;tile++)for(int gap:model.destinations(tile)){
     if(previous==null||tile!=previous[1]||gap!=previous[0])choices.add(new int[]{tile,gap});
    }
    if(choices.isEmpty())break;
    int[] pick=choices.get(rng.nextInt(choices.size()));model.move(pick[0],pick[1]);previous=pick;
   }
   if(model.path().length==0){model.initial=model.board.clone();model.count=0;model.undo.clear();return model;}
  }
  throw new IllegalStateException("Could not generate maze");
 }
 private static boolean route(int at,int n,boolean[] seen,List<Integer> route,Random rng){
  seen[at]=true;route.add(at);if(at==n*n-1)return true;
  List<Integer> options=new ArrayList<>();
  if(at>=n)options.add(at-n);if(at+n<n*n)options.add(at+n);
  if(at%n>0)options.add(at-1);if(at%n<n-1)options.add(at+1);
  Collections.shuffle(options,rng);
  for(int next:options)if(!seen[next]&&route(next,n,seen,route,rng))return true;
  route.remove(route.size()-1);return false;
 }
 private ArrayList<Integer> neighbors(int at){
  ArrayList<Integer> a=new ArrayList<>();
  if(at>=n)a.add(at-n);if(at%n<n-1)a.add(at+1);
  if(at+n<n*n)a.add(at+n);if(at%n>0)a.add(at-1);return a;
 }
 private static int direction(int a,int b,int n){return b==a-n?U:b==a+n?D:b==a-1?L:R;}
 private static int opposite(int d){return d==U?D:d==D?U:d==L?R:L;}
 public int size(){return n;}
 public int gapCount(){return gaps;}
 /** First gap, retained for callers that only need a representative empty slot. */
 public int blank(){for(int i=0;i<board.length;i++)if(board[i]==0)return i;return -1;}
 public int[] cells(){return board.clone();}
 public int[] initial(){return initial.clone();}
 public int moves(){return count;}
 public int[] destinations(int index){
  if(index<=0||index>=board.length-1||board[index]==0)return new int[0];
  int[] result=new int[4];int length=0;for(int neighbor:neighbors(index))if(board[neighbor]==0)result[length++]=neighbor;
  return Arrays.copyOf(result,length);
 }
 public boolean move(int index){int[] targets=destinations(index);return targets.length>0&&move(index,targets[0]);}
 public boolean move(int index,int target){
  if(index<=0||index>=board.length-1||target<0||target>=board.length||board[index]==0||board[target]!=0||Math.abs(index/n-target/n)+Math.abs(index%n-target%n)!=1)return false;
  undo.push(new int[]{index,target});board[target]=board[index];board[index]=0;count++;return true;
 }
 public boolean undo(){
  if(undo.isEmpty())return false;
  int[] move=undo.pop();board[move[0]]=board[move[1]];board[move[1]]=0;count--;return true;
 }
 public void reset(){board=initial.clone();count=0;undo.clear();}
 public void restoreInitial(int[] savedInitial,int moves){
  MazeModel saved=new MazeModel(n,savedInitial);
  if(saved.gapCount()!=gaps||savedInitial[0]!=board[0]||savedInitial[board.length-1]!=board[board.length-1])throw new IllegalArgumentException("Different initial board");
  int[] before=saved.cells(),after=cells();Arrays.sort(before);Arrays.sort(after);
  if(!Arrays.equals(before,after))throw new IllegalArgumentException("Different pipe tiles");
  initial=savedInitial.clone();count=Math.max(0,moves);undo.clear();
 }
 public int[] path(){
  int[] previous=new int[board.length];Arrays.fill(previous,-1);previous[0]=0;
  ArrayDeque<Integer> q=new ArrayDeque<>();q.add(0);
  while(!q.isEmpty()){
   int at=q.remove();
   if(at==board.length-1){
    ArrayList<Integer> route=new ArrayList<>();for(int i=at;;i=previous[i]){route.add(i);if(i==0)break;}
    Collections.reverse(route);int[] result=new int[route.size()];for(int i=0;i<result.length;i++)result[i]=route.get(i);return result;
   }
   for(int next:neighbors(at)){
    int d=direction(at,next,n);
    if(previous[next]<0&&(board[at]&d)!=0&&(board[next]&opposite(d))!=0){previous[next]=at;q.add(next);}
   }
  }
  return new int[0];
 }
}
