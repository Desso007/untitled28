

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.lang.invoke.CallSite;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@State(Scope.Thread)
public class Project {

    public static void main(String[] args) throws Exception {
        org.openjdk.jmh.Main.main(args);
    }

    public record Student(String name, String surname) {
    }

    private Student student;
    private Method method;
    private CallSite callSite;

    @Setup
    public void setup() throws Throwable {
        student = new Student("Alexander", "Biryukov");
        method = Student.class.getDeclaredMethod("name");
        method.setAccessible(true);

        MethodHandles.Lookup lookup = MethodHandles.lookup();
        MethodType methodType = MethodType.methodType(String.class);
        callSite = LambdaMetafactory.metafactory(
                lookup,
                "invoke",
                MethodType.methodType(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class),
                methodType,
                lookup.findVirtual(Student.class, "name", methodType),
                methodType
        );
    }

    @Benchmark
    public void directAccess(Blackhole bh) {
        String name = student.name();
        bh.consume(name);
    }

    @Benchmark
    public void reflection(Blackhole bh) throws InvocationTargetException, IllegalAccessException {
        String name = (String) method.invoke(student);
        bh.consume(name);
    }

    @Benchmark
    public void methodHandle(Blackhole bh) throws Throwable {
        String name = (String) MethodHandles.lookup().unreflect(method).invoke(student);
        bh.consume(name);
    }

    @Benchmark
    public void lambdaMetafactory(Blackhole bh) throws Throwable {
        String name = (String) callSite.getTarget().invoke(student);
        bh.consume(name);
    }
}
