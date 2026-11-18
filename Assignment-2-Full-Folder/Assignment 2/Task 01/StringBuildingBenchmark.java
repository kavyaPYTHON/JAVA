import java.util.*;

public class StringBuildingBenchmark {
    private static volatile int sink;
    interface Builder { String build(int fields); }
    static String sampleField(int i) { return "FIELD" + i + "=VALUE" + i + ";"; }
    static String usingString(int n) { String s = ""; for (int i=0;i<n;i++) s += sampleField(i); return s; }
    static String usingBuilder(int n) { StringBuilder s = new StringBuilder(); for (int i=0;i<n;i++) s.append(sampleField(i)); return s.toString(); }
    static String usingBuffer(int n) { StringBuffer s = new StringBuffer(); for (int i=0;i<n;i++) s.append(sampleField(i)); return s.toString(); }
    static long measure(Builder b, int fields, int tickets, int repeats) {
        for (int w=0;w<2;w++) for(int i=0;i<Math.max(1,tickets/10);i++) sink ^= b.build(fields).length();
        long total=0;
        for(int r=0;r<repeats;r++) { long start=System.nanoTime(); int chars=0; for(int t=0;t<tickets;t++) chars += b.build(fields).length(); total += System.nanoTime()-start; sink ^= chars; }
        return total/repeats;
    }
    public static void main(String[] args) {
        int[][] workloads={{5,1},{50,10000},{200,1000}};
        String[] names={"Short ticket","High-volume tickets","Wide ticket"};
        Builder[] methods={StringBuildingBenchmark::usingString,StringBuildingBenchmark::usingBuilder,StringBuildingBenchmark::usingBuffer};
        String[] labels={"String +","StringBuilder","StringBuffer"};
        System.out.println("Average execution time (ms); 5 measured repetitions");
        for(int w=0;w<workloads.length;w++) { System.out.println("\n"+names[w]+" | fields="+workloads[w][0]+", tickets="+workloads[w][1]);
            for(int m=0;m<methods.length;m++) System.out.printf("%-14s %.3f ms%n",labels[m],measure(methods[m],workloads[w][0],workloads[w][1],5)/1_000_000.0);
        }
        System.out.println("Estimated intermediate concatenation strings: repeated + creates many temporary strings; builder/buffer reuse a mutable character buffer.");
        System.out.println("Memory readings vary with JVM/GC; use a profiler for reliable allocation data. Sink="+sink);
    }
}