package de.jpaw.dp.tests.dependent

import de.jpaw.dp.Dependent
import de.jpaw.dp.Jdp
import java.util.concurrent.atomic.AtomicInteger
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test

import static org.testng.Assert.*

interface I1 {
    def int open();
}

interface I2 {
    def int close();
}

interface I3 {
    def int open();        // same function defined in another interface
}

@Dependent
class MultiInterfaceClass implements I1, I2, I3 {
    private static final AtomicInteger instanceCounter = new AtomicInteger();

    private val myInstanceNo = instanceCounter.incrementAndGet

    def static void resetCounter() {
        instanceCounter.set(0)
    }

    override open() {
        return myInstanceNo
    }

    override close() {
        return myInstanceNo
    }
}

@Test(singleThreaded=true)
class TestMIClassThenInterfaces {

    @BeforeMethod
    def void setup() {
        Jdp.reset
        MultiInterfaceClass.resetCounter
        Jdp.init("de.jpaw.dp.tests.dependent")
    }

    def void testClassThenInterfaces() {
        assertEquals(Jdp.getRequired(MultiInterfaceClass).open, 1)
        assertEquals(Jdp.getRequired(I1).open,  2)
        assertEquals(Jdp.getRequired(I2).close, 3)
        assertEquals(Jdp.getRequired(I3).open,  4)
        assertEquals(Jdp.getRequired(MultiInterfaceClass).close, 5)
    }

    def void testInterfaceThenClass() {
        assertEquals(Jdp.getRequired(I1).open,  1)
        assertEquals(Jdp.getRequired(I2).close, 2)
        assertEquals(Jdp.getRequired(I3).open,  3)
        assertEquals(Jdp.getRequired(MultiInterfaceClass).close, 4)
    }

    def void testInterfaceThenClassOrdering2() {
        assertEquals(Jdp.getRequired(I2).close, 1)
        assertEquals(Jdp.getRequired(I1).open,  2)
        assertEquals(Jdp.getRequired(I3).open,  3)
        assertEquals(Jdp.getRequired(MultiInterfaceClass).close, 4)
    }
}
