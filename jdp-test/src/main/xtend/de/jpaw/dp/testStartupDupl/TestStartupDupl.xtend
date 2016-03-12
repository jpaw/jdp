package de.jpaw.dp.testStartupDupl

import de.jpaw.dp.Jdp
import de.jpaw.dp.Startup

@Startup(17)
class Test17 {
    def static public void onStartup() {
        println("17a has been called")
    }
}
@Startup(170)
class Test170 {
    def static public void onStartup() {
        println("170 has been called")
    }
}
@Startup(17)  // problem: duplicate order
class Test3 {
    def static public void onStartup() {
        println("17b has been called")
    }
}

class Main {
    def static void main(String [] args) {
        Jdp.init("de.jpaw.dp.testStartupDupl");  // this should fail!
    }
}
