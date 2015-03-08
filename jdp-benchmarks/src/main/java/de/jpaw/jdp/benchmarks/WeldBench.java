package de.jpaw.jdp.benchmarks;

import java.util.concurrent.TimeUnit;

import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

import de.jpaw.jdp.benchmarks.weld.MyASEInterface;
import de.jpaw.jdp.benchmarks.weld.MyASInterface;
import de.jpaw.jdp.benchmarks.weld.MyDefaultInterface;
import de.jpaw.jdp.benchmarks.weld.MyDependentInterface;
import de.jpaw.jdp.benchmarks.weld.MySingletonExceptionInterface;
import de.jpaw.jdp.benchmarks.weld.MySingletonInterface;

//java -jar target/jdp-benchmarks.jar -i 3 -f 3 -wf 3 -wi 3 ".*Weld.*"

//Benchmark                          Mode  Cnt      Score      Error  Units
//WeldBench.callApplicationScoped    avgt    9  84065.586 ± 3070.402  ns/op
//WeldBench.callDefault              avgt    9    470.418 ±    9.641  ns/op
//WeldBench.callDependents           avgt    9    470.301 ±   16.970  ns/op
//WeldBench.callSingletons           avgt    9    470.758 ±   14.194  ns/op
//WeldBench.callException            avgt    9   1324.464 ±   19.559  ns/op
//WeldBench.callASException          avgt    9   1287.050 ±   16.715  ns/op
//WeldBench.injectApplicationScoped  avgt    9   1417.219 ±  385.671  ns/op
//WeldBench.injectDefault            avgt    9   1515.415 ±  124.728  ns/op
//WeldBench.injectDependent          avgt    9   1594.785 ±  331.480  ns/op
//WeldBench.injectSingleton          avgt    9   1494.462 ±  196.846  ns/op

@State(value = Scope.Thread)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@BenchmarkMode(Mode.AverageTime)
public class WeldBench {
    private static final int NUM = 1000;

    public int start = 7823643;

    private Weld myWeld;
    private WeldContainer container;

    private MyASInterface myAS;
    private MyASEInterface myASE;
    private MyDefaultInterface myDefault;
    private MyDependentInterface myDependent;
    private MySingletonInterface mySingleton;
    private MySingletonExceptionInterface myException;

    private <T> T getBean(Class<T> type) {
        return container.instance().select(type).get();
    }

    @Setup
    public void setupWeld() {
        myWeld = new Weld();
        container = myWeld.initialize();
        myAS = getBean(MyASInterface.class);
        myASE = getBean(MyASEInterface.class);
        mySingleton = getBean(MySingletonInterface.class);
        myDependent = getBean(MyDependentInterface.class);
        myDefault = getBean(MyDefaultInterface.class);
        myException = getBean(MySingletonExceptionInterface.class);
    }

    @TearDown
    public void shutdownWeld() {
        myWeld.shutdown();
    }

    @Benchmark
    public int callSingletons() {
        int sum = 0;
        for (int i = 0; i < NUM; ++i)
            sum += mySingleton.compute(start + i);
        return sum;
    }

    @Benchmark
    public int callDependents() {
        int sum = 0;
        for (int i = 0; i < NUM; ++i)
            sum += myDependent.compute(start + i);
        return sum;
    }

    @Benchmark
    public int callApplicationScoped() {
        int sum = 0;
        for (int i = 0; i < NUM; ++i)
            sum += myAS.compute(start + i);
        return sum;
    }

    @Benchmark
    public int callDefault() {
        int sum = 0;
        for (int i = 0; i < NUM; ++i)
            sum += myDefault.compute(start + i);
        return sum;
    }

    @Benchmark
    public int callException() {
        try {
            return myException.compute(start + 33);
        } catch (Exception e) {
            return -4;
        }
    }

    @Benchmark
    public int callASException() {
        try {
            return myASE.compute(start + 33);
        } catch (Exception e) {
            return -4;
        }
    }

    @Benchmark
    public MySingletonInterface injectSingleton() {
        return getBean(MySingletonInterface.class);
    }

    @Benchmark
    public MyDependentInterface injectDependent() {
        return getBean(MyDependentInterface.class);
    }

    @Benchmark
    public MyASInterface injectApplicationScoped() {
        return getBean(MyASInterface.class);
    }

    @Benchmark
    public MyDefaultInterface injectDefault() {
        return getBean(MyDefaultInterface.class);
    }

}
