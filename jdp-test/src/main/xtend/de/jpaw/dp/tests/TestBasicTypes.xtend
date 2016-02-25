package de.jpaw.dp.tests

import de.jpaw.dp.Inject
import de.jpaw.dp.Jdp
import de.jpaw.dp.Named
import de.jpaw.dp.Provider
import java.io.Serializable

// showcase injection of ordinary types
class MyDiTest {
    @Inject public String hello

    @Inject @Named("remotePort") public Integer port

    @Inject Provider<Serializable> mySerializer
}


class BasicMain {
    def static void main(String [] args) {
        Jdp.init("de.jpaw.dp.tests");
        Jdp.bind("Hello, world", null)
        Jdp.bind(Integer.valueOf(8880), "remotePort")

        val x = new MyDiTest()
        System.out.println('''hello is «x.hello»''')    // unqualified type
        System.out.println('''port is «x.port»''')      // named type
    }
}
