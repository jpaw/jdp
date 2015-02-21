package de.jpaw.jdp.benchmarks.weld;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class WeldApplicationScoped implements MyASInterface {

    @Override
    public int compute(int arg) {
        return arg * arg;
    }
}
