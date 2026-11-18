import java.util.*;

public class ListCollectionBenchmark {
    static volatile long sink;
    static long time(Runnable task) { long s=System.nanoTime(); task.run(); return System.nanoTime()-s; }
    static void run(int n) {
        ArrayList<Integer> a=new ArrayList<>(); LinkedList<Integer> l=new LinkedList<>();
        for(int i=0;i<n;i++){a.add(i);l.add(i);}
        int reps=Math.max(1000,Math.min(100000,n*10));
        long aEnd=time(()->{for(int i=0;i<reps;i++) a.add(-i);});
        long lEnd=time(()->{for(int i=0;i<reps;i++) l.add(-i);});
        a.subList(n,a.size()).clear(); l.subList(n,l.size()).clear();
        long aFront=time(()->{for(int i=0;i<Math.min(reps,5000);i++) a.add(0,-i);});
        long lFront=time(()->{for(int i=0;i<Math.min(reps,5000);i++) l.add(0,-i);});
        a.clear();l.clear();for(int i=0;i<n;i++){a.add(i);l.add(i);}
        int k=n/2; long aGet=time(()->{long x=0;for(int i=0;i<reps;i++)x+=a.get(k);sink=x;});
        long lGet=time(()->{long x=0;for(int i=0;i<reps;i++)x+=l.get(k);sink=x;});
        long aIter=time(()->{long x=0;for(int v:a)x+=v;sink=x;});
        long lIter=time(()->{long x=0;for(int v:l)x+=v;sink=x;});
        ArrayDeque<Integer> d=new ArrayDeque<>();
        long dequeEnds=time(()->{for(int i=0;i<reps;i++){d.addFirst(i);d.addLast(i);d.removeFirst();d.removeLast();}});
        System.out.printf("%d,%d,%d,%d,%d,%d,%d,%d%n",n,aEnd/1000,lEnd/1000,aFront/1000,lFront/1000,aGet/1000,lGet/1000,aIter/1000);
        System.out.printf("  LinkedList iteration=%d us; ArrayDeque paired-end operations=%d us%n",lIter/1000,dequeEnds/1000);
    }
    public static void main(String[] args){System.out.println("size,ArrayList-end-us,LinkedList-end-us,ArrayList-front-us,LinkedList-front-us,ArrayList-get-us,LinkedList-get-us,ArrayList-iterate-us");for(int n:new int[]{1000,10000,50000})run(n);System.out.println("Times are local JVM measurements; repeat runs before drawing conclusions. sink="+sink);}
}