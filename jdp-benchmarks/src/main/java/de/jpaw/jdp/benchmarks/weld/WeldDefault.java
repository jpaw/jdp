package de.jpaw.jdp.benchmarks.weld;

public class WeldDefault implements MyDefaultInterface {

    @Override
    public int compute(int arg) {
        return arg * arg;
    }
}
