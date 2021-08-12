package de.jpaw.jdp.benchmarks.weld;

import jakarta.inject.Singleton;

@Singleton
public class WeldSingleton implements MySingletonInterface {

    @Override
    public int compute(int arg) {
        return arg * arg;
    }
}
