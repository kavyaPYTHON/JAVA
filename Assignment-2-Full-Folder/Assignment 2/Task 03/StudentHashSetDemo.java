import java.util.*;

class Student {
    private final String regNo; private final String name;
    Student(String regNo,String name){this.regNo=regNo;this.name=name;}
    @Override public boolean equals(Object o){if(this==o)return true;if(!(o instanceof Student))return false;Student s=(Student)o;return Objects.equals(regNo,s.regNo);}
    // Correct hashCode: equal students (same regNo) must have the same hash.
    @Override public int hashCode(){return Objects.hash(regNo);}
    @Override public String toString(){return regNo+" "+name;}
}

public class StudentHashSetDemo {
    public static void main(String[] args){
        Set<Student> present=new HashSet<>();
        present.add(new Student("192311001","Arun"));
        present.add(new Student("192311002","Divya"));
        present.add(new Student("192311001","Arun"));
        System.out.println("Corrected HashSet size: "+present.size());
        System.out.println("Unique students: "+present);
        // Deliberately broken class: equals compares registration number, but Object.hashCode remains.
        class BrokenStudent {
            String regNo; BrokenStudent(String r){regNo=r;}
            public boolean equals(Object o){return o instanceof BrokenStudent && regNo.equals(((BrokenStudent)o).regNo);}
        }
        Set<BrokenStudent> broken=new HashSet<>();
        broken.add(new BrokenStudent("R101"));broken.add(new BrokenStudent("R101"));
        System.out.println("Broken implementation size (often 2; hash collisions can affect demonstration): "+broken.size());
        System.out.println("Use immutable equality fields after insertion; changing a hash key can make lookup/removal fail.");
    }
}