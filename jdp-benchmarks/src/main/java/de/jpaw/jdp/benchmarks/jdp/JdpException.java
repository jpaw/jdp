package de.jpaw.jdp.benchmarks.jdp;

import de.jpaw.dp.Singleton;

@Singleton
public class JdpException implements MyInterface {

    @Override
    public int compute(int arg) {
        if (arg > 100)
            throw new RuntimeException("Intentional exception");
        return arg * arg;
    }
}
