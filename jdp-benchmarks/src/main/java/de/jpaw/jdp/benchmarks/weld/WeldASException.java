package de.jpaw.jdp.benchmarks.weld;

import jakarta.inject.Singleton;

@Singleton
public class WeldASException implements MyASEInterface {

    @Override
    public int compute(int arg) {
        if (arg > 100)
            throw new RuntimeException("Intentional exception");
        return arg * arg;
    }
}
