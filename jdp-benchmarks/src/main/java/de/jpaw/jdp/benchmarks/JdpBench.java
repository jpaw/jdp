package de.jpaw.jdp.benchmarks;

import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

import de.jpaw.dp.Jdp;
import de.jpaw.jdp.benchmarks.jdp.JdpDependent;
import de.jpaw.jdp.benchmarks.jdp.JdpSingleton;
import de.jpaw.jdp.benchmarks.jdp.MyInterface;

//java -jar target/jdp-benchmarks.jar -i 5 -f 5 -wf 3 -wi 3 ".*ModVsAnd.*"

//Benchmark                 Mode  Cnt    Score    Error  Units
//JdpBench.callDependents   avgt    9  479.183 ± 17.155  ns/op
//JdpBench.callSingletons   avgt    9  468.974 ±  9.824  ns/op
//JdpBench.callException    avgt    9    4.017 ±  0.086  ns/op
//JdpBench.injectDependent  avgt    9   27.638 ±  1.122  ns/op
//JdpBench.injectSingleton  avgt    9   15.565 ±  0.389  ns/op

@State(value = Scope.Thread)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@BenchmarkMode(Mode.AverageTime)
public class JdpBench {
    private static final int NUM = 1000;

    private MyInterface mySingleton;
    private MyInterface myDependent;

    public int start = 7823643;

    @Setup
    public void setupJdp() {
        Jdp.reset();
        Jdp.init("de.jpaw.jdp.benchmarks.jdp");
        mySingleton = Jdp.getRequired(JdpSingleton.class);
        myDependent = Jdp.getRequired(JdpDependent.class);
    }

    @Benchmark
    public int callSingletons() {
        int sum = 0;
        for (int i = 0; i < NUM; ++i)
            sum += mySingleton.compute(start + i);
        return sum;
    }

    @Benchmark
    public int callException() {
        try {
            return mySingleton.compute(start + 33);
        } catch (Exception e) {
            return -4;
        }
    }

    @Benchmark
    public int callDependents() {
        int sum = 0;
        for (int i = 0; i < NUM; ++i)
            sum += myDependent.compute(start + i);
        return sum;
    }

    @Benchmark
    public MyInterface injectSingleton() {
        return Jdp.getRequired(JdpSingleton.class);
    }

    @Benchmark
    public MyInterface injectDependent() {
        return Jdp.getRequired(JdpDependent.class);
    }
}
