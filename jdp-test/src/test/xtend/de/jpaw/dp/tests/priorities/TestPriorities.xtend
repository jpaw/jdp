package de.jpaw.dp.tests.priorities

import de.jpaw.dp.Default
import de.jpaw.dp.Fallback
import de.jpaw.dp.Jdp
import de.jpaw.dp.Singleton
import de.jpaw.dp.exceptions.NonuniqueImplementationException
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test

import static org.testng.Assert.*

interface Leaden {}
interface Metal {}
interface Medal {}
interface Letal {}
interface Grey {}

@Fallback
@Singleton
class Lead implements Metal, Letal, Leaden, Grey {
}

@Singleton
class Bronze implements Metal, Medal {
}

@Singleton
class Silver implements Metal, Medal, Grey {
}

@Default
@Singleton
class Gold implements Metal, Medal {
}

@Singleton
class Mercury implements Metal, Letal, Grey {
}

@Test(singleThreaded=true)
class TestPriorities {

    @BeforeMethod
    def void setup() {
        Jdp.reset
        Jdp.init("de.jpaw.dp.tests.priorities")
    }

    def void testFallback1() {
        assertEquals(Jdp.getRequired(Leaden).class, Lead)  // the only class is the fallback one
    }
    def void testFallback2() {
        assertEquals(Jdp.getRequired(Letal).class, Mercury)  // fallback is skipped in favour of other implementation
    }

    def void testDefault1() {
        assertEquals(Jdp.getRequired(Metal).class, Gold)  // Gold has priority (with Fallback)
    }
    def void testDefault2() {
        assertEquals(Jdp.getRequired(Medal).class, Gold)  // Gold has priority (no Fallback)
    }

    @Test(expectedExceptions=NonuniqueImplementationException)
    def void testNonUnique() {
        Jdp.getRequired(Grey)                           // fallback and no default, and multiple candidates
    }
}
