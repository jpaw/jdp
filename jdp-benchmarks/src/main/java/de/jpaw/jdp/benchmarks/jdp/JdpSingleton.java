package de.jpaw.jdp.benchmarks.jdp;

import de.jpaw.dp.Singleton;

@Singleton
public class JdpSingleton implements MyInterface {

    @Override
    public int compute(int arg) {
        return arg * arg;
    }
}
