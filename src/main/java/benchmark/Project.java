package benchmark;

import java.lang.invoke.*;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

@State(Scope.Thread)
public class Project {

    public static void main(String[] args) throws RunnerException {
        Options options = new OptionsBuilder()
                .include(Project.class.getSimpleName())
                .shouldFailOnError(true)
                .shouldDoGC(true)
                .mode(Mode.AverageTime)
                .timeUnit(TimeUnit.NANOSECONDS)
                .forks(1)
                .warmupForks(1)
                .warmupIterations(1)
                .warmupTime(TimeValue.seconds(5))
                .measurementIterations(1)
                .measurementTime(TimeValue.seconds(5))
                .build();

        new Runner(options).run();
    }

    record Student(String name, String surname) {
    }

    private Student student;
    private Method method;
    private MethodHandle methodHandle;
    private CallSite callSite;

    @Setup
    public void setup() throws NoSuchMethodException, IllegalAccessException, NoSuchFieldException, LambdaConversionException {
        student = new Student("Alexander", "Biryukov");
        method = Student.class.getMethod("name");
        methodHandle = MethodHandles.lookup().unreflect(method);
        MethodType methodType = MethodType.methodType(String.class);

        callSite = LambdaMetafactory.metafactory(MethodHandles.lookup(), "get",
                MethodType.methodType(Supplier.class, Student.class), methodType, methodHandle, methodType);
    }

    @Benchmark
    public void directAccess(Blackhole bh) {
        String name = student.name();
        bh.consume(name);
    }

    @Benchmark
    public void reflection(Blackhole bh) throws Exception {
        String name = (String) method.invoke(student);
        bh.consume(name);
    }

    @Benchmark
    public void methodHandle(Blackhole bh) throws Throwable {
        String name = (String) methodHandle.invoke(student);
        bh.consume(name);
    }

    @Benchmark
    public void lambdaMetafactory(Blackhole bh) throws Throwable {
        Supplier<String> lambda = (Supplier<String>) callSite.getTarget().invokeExact(student);
        String name = lambda.get();
        bh.consume(name);
    }
}
