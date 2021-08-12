package de.jpaw.jdp.benchmarks.weld;

import jakarta.enterprise.context.Dependent;

@Dependent
public class WeldDependent implements MyDependentInterface {

    @Override
    public int compute(int arg) {
        return arg * arg;
    }
}
