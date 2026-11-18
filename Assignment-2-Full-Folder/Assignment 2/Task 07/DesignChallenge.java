import java.util.*;

public class DesignChallenge {
    static long arrayListFront(int n){List<Integer> q=new ArrayList<>();long s=System.nanoTime();for(int i=0;i<n;i++)q.add(0,i);while(!q.isEmpty())q.remove(0);return System.nanoTime()-s;}
    static long arrayDequeEnds(int n){Deque<Integer> q=new ArrayDeque<>();long s=System.nanoTime();for(int i=0;i<n;i++)q.addLast(i);while(!q.isEmpty())q.removeFirst();return System.nanoTime()-s;}
    public static void main(String[] args){System.out.println("Queue redesign: ArrayList front operations -> ArrayDeque FIFO operations");for(int n:new int[]{1000,10000,50000}){long original=arrayListFront(n),redesigned=arrayDequeEnds(n);System.out.printf("n=%d | ArrayList front add/remove=%.3f ms | ArrayDeque addLast/removeFirst=%.3f ms%n",n,original/1e6,redesigned/1e6);}System.out.println("Diagnosis: ArrayList shifts existing elements for front insertion/removal (O(n) per operation).");System.out.println("Redesign: ArrayDeque uses a resizable circular array; addLast/removeFirst are amortized O(1).");System.out.println("Trade-off: ArrayDeque is not indexed and does not permit null elements. Repeat measurements on the target JVM.");}
}