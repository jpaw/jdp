package de.jpaw.jdp.benchmarks.jdp;

import de.jpaw.dp.Dependent;

@Dependent
public class JdpDependent implements MyInterface {

    @Override
    public int compute(int arg) {
        return arg * arg;
    }
}
